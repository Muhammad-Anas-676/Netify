package com.netpulse.shared

/**
 * Platform-specific network info. Implementations live in androidMain/iosMain.
 *
 * Honest limits, by design of each platform (not by choice of this app):
 * - `ssid`: available on Android via WifiManager for the *currently connected*
 *   network. On iOS this requires special Apple entitlements even to read the
 *   connected SSID, and is often unavailable — treat as best-effort/nullable.
 * - Saved Wi-Fi passwords are NOT part of this interface at all. Android needs
 *   root or the user's own QR-export flow; iOS exposes no API for this to any
 *   third-party app, ever. Don't add a `password` field here — there's no
 *   platform-safe way to fill it in.
 */
expect class NetworkInfoProvider {
    fun currentSnapshot(): NetworkSnapshot
    fun isSecuredNetwork(): Boolean?
}
