package com.netify.app.domain

import com.netify.app.domain.model.DiscoveredDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Best-effort local network device scanner.
 *
 * There is no public, non-root Android API to read the router's ARP/DHCP
 * client table, so this performs a reachability sweep of the /24 subnet
 * (254 addresses, checked in parallel with a short timeout) — the same
 * technique used by most non-root "network scanner" apps. It will find
 * devices that respond to a ping/TCP probe; devices that block ICMP or are
 * asleep may be missed. Hostnames are resolved via reverse DNS where the
 * router/device provides one, and are frequently unavailable.
 */
class DeviceScanner {

    suspend fun scanSubnet(deviceIp: String, timeoutMs: Int = 300): List<DiscoveredDevice> =
        withContext(Dispatchers.IO) {
            if (deviceIp.isBlank() || deviceIp == "--") return@withContext emptyList()
            val parts = deviceIp.split(".")
            if (parts.size != 4) return@withContext emptyList()
            val prefix = "${parts[0]}.${parts[1]}.${parts[2]}."

            val jobs = (1..254).map { host ->
                async {
                    val ip = "$prefix$host"
                    try {
                        val addr = InetAddress.getByName(ip)
                        if (addr.isReachable(timeoutMs)) {
                            val hostname = addr.canonicalHostName.takeIf { it != ip }
                            DiscoveredDevice(ip = ip, hostname = hostname, isLikelyThisDevice = ip == deviceIp)
                        } else null
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            jobs.awaitAll().filterNotNull().sortedBy {
                it.ip.substringAfterLast(".").toIntOrNull() ?: 0
            }
        }

    /** Local device's own IP on the current WiFi interface, as a fallback source. */
    fun getLocalIpFallback(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains(":") == false }
            ?.hostAddress
    }.getOrNull()
}
