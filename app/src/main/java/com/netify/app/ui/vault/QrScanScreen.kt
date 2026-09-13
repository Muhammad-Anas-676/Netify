package com.netify.app.ui.vault

import android.Manifest
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.netify.app.di.AppContainer
import com.netify.app.domain.QrHelper

/**
 * Scans a standard WiFi QR code and offers to (a) save it into Netify's own
 * vault and/or (b) connect to it via Android's WifiNetworkSuggestion API —
 * the only supported, non-root way for a third-party app to add WiFi
 * credentials on Android 10+ (the OS shows a one-time system connect
 * prompt to the user, by design).
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QrScanScreen(container: AppContainer, onDone: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    var parsed by remember { mutableStateOf<QrHelper.ParsedWifiQr?>(null) }

    LaunchedEffect(Unit) { if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Scan WiFi QR") })

        if (!cameraPermission.status.isGranted) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text("Camera permission is needed to scan a QR code.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = { cameraPermission.launchPermissionRequest() }) { Text("Grant permission") }
            }
        } else if (parsed == null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        val preview = androidx.camera.core.Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        val scanner = BarcodeScanning.getClient()
                        analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy: ImageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        val raw = barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }?.rawValue
                                        if (raw != null) {
                                            QrHelper.parseWifiQr(raw)?.let { parsed = it }
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )
        } else {
            val result = parsed!!
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Text("Found network", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("SSID: ${result.ssid}")
                Text("Security: ${result.security}")
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        container.vaultStorage.add(result.ssid, result.password, result.security)
                        connectViaSuggestion(context, result)
                        onDone()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save & Connect") }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { parsed = null }, modifier = Modifier.fillMaxWidth()) { Text("Scan again") }
            }
        }
    }
}

private fun connectViaSuggestion(context: android.content.Context, parsed: QrHelper.ParsedWifiQr) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return // Older APIs use a different, deprecated flow.
    val wifiManager = context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager
    val builder = WifiNetworkSuggestion.Builder().setSsid(parsed.ssid)
    if (parsed.password.isNotBlank()) {
        when (parsed.security.uppercase()) {
            "WEP" -> { /* WEP suggestions are not supported by the platform API; ask the user to add manually. */ }
            else -> builder.setWpa2Passphrase(parsed.password)
        }
    }
    val suggestion = builder.build()
    runCatching {
        wifiManager.addNetworkSuggestions(listOf(suggestion))
    }
    // Android shows its own one-time system prompt asking the user to approve
    // this suggestion before it actually connects — this is expected OS
    // behavior on Android 10+, not an app bug.
}
