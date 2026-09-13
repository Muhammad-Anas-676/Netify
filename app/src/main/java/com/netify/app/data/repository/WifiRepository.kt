package com.netify.app.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.text.format.Formatter
import com.netify.app.domain.model.ChannelCongestion
import com.netify.app.domain.model.SignalQuality
import com.netify.app.domain.model.WifiInfoSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Wraps [WifiManager] to expose the connected network's real details.
 *
 * Reading SSID/BSSID requires ACCESS_FINE_LOCATION at runtime on API 27+;
 * the caller (ViewModel) is responsible for requesting that permission
 * before calling [getCurrentWifiInfo]. Without the permission, Android
 * returns a redacted SSID ("<unknown ssid>") — this is an OS privacy
 * restriction, not a bug in this class.
 */
class WifiRepository(private val context: Context) {

    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager: ConnectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun getCurrentWifiInfo(): WifiInfoSnapshot {
        val info: WifiInfo? = wifiManager.connectionInfo
        val isConnected = info != null && info.networkId != -1
        if (info == null || !isConnected) {
            return WifiInfoSnapshot(
                isConnected = false, ssid = "Not connected", bssid = "--", rssiDbm = 0,
                linkSpeedMbps = 0, frequencyMhz = 0, channel = 0, is5Ghz = false,
                gatewayIp = "--", deviceIp = "--", signalQuality = SignalQuality.UNKNOWN
            )
        }

        val ssid = info.ssid?.trim('"').orEmpty().ifBlank { "Unknown" }
        val frequency = info.frequency
        val channel = frequencyToChannel(frequency)
        val is5Ghz = frequency in 4900..5900
        val quality = rssiToQuality(info.rssi)

        val dhcp = wifiManager.dhcpInfo
        val gateway = dhcp?.gateway?.let { intToIp(it) } ?: "--"
        val deviceIp = dhcp?.ipAddress?.let { intToIp(it) } ?: "--"

        return WifiInfoSnapshot(
            isConnected = true,
            ssid = ssid,
            bssid = info.bssid ?: "--",
            rssiDbm = info.rssi,
            linkSpeedMbps = info.linkSpeed,
            frequencyMhz = frequency,
            channel = channel,
            is5Ghz = is5Ghz,
            gatewayIp = gateway,
            deviceIp = deviceIp,
            signalQuality = quality
        )
    }

    /**
     * Channel congestion from nearby access points. Requires
     * ACCESS_FINE_LOCATION and a completed [WifiManager.startScan]; Android
     * throttles how often third-party apps may trigger active scans
     * (a handful of scans per app per 2 minutes on API 28+), so this reads
     * the most recent scan results rather than forcing a new scan every call.
     */
    @Suppress("MissingPermission")
    suspend fun getChannelCongestion(): List<ChannelCongestion> = withContext(Dispatchers.Default) {
        val results = runCatching { wifiManager.scanResults }.getOrDefault(emptyList())
        val counts = (1..13).associateWith { 0 }.toMutableMap()
        results.forEach { r ->
            val ch = frequencyToChannel(r.frequency)
            if (ch in 1..13) counts[ch] = (counts[ch] ?: 0) + 1
        }
        counts.map { (ch, count) -> ChannelCongestion(ch, count, isBusy = count >= 4) }
            .sortedBy { it.channel }
    }

    fun requestScan(): Boolean = runCatching { wifiManager.startScan() }.getOrDefault(false)

    fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun intToIp(ip: Int): String = Formatter.formatIpAddress(ip)

    private fun rssiToQuality(rssi: Int): SignalQuality = when {
        rssi >= -55 -> SignalQuality.EXCELLENT
        rssi >= -67 -> SignalQuality.GOOD
        rssi >= -75 -> SignalQuality.FAIR
        rssi > -100 -> SignalQuality.WEAK
        else -> SignalQuality.UNKNOWN
    }

    /** Maps a WiFi frequency (MHz) to its 2.4/5 GHz channel number. */
    private fun frequencyToChannel(freqMhz: Int): Int = when {
        freqMhz == 2484 -> 14
        freqMhz in 2412..2472 -> (freqMhz - 2412) / 5 + 1
        freqMhz in 5170..5825 -> (freqMhz - 5000) / 5
        else -> -1
    }
}
