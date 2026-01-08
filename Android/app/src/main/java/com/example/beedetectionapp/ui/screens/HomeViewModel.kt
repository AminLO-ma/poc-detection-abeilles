package com.example.beedetectionapp.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.beedetectionapp.data.model.TFLiteObjectDetector
import com.example.beedetectionapp.domain.model.DetectionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {

    private val _detectionResults = MutableStateFlow<List<DetectionResult>>(emptyList())
    val detectionResults: StateFlow<List<DetectionResult>> = _detectionResults.asStateFlow()

    private var detector: TFLiteObjectDetector? = null

    private var lastDetections: List<DetectionResult> = emptyList()
    private var lastDetectionTime: Long = 0

    // Temps (en ms) pendant lequel on garde le carré même si l'IA perd la cible
    private val PERSISTENCE_TIME_MS = 300L

    // Init détecteur (unique)
    fun initDetector(context: Context) {
        if (detector == null) {
            try {
                detector = TFLiteObjectDetector(context.applicationContext)
                android.util.Log.d("BEE_APP", "Modèle chargé avec succès")
            } catch (e: Exception) {
                android.util.Log.e("BEE_APP", "ERREUR FATALE CHARGEMENT MODELE", e)
            }
        }
    }

    fun onFrameReceived(results: List<DetectionResult>) {
        val currentTime = System.currentTimeMillis()

        if (results.isNotEmpty()) {
            _detectionResults.value = results
            lastDetections = results
            lastDetectionTime = currentTime
        } else {
            if (currentTime - lastDetectionTime < PERSISTENCE_TIME_MS) {
                _detectionResults.value = lastDetections
            } else {
                _detectionResults.value = emptyList()
            }
        }
    }

    fun getDetector(): TFLiteObjectDetector? = detector
}