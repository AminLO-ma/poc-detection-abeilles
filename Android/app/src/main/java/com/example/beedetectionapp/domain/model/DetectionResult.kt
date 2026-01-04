package com.example.beedetectionapp.domain.model

import android.graphics.RectF

data class DetectionResult(
    val boundingBox: RectF, // Coordonnées box
    val score: Float,       // Confiance
    val label: String       // Label
)