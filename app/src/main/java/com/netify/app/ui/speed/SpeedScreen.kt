package com.netify.app.ui.speed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netify.app.di.AppContainer
import com.netify.app.domain.SpeedTestEngine
import com.netify.app.domain.model.SpeedTestProgress
import com.netify.app.ui.theme.AccentBlue
import com.netify.app.ui.theme.AccentCyan
import com.netify.app.ui.theme.GoodGreen
import kotlin.math.min

@Composable
fun SpeedScreen(container: AppContainer) {
    val vm: SpeedViewModel = viewModel(factory = SpeedViewModel.factory(container))
    val state by vm.state.collectAsState()
    val server = SpeedTestEngine.DEFAULT_SERVERS[state.serverIndex]

    val liveMbps = when (val p = state.progress) {
        is SpeedTestProgress.Downloading -> p.currentMbps
        is SpeedTestProgress.Done -> p.result.downloadMbps
        else -> 0.0
    }
    val statusText = when (val p = state.progress) {
        SpeedTestProgress.Idle -> "Tap start to measure your connection"
        SpeedTestProgress.MeasuringPing -> "Measuring ping…"
        is SpeedTestProgress.Downloading -> "Measuring download…"
        is SpeedTestProgress.Uploading -> "Measuring upload…"
        is SpeedTestProgress.Done -> "Done — tap to test again"
        is SpeedTestProgress.Failed -> p.reason
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Testing via", style = MaterialTheme.typography.labelSmall)
                            Text(server.name, fontWeight = FontWeight.Bold)
                        }
                        AssistChip(onClick = { vm.cycleServer() }, label = { Text("Change") })
                    }

                    Spacer(Modifier.height(16.dp))

                    Box(Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
                        SpeedGauge(valueMbps = liveMbps, maxScale = 200.0)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (liveMbps > 0) "%.1f".format(liveMbps) else "--",
                                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 36.sp)
                            )
                            Text("Mbps", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text(
                        statusText,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = { vm.runTest() },
                        enabled = !state.isRunning,
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                    ) { Text(if (state.isRunning) "Testing…" else "Start Test") }

                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCell("Download", state.lastDownload?.let { "%.1f".format(it) } ?: "--", "Mbps", Modifier.weight(1f))
                        StatCell("Upload", state.lastUpload?.let { "%.1f".format(it) } ?: "--", "Mbps", Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCell("Ping", state.lastPing?.let { "%.0f".format(it) } ?: "--", "ms", Modifier.weight(1f))
                        StatCell("Jitter", state.lastJitter?.let { "%.0f".format(it) } ?: "--", "ms", Modifier.weight(1f))
                    }

                    if (state.lastDownload != null && state.ispPlanMbps > 0) {
                        val pct = ((state.lastDownload!! / state.ispPlanMbps) * 100).toInt()
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = GoodGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("$pct% of your plan", fontWeight = FontWeight.Bold, color = GoodGreen)
                                Text(
                                    "Plan: ${state.ispPlanMbps} Mbps",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp) {
        Column(Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Row {
                Text(value, fontWeight = FontWeight.Bold)
                Text(" $unit", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SpeedGauge(valueMbps: Double, maxScale: Double) {
    val clamped = min(valueMbps, maxScale)
    val sweep = (clamped / maxScale) * 270.0
    Canvas(modifier = Modifier.size(196.dp)) {
        val stroke = Stroke(width = 18f, cap = StrokeCap.Round)
        drawArc(
            color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.3f),
            startAngle = 135f, sweepAngle = 270f, useCenter = false,
            style = stroke, size = Size(size.width, size.height)
        )
        drawArc(
            brush = Brush.linearGradient(listOf(AccentCyan, AccentBlue)),
            startAngle = 135f, sweepAngle = sweep.toFloat(), useCenter = false,
            style = stroke, size = Size(size.width, size.height)
        )
    }
}

