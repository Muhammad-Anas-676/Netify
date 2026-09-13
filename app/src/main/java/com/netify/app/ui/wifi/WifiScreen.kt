package com.netify.app.ui.wifi

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.netify.app.di.AppContainer
import com.netify.app.domain.model.SignalQuality

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WifiScreen(container: AppContainer) {
    val vm: WifiViewModel = viewModel(factory = WifiViewModel.factory(container))
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(locationPermission.status.isGranted) {
        vm.onLocationPermissionResult(locationPermission.status.isGranted)
    }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (granted) vm.onLocationPermissionResult(true) else locationPermission.launchPermissionRequest()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        item { Text("WiFi", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 12.dp)) }

        item {
            val info = state.info
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    if (info == null || !info.isConnected) {
                        Text("Not connected to WiFi", fontWeight = FontWeight.Bold)
                        Text("Connect to a network to see live details.", style = MaterialTheme.typography.labelSmall)
                    } else {
                        Text(info.ssid, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "${if (info.is5Ghz) "5 GHz" else "2.4 GHz"} · Channel ${info.channel}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        AssistChip(onClick = {}, label = { Text(qualityLabel(info.signalQuality) + " · ${info.rssiDbm} dBm") })
                        Spacer(Modifier.height(10.dp))
                        InfoRow("Link speed", "${info.linkSpeedMbps} Mbps")
                        InfoRow("Gateway", info.gatewayIp)
                        InfoRow("Your IP", info.deviceIp)
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Channel congestion", fontWeight = FontWeight.Bold)
                    Text("Nearby networks on 2.4 GHz", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth().height(60.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.channels.forEach { c ->
                            val heightFraction = (c.apCount / 8f).coerceIn(0.08f, 1f)
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight(heightFraction)
                                    .background(
                                        if (c.isBusy) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                    )
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (state.channelAdvice.isNotBlank()) {
                        Text(state.channelAdvice, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Devices on this network", fontWeight = FontWeight.Bold)
                            Text("${state.devices.size} found", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { vm.scanDevices() }, enabled = !state.isScanningDevices) {
                            Text(if (state.isScanningDevices) "Scanning…" else "Rescan")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    state.devices.forEach { d ->
                        InfoRow(d.hostname ?: "Unknown device", d.ip)
                    }
                    if (state.devices.isEmpty() && !state.isScanningDevices) {
                        Text(
                            "Tap Rescan to sweep your local network for connected devices.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(key: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private fun qualityLabel(q: SignalQuality) = when (q) {
    SignalQuality.EXCELLENT -> "Excellent"
    SignalQuality.GOOD -> "Good"
    SignalQuality.FAIR -> "Fair"
    SignalQuality.WEAK -> "Weak"
    SignalQuality.UNKNOWN -> "Unknown"
}
