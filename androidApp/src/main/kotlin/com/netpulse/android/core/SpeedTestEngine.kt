package com.netpulse.android.core

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Real network speed test — timed HTTP transfers against public endpoints,
 * same approach as the HTML prototype, running natively here.
 */
class SpeedTestEngine(private val client: HttpClient) {

    private val downloadUrl = "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.7.1/jquery.min.js"
    private val pingUrl = "https://httpbin.org/get"
    private val uploadUrl = "https://httpbin.org/post"

    suspend fun measurePingMs(samples: Int = 4): Double? = withContext(Dispatchers.IO) {
        val times = mutableListOf<Double>()
        repeat(samples) {
            val start = System.currentTimeMillis()
            try {
                client.get(pingUrl) {
                    url { parameters.append("_", (start + it).toString()) }
                }
                times.add((System.currentTimeMillis() - start).toDouble())
            } catch (_: Exception) { /* skip a failed sample */ }
        }
        if (times.isEmpty()) null else times.average()
    }

    suspend fun measureDownloadMbps(durationMs: Long = 3500): Double? = withContext(Dispatchers.IO) {
        var totalBytes = 0L
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < durationMs) {
            try {
                val response = client.get(downloadUrl) {
                    url { parameters.append("_", System.currentTimeMillis().toString()) }
                }
                totalBytes += response.bodyAsBytes().size
            } catch (_: Exception) { break }
        }
        val elapsedSec = (System.currentTimeMillis() - start) / 1000.0
        if (totalBytes == 0L || elapsedSec == 0.0) null
        else (totalBytes * 8) / elapsedSec / 1_000_000.0
    }

    suspend fun measureUploadMbps(durationMs: Long = 2500): Double? = withContext(Dispatchers.IO) {
        val payload = ByteArray(256 * 1024)
        var totalBytes = 0L
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < durationMs) {
            try {
                client.post(uploadUrl) {
                    setBody(payload)
                    contentType(ContentType.Application.OctetStream)
                }
                totalBytes += payload.size
            } catch (_: Exception) { break }
        }
        val elapsedSec = (System.currentTimeMillis() - start) / 1000.0
        if (totalBytes == 0L || elapsedSec == 0.0) null
        else (totalBytes * 8) / elapsedSec / 1_000_000.0
    }

    suspend fun runFullTest(): SpeedResult? {
        val ping = measurePingMs() ?: return null
        val down = measureDownloadMbps() ?: return null
        val up = measureUploadMbps() ?: return null
        return SpeedResult(
            downloadMbps = down,
            uploadMbps = up,
            pingMs = ping,
            timestampEpochMs = System.currentTimeMillis()
        )
    }
}
