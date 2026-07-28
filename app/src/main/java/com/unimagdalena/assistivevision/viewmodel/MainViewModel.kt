package com.unimagdalena.assistivevision.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.unimagdalena.assistivevision.R
import com.unimagdalena.assistivevision.data.SettingsManager
import com.unimagdalena.assistivevision.model.Detection
import com.unimagdalena.assistivevision.modules.DetectionModule
import com.unimagdalena.assistivevision.modules.LocalizationModule
import com.unimagdalena.assistivevision.modules.PreprocessingModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsManager(application)

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

    private val _detections = MutableLiveData<List<Detection>>(emptyList())
    val detections: LiveData<List<Detection>> = _detections

    private val _showPreview = MutableLiveData(true)
    val showPreview: LiveData<Boolean> = _showPreview

    private val _defaultAutoMode = MutableLiveData(true)
    val defaultAutoMode: LiveData<Boolean> = _defaultAutoMode

    private val _confidence = MutableStateFlow(0.30f)
    private val _maxObjects = MutableStateFlow(2)

    private var lastFrameTime = System.currentTimeMillis()
    private val pipelineMutex = Mutex()

    init {
        appContext = application
        preprocessingModule = PreprocessingModule()
        detectionModule = DetectionModule(application)

        viewModelScope.launch {
            settings.confidence.collect { _confidence.value = it }
        }
        viewModelScope.launch {
            settings.maxObjects.collect { _maxObjects.value = it }
        }
        viewModelScope.launch {
            settings.showPreview.collect { _showPreview.postValue(it) }
        }
        viewModelScope.launch {
            settings.defaultAutoMode.collect { _defaultAutoMode.postValue(it) }
        }
        viewModelScope.launch {
            settings.language.collect { /* locale applied on user action in SettingsFragment */ }
        }
        // TODO: Language change causes a black flash during activity recreation.
        // Options to fix:
        //  1. Add android:configChanges="locale|layoutDirection" to <activity> in
        //     AndroidManifest.xml, then handle locale in onConfigurationChanged()
        //  2. Use windowBackground in themes.xml to match the app background color
        //     so the flash blends in instead of showing black
        //  3. Apply locale only on next cold start (remove setApplicationLocales
        //     from SettingsFragment, keep only in MainActivity.applySavedLocale)
        viewModelScope.launch {
            settings.speechRate.collect { /* consumed by AudioModule when implemented */ }
        }
        viewModelScope.launch {
            settings.cooldown.collect { /* consumed by PriorityFilterModule when implemented */ }
        }
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
        val confidence = _confidence.value
        val maxObjects = _maxObjects.value

        viewModelScope.launch(Dispatchers.Default) {
            if (!pipelineMutex.tryLock()) return@launch
            try {
                val now = System.currentTimeMillis()
                val elapsed = now - lastFrameTime
                lastFrameTime = now
                val currentFps = if (elapsed > 0) 1000f / elapsed else 0f

                val tensor = preprocessingModule.preprocess(bitmap)
                val detections = detectionModule.detect(tensor, confidence)
                val limited = detections.take(maxObjects)
                val localized = localizationModule.localize(
                    limited,
                    bitmap.width,
                    bitmap.height
                )

                _detectionCount.postValue(localized.size)
                _detections.postValue(localized)
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
