package com.unimagdalena.assistivevision.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.unimagdalena.assistivevision.R
import com.unimagdalena.assistivevision.data.SettingsManager
import com.unimagdalena.assistivevision.model.Detection
import com.unimagdalena.assistivevision.model.LocalizedLabels
import com.unimagdalena.assistivevision.model.SpeechFormatter
import com.unimagdalena.assistivevision.modules.AudioModule
import com.unimagdalena.assistivevision.modules.DetectionModule
import com.unimagdalena.assistivevision.modules.LocalizationModule
import com.unimagdalena.assistivevision.modules.PreprocessingModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsManager(application)

    private lateinit var preprocessingModule: PreprocessingModule
    private lateinit var detectionModule: DetectionModule
    private val localizationModule = LocalizationModule()
    private lateinit var audioModule: AudioModule
    private lateinit var appContext: Context

    private var lastBitmap: Bitmap? = null

    private val _detectionCount = MutableLiveData(0)
    val detectionCount: LiveData<Int> = _detectionCount

    private val _zoneLabel = MutableLiveData("")
    val zoneLabel: LiveData<String> = _zoneLabel

    private val _detections = MutableLiveData<List<Detection>>(emptyList())
    val detections: LiveData<List<Detection>> = _detections

    private val _frameSize = MutableLiveData<Pair<Int, Int>>(0 to 0)
    val frameSize: LiveData<Pair<Int, Int>> = _frameSize

    private val _showPreview = MutableLiveData(true)
    val showPreview: LiveData<Boolean> = _showPreview

    private val _isCapturing = MutableLiveData(false)
    val isCapturing: LiveData<Boolean> = _isCapturing

    private val _frozenFrame = MutableLiveData<Bitmap?>(null)
    val frozenFrame: LiveData<Bitmap?> = _frozenFrame

    private val _confidence = MutableStateFlow(0.30f)
    private val _maxObjects = MutableStateFlow(2)

    private val pipelineMutex = Mutex()

    init {
        appContext = application
        preprocessingModule = PreprocessingModule()
        detectionModule = DetectionModule(application)
        audioModule = AudioModule(application)
        audioModule.listener = object : AudioModule.Listener {
            override fun onDone() = unfreeze()
            override fun onError() = unfreeze()
        }

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
            settings.language.collect { audioModule.setLanguage(it) }
        }
        viewModelScope.launch {
            settings.speechRate.collect { audioModule.setSpeechRate(it) }
        }
    }

    fun processFrame(bitmap: Bitmap) {
        lastBitmap = bitmap
        _frameSize.postValue(bitmap.width to bitmap.height)
    }

    fun captureFrame() {
        val bitmap = lastBitmap ?: return
        if (_isCapturing.value == true) return
        if (_frozenFrame.value != null) return
        _isCapturing.postValue(true)
        _frozenFrame.postValue(bitmap)
        runPipeline(bitmap)
    }

    private fun runPipeline(bitmap: Bitmap) {
        val confidence = _confidence.value
        val maxObjects = _maxObjects.value
        val frameWidth = bitmap.width
        val frameHeight = bitmap.height

        viewModelScope.launch(Dispatchers.Default) {
            if (!pipelineMutex.tryLock()) return@launch
            try {
                val tensor = preprocessingModule.preprocess(bitmap)
                val detections = detectionModule.detect(tensor, confidence)
                val limited = detections.take(maxObjects)
                val inFrame = limited.map { toFrameCoordinates(it, frameWidth, frameHeight) }
                val localized = localizationModule.localize(inFrame, frameWidth, frameHeight)

                _detectionCount.postValue(localized.size)
                _detections.postValue(localized)

                if (localized.isNotEmpty()) {
                    val top = localized.first()
                    _zoneLabel.postValue("${top.className} · ${top.zone}")
                    val started = audioModule.speak(buildUtterance(localized))
                    if (!started) unfreeze()
                } else {
                    _zoneLabel.postValue(appContext.getString(R.string.no_detections))
                    unfreeze()
                }
            } finally {
                _isCapturing.postValue(false)
                pipelineMutex.unlock()
            }
        }
    }

    private fun unfreeze() {
        _frozenFrame.postValue(null)
        _detections.postValue(emptyList())
        _zoneLabel.postValue(appContext.getString(R.string.status_ready))
    }

    private fun toFrameCoordinates(detection: Detection, frameWidth: Int, frameHeight: Int): Detection {
        val inputSize = DetectionModule.INPUT_SIZE.toFloat()
        val scale = minOf(inputSize / frameWidth, inputSize / frameHeight)
        val dx = (inputSize - frameWidth * scale) / 2f
        val dy = (inputSize - frameHeight * scale) / 2f
        return Detection(
            classId = detection.classId,
            className = detection.className,
            score = detection.score,
            bbox = RectF(
                (detection.bbox.left - dx) / scale,
                (detection.bbox.top - dy) / scale,
                (detection.bbox.right - dx) / scale,
                (detection.bbox.bottom - dy) / scale
            ),
            zone = detection.zone
        )
    }

    private fun buildUtterance(localized: List<Detection>): String {
        val spoken = localized.map { detection ->
            LocalizedLabels.classLabel(appContext, detection.classId) to
                LocalizedLabels.zoneLabel(appContext, detection.zone)
        }
        return SpeechFormatter.announce(spoken)
    }

    override fun onCleared() {
        super.onCleared()
        audioModule.shutdown()
        detectionModule.close()
    }
}