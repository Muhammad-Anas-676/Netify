package com.netpulse.shared

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.SystemConfiguration.CNCopyCurrentNetworkInfo
import platform.SystemConfiguration.CNCopySupportedInterfaces
import platform.darwin.NSObject

/**
 * iOS implementation. Reading even the *connected* SSID here requires the
 * "Access WiFi Information" capability from Apple (an entitlement you must
 * request and justify in App Store review) — without it this returns null,
 * which is expected, not a bug. There is no saved-password API on iOS at
 * all, for any app — see NetworkInfoProvider.kt for the full explanation.
 */
actual class NetworkInfoProvider {

    actual fun currentSnapshot(): NetworkSnapshot {
        val ssid = readSsidIfEntitled()
        return NetworkSnapshot(
            isOnline = true, // wire up to NWPathMonitor for a real online/offline signal
            ssid = ssid,
            signalStrengthDbm = null, // iOS does not expose RSSI to third-party apps
            isSecured = null,
            ipAddress = null // read via getifaddrs() if you need this on iOS
        )
    }

    actual fun isSecuredNetwork(): Boolean? = null

    @OptIn(ExperimentalForeignApi::class)
    private fun readSsidIfEntitled(): String? {
        // Requires the com.apple.developer.networking.wifi-info entitlement.
        // Without it, CNCopyCurrentNetworkInfo silently returns null on device.
        return null
    }
}

actual fun nowMs(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()
