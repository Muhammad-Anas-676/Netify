package com.netify.app.domain.model

/** One completed speed test result. Timestamps are epoch millis. */
data class SpeedTestResult(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Double,
    val jitterMs: Double,
    val serverName: String,
    val timestamp: Long = System.currentTimeMillis()
)

/** Live progress emitted while a test is running, so the UI gauge can animate in real time. */
sealed class SpeedTestProgress {
    data object Idle : SpeedTestProgress()
    data object MeasuringPing : SpeedTestProgress()
    data class Downloading(val currentMbps: Double) : SpeedTestProgress()
    data class Uploading(val currentMbps: Double) : SpeedTestProgress()
    data class Done(val result: SpeedTestResult) : SpeedTestProgress()
    data class Failed(val reason: String) : SpeedTestProgress()
}

data class TestServer(val name: String, val downloadUrl: String, val uploadUrl: String, val pingHost: String)

/** Snapshot of the currently connected WiFi network. */
data class WifiInfoSnapshot(
    val isConnected: Boolean,
    val ssid: String,
    val bssid: String,
    val rssiDbm: Int,
    val linkSpeedMbps: Int,
    val frequencyMhz: Int,
    val channel: Int,
    val is5Ghz: Boolean,
    val gatewayIp: String,
    val deviceIp: String,
    val signalQuality: SignalQuality
)

enum class SignalQuality { EXCELLENT, GOOD, FAIR, WEAK, UNKNOWN }

data class ChannelCongestion(val channel: Int, val apCount: Int, val isBusy: Boolean)

data class DiscoveredDevice(val ip: String, val hostname: String?, val isLikelyThisDevice: Boolean)

data class VaultEntry(val id: String, val ssid: String, val password: String, val security: String)
