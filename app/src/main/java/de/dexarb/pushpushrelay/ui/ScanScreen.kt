package de.dexarb.pushpushrelay.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun ScanScreen(onQrScanned: (String) -> Boolean) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    if (!hasPermission) {
        LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Camera permission required") }
        return
    }
    ScanCamera(onQrScanned)
}

@Composable
private fun ScanCamera(onQrScanned: (String) -> Boolean) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }
    var handled by remember { mutableStateOf(false) }
    var cameraReady by remember { mutableStateOf(false) }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
    }
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val analysis = ImageAnalysis.Builder().build().also { analysis ->
                        @androidx.camera.core.ExperimentalGetImage
                        analysis.setAnalyzer(executor) { proxy ->
                            val image = proxy.image ?: run { proxy.close(); return@setAnalyzer }
                            val input = InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees)
                            scanner.process(input)
                                .addOnSuccessListener { codes ->
                                    codes.firstOrNull()?.rawValue?.let { raw ->
                                        if (!handled && onQrScanned(raw)) {
                                            handled = true
                                        }
                                    }
                                }
                                .addOnCompleteListener { proxy.close() }
                        }
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    cameraReady = true
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
        )
        if (!cameraReady) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PendulumSpinner()
            }
        }
        Text(
            "Scan the pairing QR from the Communication tab",
            Modifier.align(Alignment.BottomCenter).padding(24.dp),
        )
    }
}
