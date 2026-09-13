package com.netify.app.domain

import com.netify.app.domain.model.SpeedTestProgress
import com.netify.app.domain.model.SpeedTestResult
import com.netify.app.domain.model.TestServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

/**
 * Real, network-based speed test engine.
 *
 * Download/upload use Cloudflare's public speed-test endpoints
 * (speed.cloudflare.com/__down and /__up) — the same infrastructure the
 * official Cloudflare speed test uses. No API key required. Multiple
 * parallel streams are opened, matching how real speed test apps measure
 * throughput on fast connections (a single TCP stream under-reports on
 * high-bandwidth or high-latency links).
 *
 * Ping/jitter use a lightweight GET against the same host so the round
 * trip reflects the network path actually used for the transfer.
 */
class SpeedTestEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        val DEFAULT_SERVERS = listOf(
            TestServer(
                name = "Cloudflare · Global Anycast",
                downloadUrl = "https://speed.cloudflare.com/__down?bytes=",
                uploadUrl = "https://speed.cloudflare.com/__up",
                pingHost = "https://speed.cloudflare.com/__down?bytes=0"
            ),
            TestServer(
                name = "Cloudflare · Nearest Edge",
                downloadUrl = "https://speed.cloudflare.com/__down?bytes=",
                uploadUrl = "https://speed.cloudflare.com/__up",
                pingHost = "https://speed.cloudflare.com/__down?bytes=0"
            )
        )
        private const val PARALLEL_STREAMS = 4
        private const val DOWNLOAD_DURATION_MS = 4000L
        private const val UPLOAD_DURATION_MS = 3000L
        private const val PING_SAMPLES = 6
    }

    /** Runs the full ping -> download -> upload sequence, emitting live progress. */
    fun runFullTest(server: TestServer = DEFAULT_SERVERS.first()): Flow<SpeedTestProgress> = callbackFlow {
        trySend(SpeedTestProgress.MeasuringPing)

        val pingResult = measurePingAndJitter(server.pingHost, PING_SAMPLES)
        if (pingResult == null) {
            trySend(SpeedTestProgress.Failed("Could not reach the test server. Check your connection."))
            close()
            return@callbackFlow
        }
        val (ping, jitter) = pingResult

        val download = measureDownload(server.downloadUrl, DOWNLOAD_DURATION_MS) { live ->
            trySend(SpeedTestProgress.Downloading(live))
        }
        if (download == null) {
            trySend(SpeedTestProgress.Failed("Download test failed — check your connection."))
            close()
            return@callbackFlow
        }

        val upload = measureUpload(server.uploadUrl, UPLOAD_DURATION_MS) { live ->
            trySend(SpeedTestProgress.Uploading(live))
        }

        val result = SpeedTestResult(
            downloadMbps = download,
            uploadMbps = upload ?: 0.0,
            pingMs = ping,
            jitterMs = jitter,
            serverName = server.name
        )
        trySend(SpeedTestProgress.Done(result))
        close()
        awaitClose { }
    }

    /** Average RTT and mean absolute deviation (jitter) over [samples] round trips. */
    private suspend fun measurePingAndJitter(url: String, samples: Int): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            val timings = mutableListOf<Double>()
            repeat(samples) {
                val start = System.nanoTime()
                val ok = runCatching {
                    val req = Request.Builder().url(url).build()
                    client.newCall(req).execute().use { it.isSuccessful || it.code in 200..399 }
                }.getOrDefault(false)
                if (ok) {
                    val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
                    timings.add(elapsedMs)
                }
            }
            if (timings.isEmpty()) return@withContext null
            val avg = timings.average()
            val jitter = if (timings.size > 1) {
                timings.zipWithNext { a, b -> abs(a - b) }.average()
            } else 0.0
            avg to jitter
        }

    /** Opens [PARALLEL_STREAMS] concurrent downloads for [durationMs] and reports aggregate Mbps. */
    private suspend fun measureDownload(
        baseUrl: String,
        durationMs: Long,
        onLive: (Double) -> Unit
    ): Double? = withContext(Dispatchers.IO) {
        val totalBytes = AtomicLong(0)
        val start = System.nanoTime()
        val endAt = start + durationMs * 1_000_000

        val jobs = (1..PARALLEL_STREAMS).map { streamIndex ->
            async {
                while (System.nanoTime() < endAt) {
                    val chunkSize = 5_000_000 // 5 MB chunk per request
                    val req = Request.Builder()
                        .url("$baseUrl$chunkSize&s=$streamIndex&t=${System.currentTimeMillis()}")
                        .build()
                    try {
                        client.newCall(req).execute().use { resp ->
                            if (!resp.isSuccessful) return@use
                            val body = resp.body ?: return@use
                            val source = body.source()
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val read = source.read(buffer)
                                if (read == -1) break
                                totalBytes.addAndGet(read.toLong())
                                if (System.nanoTime() >= endAt) break
                                val elapsedSec = (System.nanoTime() - start) / 1_000_000_000.0
                                if (elapsedSec > 0.15) {
                                    onLive((totalBytes.get() * 8) / elapsedSec / 1_000_000.0)
                                }
                            }
                        }
                    } catch (_: IOException) {
                        // A single failed chunk shouldn't kill the whole measurement.
                    }
                }
            }
        }
        jobs.awaitAll()
        val elapsedSec = (System.nanoTime() - start) / 1_000_000_000.0
        val bytes = totalBytes.get()
        if (bytes == 0L || elapsedSec <= 0) return@withContext null
        (bytes * 8) / elapsedSec / 1_000_000.0
    }

    /** Opens [PARALLEL_STREAMS] concurrent uploads for [durationMs] and reports aggregate Mbps. */
    private suspend fun measureUpload(
        url: String,
        durationMs: Long,
        onLive: (Double) -> Unit
    ): Double? = withContext(Dispatchers.IO) {
        val chunk = ByteArray(1 * 1024 * 1024) // 1 MB body per request
        val totalBytes = AtomicLong(0)
        val start = System.nanoTime()
        val endAt = start + durationMs * 1_000_000

        val jobs = (1..PARALLEL_STREAMS).map {
            async {
                while (System.nanoTime() < endAt) {
                    val body: RequestBody = chunk.toRequestBody(null, 0, chunk.size)
                    val req = Request.Builder().url(url).post(body).build()
                    try {
                        client.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                totalBytes.addAndGet(chunk.size.toLong())
                                val elapsedSec = (System.nanoTime() - start) / 1_000_000_000.0
                                if (elapsedSec > 0.15) {
                                    onLive((totalBytes.get() * 8) / elapsedSec / 1_000_000.0)
                                }
                            }
                        }
                    } catch (_: IOException) {
                        // ignore a single failed chunk
                    }
                }
            }
        }
        jobs.awaitAll()
        val elapsedSec = (System.nanoTime() - start) / 1_000_000_000.0
        val bytes = totalBytes.get()
        if (bytes == 0L || elapsedSec <= 0) return@withContext null
        (bytes * 8) / elapsedSec / 1_000_000.0
    }
}
