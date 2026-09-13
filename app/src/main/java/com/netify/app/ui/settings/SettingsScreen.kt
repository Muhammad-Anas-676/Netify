package com.netify.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netify.app.data.datastore.ThemeMode
import com.netify.app.di.AppContainer

@Composable
fun SettingsScreen(container: AppContainer) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 12.dp)) }

        item {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Appearance", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    SegmentedRow(
                        options = listOf("Light" to ThemeMode.LIGHT, "Dark" to ThemeMode.DARK, "System" to ThemeMode.SYSTEM),
                        selected = state.theme,
                        onSelect = { vm.setTheme(it) }
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Speed units", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    SegmentedRow(
                        options = listOf("Mbps" to true, "MBps" to false),
                        selected = state.unitsMbps,
                        onSelect = { vm.setUnitsMbps(it) }
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Language", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    SegmentedRow(
                        options = listOf("English" to "en", "اردو" to "ur", "हिन्दी" to "hi"),
                        selected = state.language,
                        onSelect = { vm.setLanguage(it) }
                    )
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("ISP plan & alerts", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    LabeledStepper("Plan speed", state.ispPlanMbps, "Mbps", step = 10) { vm.setIspPlan(it) }
                    Spacer(Modifier.height(10.dp))
                    LabeledStepper("Alert threshold", state.alertThresholdPct, "%", step = 5) { vm.setAlertThreshold(it) }

                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Daily auto-test", fontWeight = FontWeight.SemiBold)
                            Text("Runs at ${state.autoTestHour}:00", style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(
                            checked = state.autoTestEnabled,
                            onCheckedChange = { vm.setAutoTestEnabled(it, context.applicationContext) }
                        )
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("About", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Version"); Text("1.0.0", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> SegmentedRow(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (label, value) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LabeledStepper(label: String, value: Int, unit: String, step: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = { onChange((value - step).coerceAtLeast(0)) }) { Text("–") }
            Text("$value $unit", modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = { onChange(value + step) }) { Text("+") }
        }
    }
}
