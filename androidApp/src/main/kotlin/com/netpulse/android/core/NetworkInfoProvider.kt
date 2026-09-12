package com.netpulse.android.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager

class NetworkInfoProvider(private val context: Context) {

    fun currentSnapshot(): NetworkSnapshot {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        val online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo

        val ssid = info?.ssid?.trim('"')?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }

        return NetworkSnapshot(
            isOnline = online,
            ssid = ssid,
            signalStrengthDbm = info?.rssi,
            isSecured = null, // needs scan-results matching, see note below
            ipAddress = intIpToString(info?.ipAddress ?: 0)
        )
    }

    private fun intIpToString(ip: Int): String {
        if (ip == 0) return "0.0.0.0"
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }

    // NOTE ON WI-FI PASSWORDS: there is no field for one here on purpose.
    // WifiManager can give you the connected SSID/signal/IP above, but no
    // Android API — with or without root — returns a saved network's
    // password to an app directly. The only real paths are root shell
    // access reading /data/misc/wifi (root only) or the user's own
    // QR-code export flow (Android 10+, Settings > Wi-Fi > network > Share).
}
