package com.example.beedetectionapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.beedetectionapp.data.analyzer.BeeImageAnalyzer
import com.example.beedetectionapp.ui.components.BeeCard
import com.example.beedetectionapp.ui.theme.HoneyGold
import java.util.concurrent.Executors

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permissions
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    // Init détecteur
    LaunchedEffect(Unit) {
        viewModel.initDetector(context)
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Caméra (Arrière-plan)
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        // Config prévisualisation
                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(previewView.surfaceProvider)

                        // Config analyse
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(640, 640))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888) // Format requis
                            .build()

                        imageAnalysis.setAnalyzer(
                            Executors.newSingleThreadExecutor(),
                            BeeImageAnalyzer(viewModel.getDetector()!!) { results ->
                                viewModel.onFrameReceived(results)
                            }
                        )

                        // Liaison cycle de vie
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // 2. Overlay (Avant-plan)
            val detections by viewModel.detectionResults.collectAsState()

            val density = LocalDensity.current

            // Configuration Paint texte
            val textPaint = remember(density) {
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    // Conversion SP vers PX
                    textSize = density.run { 20.sp.toPx() }
                    isAntiAlias = true
                    // Typeface gras
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                }
            }

            // Canvas des bounding boxes
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Facteur d'échelle
                val scale = maxOf(size.width / 640f, size.height / 640f)

                // On calcule le décalage pour centrer l'image virtuelle
                val offsetX = (size.width - 640f * scale) / 2
                val offsetY = (size.height - 640f * scale) / 2

                for (result in detections) {
                    val box = result.boundingBox
                    // Ajustement coordonnées
                    val left = box.left * scale + offsetX
                    val top = box.top * scale + offsetY
                    val right = box.right * scale + offsetX
                    val bottom = box.bottom * scale + offsetY

                    val width = right - left
                    val height = bottom - top

                    // 1. Contour boîte
                    drawRoundRect(
                        color = HoneyGold,
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(width, height),
                        style = Stroke(width = 8f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                    )

                    // 2. Fond semi-transparent
                    drawRoundRect(
                        color = HoneyGold.copy(alpha = 0.2f),
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(width, height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                    )

                    // 3. Texte label/score
                    val confidence = (result.score * 100).toInt()
                    val label = "Abeille $confidence%"

                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        left,     // Position X
                        top - 10, // Position Y
                        textPaint
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp), // Marge status bar
                contentAlignment = Alignment.TopCenter
            ) {
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
                    contentColor = androidx.compose.ui.graphics.Color.White
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Indicateur status
                        val dotColor =
                            if (detections.isNotEmpty()) HoneyGold else androidx.compose.ui.graphics.Color.Gray

                        Canvas(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(8.dp)
                        ) {
                            drawCircle(dotColor)
                        }

                        Text(
                            text = if (detections.isNotEmpty()) "${detections.size} Abeille(s) !" else "Recherche en cours...",
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // 3. HUD
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .padding(bottom = 32.dp) // Marge navigation
            ) {
                // Carte détection
                BeeCard(
                    title = "Détection",
                    // Texte dynamique
                    value = if (detections.isEmpty()) "Aucune" else "${detections.size} détectée(s)",
                    // On peut passer une couleur d'icône ou de texte différente au composant BeeCard si prévu
                    // tint = if (detections.isNotEmpty()) HoneyGold else Color.Gray
                )

                // Aide si vide
                if (detections.isEmpty()) {
                    Text(
                        text = "Pointez la caméra vers une ruche ou une fleur",
                        color = Color.White.copy(alpha = 0.8f),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    } else {
        // Écran permission manquante
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                Text("Autoriser la caméra")
            }
        }
    }
}