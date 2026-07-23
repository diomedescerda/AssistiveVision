package com.unimagdalena.assistivevision.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unimagdalena.assistivevision.R
import com.unimagdalena.assistivevision.modules.DetectionModule
import com.unimagdalena.assistivevision.modules.LocalizationModule
import com.unimagdalena.assistivevision.modules.PreprocessingModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class MainViewModel : ViewModel() {

    private lateinit var preprocessingModule: PreprocessingModule
    private lateinit var detectionModule: DetectionModule
    private val localizationModule = LocalizationModule()
    private lateinit var appContext: Context

    private var isAutomatic = true
    private var lastBitmap: Bitmap? = null

    private val _detectionCount = MutableLiveData(0)
    val detectionCount: LiveData<Int> = _detectionCount

    private val _zoneLabel = MutableLiveData("")
    val zoneLabel: LiveData<String> = _zoneLabel

    private val _fps = MutableLiveData(0f)
    val fps: LiveData<Float> = _fps

    private var lastFrameTime = System.currentTimeMillis()
    private val pipelineMutex = Mutex()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        preprocessingModule = PreprocessingModule()
        detectionModule = DetectionModule(context)
    }

    fun setAutomatic(automatic: Boolean) {
        isAutomatic = automatic
    }

    fun processFrame(bitmap: Bitmap) {
        lastBitmap = bitmap
        if (isAutomatic) runPipeline(bitmap)
    }

    fun speakNow() {
        lastBitmap?.let { runPipeline(it) }
    }

    private fun runPipeline(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            if (!pipelineMutex.tryLock()) return@launch
            try {
                val now = System.currentTimeMillis()
                val elapsed = now - lastFrameTime
                lastFrameTime = now
                val currentFps = if (elapsed > 0) 1000f / elapsed else 0f

                val tensor = preprocessingModule.preprocess(bitmap)
                val detections = detectionModule.detect(tensor)
                val localized = localizationModule.localize(
                    detections,
                    bitmap.width,
                    bitmap.height
                )

                _detectionCount.postValue(localized.size)
                _fps.postValue(currentFps)

                if (localized.isNotEmpty()) {
                    val top = localized.first()
                    _zoneLabel.postValue("${top.className} · ${top.zone}")
                } else {
                    _zoneLabel.postValue(appContext.getString(R.string.no_detections))
                }
            } finally {
                pipelineMutex.unlock()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        detectionModule.close()
    }
}