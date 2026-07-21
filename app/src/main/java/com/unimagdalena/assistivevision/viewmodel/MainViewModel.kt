package com.unimagdalena.assistivevision.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.unimagdalena.assistivevision.model.Detection
import com.unimagdalena.assistivevision.modules.DetectionModule
import com.unimagdalena.assistivevision.modules.PreprocessingModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private lateinit var preprocessingModule: PreprocessingModule
    private lateinit var detectionModule: DetectionModule

    private val _detections = MutableStateFlow<List<Detection>>(emptyList())
    val detections: StateFlow<List<Detection>> = _detections.asStateFlow()

    fun initialize(context: Context) {
        preprocessingModule = PreprocessingModule()
        detectionModule = DetectionModule(context)
    }

    fun processFrame(bitmap: Bitmap) {
        val buffer = preprocessingModule.preprocess(bitmap)
        _detections.value = detectionModule.detect(buffer)
    }

    override fun onCleared() {
        super.onCleared()
        if (::detectionModule.isInitialized) detectionModule.close()
    }
}
