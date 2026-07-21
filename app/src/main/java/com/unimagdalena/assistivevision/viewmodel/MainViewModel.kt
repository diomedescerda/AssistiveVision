package com.unimagdalena.assistivevision.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.unimagdalena.assistivevision.model.Detection
import com.unimagdalena.assistivevision.modules.DetectionModule
import com.unimagdalena.assistivevision.modules.LocalizationModule
import com.unimagdalena.assistivevision.modules.PreprocessingModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DetectionMode { AUTO, MANUAL }

class MainViewModel : ViewModel() {
    private lateinit var preprocessingModule: PreprocessingModule
    private lateinit var detectionModule: DetectionModule
    private lateinit var localizationModule: LocalizationModule

    private var latestBitmap: Bitmap? = null

    private val _detections = MutableStateFlow<List<Detection>>(emptyList())
    val detections: StateFlow<List<Detection>> = _detections.asStateFlow()

    private val _mode = MutableStateFlow(DetectionMode.AUTO)
    val mode: StateFlow<DetectionMode> = _mode.asStateFlow()

    companion object {
        private const val INPUT_SIZE = 448
    }

    fun initialize(context: Context) {
        preprocessingModule = PreprocessingModule()
        detectionModule = DetectionModule(context)
        localizationModule = LocalizationModule()
    }

    fun processFrame(bitmap: Bitmap) {
        latestBitmap = bitmap
        if (_mode.value == DetectionMode.AUTO) {
            runDetection(bitmap)
        }
    }

    fun captureManualFrame() {
        latestBitmap?.let { runDetection(it) }
    }

    fun setMode(mode: DetectionMode) {
        _mode.value = mode
        if (mode == DetectionMode.MANUAL) {
            _detections.value = emptyList()
        }
    }

    private fun runDetection(bitmap: Bitmap) {
        val buffer = preprocessingModule.preprocess(bitmap)
        val detected = detectionModule.detect(buffer)
        _detections.value = localizationModule.localize(detected, INPUT_SIZE, INPUT_SIZE)
    }

    override fun onCleared() {
        super.onCleared()
        if (::detectionModule.isInitialized) detectionModule.close()
    }
}
