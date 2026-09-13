package com.netify.app.ui.vault

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netify.app.di.AppContainer
import com.netify.app.domain.QrHelper
import com.netify.app.domain.model.VaultEntry

@Composable
fun VaultScreen(container: AppContainer, onScanQr: () -> Unit) {
    val vm: VaultViewModel = viewModel(factory = VaultViewModel.factory(container))
    val entries by vm.entries.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var qrEntry by remember { mutableStateOf<VaultEntry?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)) {
        item {
            Text("Password Vault", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp))
            Text(
                "Saved locally, encrypted on this device only",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showAddDialog = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Add network")
                }
                OutlinedButton(onClick = onScanQr, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Scan QR")
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (entries.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No saved networks yet. Add one manually or scan a WiFi QR code.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(entries) { entry ->
                Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(entry.ssid, fontWeight = FontWeight.Bold)
                            Text("•".repeat(entry.password.length.coerceAtMost(10)), style = MaterialTheme.typography.labelSmall)
                        }
                        Row {
                            IconButton(onClick = { qrEntry = entry }) {
                                Icon(Icons.Filled.QrCode, contentDescription = "Show QR")
                            }
                            IconButton(onClick = { vm.deleteEntry(entry.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Netify stores these passwords in its own encrypted vault. Android does not allow any app — including this one — to read passwords already saved in your phone's WiFi settings.",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    if (showAddDialog) {
        AddNetworkDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { ssid, pass, sec -> vm.addEntry(ssid, pass, sec); showAddDialog = false }
        )
    }

    qrEntry?.let { entry ->
        QrPreviewDialog(entry = entry, onDismiss = { qrEntry = null })
    }
}

@Composable
private fun AddNetworkDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add network") },
        text = {
            Column {
                OutlinedTextField(value = ssid, onValueChange = { ssid = it }, label = { Text("Network name (SSID)") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
            }
        },
        confirmButton = {
            TextButton(onClick = { if (ssid.isNotBlank()) onConfirm(ssid, password, "WPA") }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun QrPreviewDialog(entry: VaultEntry, onDismiss: () -> Unit) {
    val content = remember(entry) { QrHelper.buildWifiQrContent(entry.ssid, entry.password, entry.security) }
    val bitmap = remember(content) { QrHelper.generateQrBitmap(content, 480).asImageBitmap() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.ssid) },
        text = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Image(bitmap = bitmap, contentDescription = "WiFi QR code", modifier = Modifier.size(220.dp))
                Spacer(Modifier.height(8.dp))
                Text("Scan with any camera to join this network.", style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
