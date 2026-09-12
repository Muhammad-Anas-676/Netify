package com.netpulse.shared

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Real network speed test — same approach as the HTML prototype
 * (timed HTTP transfers against public endpoints), just running as
 * native code on Android/iOS instead of browser fetch calls.
 */
class SpeedTestEngine(private val client: HttpClient) {

    private val downloadUrl = "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.7.1/jquery.min.js"
    private val pingUrl = "https://httpbin.org/get"
    private val uploadUrl = "https://httpbin.org/post"

    suspend fun measurePingMs(samples: Int = 4): Double? = withContext(Dispatchers.Default) {
        val times = mutableListOf<Double>()
        repeat(samples) {
            val start = nowMs()
            try {
                client.get(pingUrl) {
                    url { parameters.append("_", (start + it).toString()) }
                }
                times.add((nowMs() - start).toDouble())
            } catch (_: Exception) { /* skip a failed sample */ }
        }
        if (times.isEmpty()) null else times.average()
    }

    suspend fun measureDownloadMbps(durationMs: Long = 3500): Double? = withContext(Dispatchers.Default) {
        var totalBytes = 0L
        val start = nowMs()
        while (nowMs() - start < durationMs) {
            try {
                val response = client.get(downloadUrl) {
                    url { parameters.append("_", nowMs().toString()) }
                }
                totalBytes += response.bodyAsBytes().size
            } catch (_: Exception) { break }
        }
        val elapsedSec = (nowMs() - start) / 1000.0
        if (totalBytes == 0L || elapsedSec == 0.0) null
        else (totalBytes * 8) / elapsedSec / 1_000_000.0
    }

    suspend fun measureUploadMbps(durationMs: Long = 2500): Double? = withContext(Dispatchers.Default) {
        val payload = ByteArray(256 * 1024) // 256KB chunk, same as the prototype
        var totalBytes = 0L
        val start = nowMs()
        while (nowMs() - start < durationMs) {
            try {
                client.post(uploadUrl) {
                    setBody(payload)
                    contentType(ContentType.Application.OctetStream)
                }
                totalBytes += payload.size
            } catch (_: Exception) { break }
        }
        val elapsedSec = (nowMs() - start) / 1000.0
        if (totalBytes == 0L || elapsedSec == 0.0) null
        else (totalBytes * 8) / elapsedSec / 1_000_000.0
    }

    /** Runs ping → download → upload in sequence and returns a full result, or null if any step failed. */
    suspend fun runFullTest(): SpeedResult? {
        val ping = measurePingMs() ?: return null
        val down = measureDownloadMbps() ?: return null
        val up = measureUploadMbps() ?: return null
        return SpeedResult(
            downloadMbps = down,
            uploadMbps = up,
            pingMs = ping,
            timestampEpochMs = nowMs()
        )
    }
}

expect fun nowMs(): Long
