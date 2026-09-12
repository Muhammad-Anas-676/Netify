package com.netpulse.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netpulse.android.core.NetworkInfoProvider
import com.netpulse.android.core.NetworkSnapshot
import com.netpulse.android.core.SpeedResult
import com.netpulse.android.core.SpeedTestEngine
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val httpClient = HttpClient(OkHttp)
    private val speedTestEngine by lazy { SpeedTestEngine(httpClient) }
    private lateinit var networkInfoProvider: NetworkInfoProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        networkInfoProvider = NetworkInfoProvider(applicationContext)

        setContent {
            MaterialTheme {
                SpeedScreen(
                    onRunTest = { speedTestEngine.runFullTest() },
                    onReadNetwork = { networkInfoProvider.currentSnapshot() }
                )
            }
        }
    }
}

@Composable
fun SpeedScreen(
    onRunTest: suspend () -> SpeedResult?,
    onReadNetwork: () -> NetworkSnapshot
) {
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<SpeedResult?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Tap Start to measure your real connection") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Netify", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        Text(
            text = result?.let { "${"%.1f".format(it.downloadMbps)} Mbps" } ?: "--",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(status, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))

        Button(
            enabled = !isTesting,
            onClick = {
                scope.launch {
                    isTesting = true
                    status = "Testing…"
                    val run = onRunTest()
                    if (run != null) {
                        result = run
                        status = "Done — ↓ ${"%.1f".format(run.downloadMbps)} · ↑ ${"%.1f".format(run.uploadMbps)} Mbps · ${run.pingMs.toInt()} ms"
                        // TODO: persist `run` into a Room-backed history store
                    } else {
                        status = "Test failed — check your connection"
                    }
                    isTesting = false
                }
            }
        ) {
            Text(if (isTesting) "Testing…" else "Start Test")
        }

        Spacer(Modifier.height(32.dp))

        OutlinedButton(onClick = {
            val snap = onReadNetwork()
            status = "SSID: ${snap.ssid ?: "unavailable"} · online: ${snap.isOnline}"
        }) {
            Text("Read network info")
        }
    }
}
