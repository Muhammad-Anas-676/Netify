package com.netpulse.shared

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager

actual class NetworkInfoProvider(private val context: Context) {

    actual fun currentSnapshot(): NetworkSnapshot {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        val online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo

        // SSID comes back quoted ("MyNetwork") — strip the quotes for display.
        val ssid = info?.ssid?.trim('"')?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }

        return NetworkSnapshot(
            isOnline = online,
            ssid = ssid,
            signalStrengthDbm = info?.rssi,
            isSecured = isSecuredNetwork(),
            ipAddress = intIpToString(info?.ipAddress ?: 0)
        )
    }

    actual fun isSecuredNetwork(): Boolean? {
        // WifiManager doesn't expose the security type of the *currently connected*
        // network directly pre-Android 12; a full implementation should read this
        // from WifiManager.getScanResults() matched against the connected SSID
        // (requires ACCESS_FINE_LOCATION permission, same as SSID access).
        return null
    }

    private fun intIpToString(ip: Int): String {
        if (ip == 0) return "0.0.0.0"
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }
}

actual fun nowMs(): Long = System.currentTimeMillis()
