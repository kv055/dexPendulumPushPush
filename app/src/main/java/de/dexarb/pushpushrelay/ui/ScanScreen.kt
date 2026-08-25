package de.dexarb.pushpushrelay.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.dexarb.pushpushrelay.R
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun ScanScreen(onQrScanned: (String) -> Boolean) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var deniedPermanently by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) {
            deniedPermanently = activity == null ||
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
        }
    }
    if (!hasPermission) {
        LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Camera permission is required to scan the pairing QR")
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                if (deniedPermanently) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                    )
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }) {
                Text(if (deniedPermanently) "Open app settings" else "Grant camera permission")
            }
        }
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
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Image(
            painter = painterResource(
                if (isSystemInDarkTheme()) R.drawable.brand_mark_dark else R.drawable.brand_mark_light
            ),
            contentDescription = "DexArb pendulum mark",
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(48.dp))
        Box(
            Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(3.dp, BrandPeach, RoundedCornerShape(16.dp))
        ) {
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
        }
        Spacer(Modifier.height(32.dp))
        Text("Scan the pairing QR from the Communication tab", textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
    }
}
