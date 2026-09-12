package com.netpulse.android.core

/** One completed speed test run. */
data class SpeedResult(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Double,
    val timestampEpochMs: Long
)

/** A single history log entry — speed test or password view. */
sealed class HistoryEntry {
    abstract val timestampEpochMs: Long

    data class SpeedTest(
        val result: SpeedResult,
        override val timestampEpochMs: Long
    ) : HistoryEntry()

    data class PasswordViewed(
        val networkName: String,
        override val timestampEpochMs: Long
    ) : HistoryEntry()
}

/** Snapshot of the current connection. */
data class NetworkSnapshot(
    val isOnline: Boolean,
    val ssid: String?,
    val signalStrengthDbm: Int?,
    val isSecured: Boolean?,
    val ipAddress: String?
)

/** User's target ISP-promised speed, for the promised-vs-actual alert. */
data class IspPlan(
    val promisedDownloadMbps: Double,
    val promisedUploadMbps: Double,
    val alertThresholdPercent: Int = 70
)
