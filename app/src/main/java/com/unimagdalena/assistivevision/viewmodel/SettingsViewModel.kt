package com.unimagdalena.assistivevision.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unimagdalena.assistivevision.data.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsManager(application)

    val language    = settings.language.stateIn(viewModelScope, SharingStarted.Eagerly, "es")
    val confidence  = settings.confidence.stateIn(viewModelScope, SharingStarted.Eagerly, 0.30f)
    val maxObjects  = settings.maxObjects.stateIn(viewModelScope, SharingStarted.Eagerly, 2)
    val showPreview = settings.showPreview.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val speechRate  = settings.speechRate.stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    fun setLanguage(value: String)        = viewModelScope.launch { settings.setLanguage(value) }
    fun setConfidence(value: Float)       = viewModelScope.launch { settings.setConfidence(value) }
    fun setMaxObjects(value: Int)         = viewModelScope.launch { settings.setMaxObjects(value) }
    fun setShowPreview(value: Boolean)    = viewModelScope.launch { settings.setShowPreview(value) }
    fun setSpeechRate(value: Float)       = viewModelScope.launch { settings.setSpeechRate(value) }
}