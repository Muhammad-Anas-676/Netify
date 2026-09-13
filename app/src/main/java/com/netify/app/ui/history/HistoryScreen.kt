package com.netify.app.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netify.app.di.AppContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(container: AppContainer) {
    val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(container))
    val entries by vm.entries.collectAsState()
    val dateFmt = remember { SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("History", style = MaterialTheme.typography.headlineSmall)
                if (entries.isNotEmpty()) {
                    TextButton(onClick = { vm.clearHistory() }) { Text("Clear") }
                }
            }
        }
        if (entries.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("No tests yet", fontWeight = FontWeight.Bold)
                        Text("Run a speed test to start building your history.", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } else {
            items(entries) { e ->
                Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(dateFmt.format(Date(e.timestamp)), style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("↓ %.1f".format(e.downloadMbps))
                            Text("↑ %.1f".format(e.uploadMbps))
                            Text("${e.pingMs.toInt()}ms")
                        }
                    }
                }
            }
        }
    }
}
