package com.example.beedetectionapp.data.analyzer

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.beedetectionapp.data.model.TFLiteObjectDetector
import com.example.beedetectionapp.domain.model.DetectionResult

class BeeImageAnalyzer(
    private val detector: TFLiteObjectDetector,
    private val onResults: (List<DetectionResult>) -> Unit
) : ImageAnalysis.Analyzer {

    // Optimisation mémoire
    private val matrix = Matrix()

    override fun analyze(image: ImageProxy) {
        // Conversion directe
        val bitmap = image.toBitmap()

        if (bitmap != null) {
            val rotation = image.imageInfo.rotationDegrees

            // Gestion rotation
            val rotatedBitmap = if (rotation != 0) {
                matrix.reset()
                matrix.postRotate(rotation.toFloat())
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            val results = detector.detect(rotatedBitmap)
            onResults(results)
        }

        image.close()
    }
}