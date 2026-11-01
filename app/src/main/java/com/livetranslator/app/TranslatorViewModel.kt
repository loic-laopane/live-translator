package com.livetranslator.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TranslatorUiState(
    val input: String = "",
    val targetLanguage: String = Languages.defaultTarget(),
    val detectedLanguage: String? = null,
    val translation: String = "",
    val status: Status = Status.Idle,
)

sealed interface Status {
    data object Idle : Status
    data object Translating : Status
    data class DownloadingModel(val source: String, val target: String) : Status
    data object LanguageNotDetected : Status
    data class Error(val message: String) : Status
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = LiveTranslatorEngine()
    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val inputFlow = MutableStateFlow("")
    private val targetFlow = MutableStateFlow(
        prefs.getString(KEY_TARGET, null) ?: Languages.defaultTarget()
    )

    private val _state = MutableStateFlow(TranslatorUiState(targetLanguage = targetFlow.value))
    val state: StateFlow<TranslatorUiState> = _state.asStateFlow()

    /** Last language confidently detected; used as fallback for very short / ambiguous input. */
    private var lastDetected: String? = null

    init {
        viewModelScope.launch {
            combine(inputFlow.debounce(DEBOUNCE_MS), targetFlow) { text, target -> text to target }
                .distinctUntilChanged()
                .mapLatest { (text, target) -> process(text, target) }
                .collect { }
        }
    }

    fun onInputChange(text: String) {
        _state.update { it.copy(input = text) }
        inputFlow.value = text
    }

    fun onTargetChange(code: String) {
        prefs.edit().putString(KEY_TARGET, code).apply()
        _state.update { it.copy(targetLanguage = code) }
        targetFlow.value = code
    }

    fun clear() = onInputChange("")

    private suspend fun process(text: String, target: String) {
        if (text.isBlank()) {
            lastDetected = null
            _state.update { it.copy(translation = "", detectedLanguage = null, status = Status.Idle) }
            return
        }
        try {
            val detected = engine.detectLanguage(text)
            if (detected != null) lastDetected = detected
            val source = detected ?: lastDetected
            if (source == null) {
                _state.update { it.copy(detectedLanguage = null, status = Status.LanguageNotDetected) }
                return
            }
            _state.update {
                it.copy(
                    detectedLanguage = source,
                    status = if (engine.isModelReady(source, target) || source == target) {
                        Status.Translating
                    } else {
                        Status.DownloadingModel(source, target)
                    },
                )
            }
            val result = engine.translate(text, source, target)
            _state.update { it.copy(translation = result, status = Status.Idle) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(status = Status.Error(e.localizedMessage ?: e.toString())) }
        }
    }

    override fun onCleared() {
        engine.close()
    }

    companion object {
        private const val DEBOUNCE_MS = 350L
        private const val KEY_TARGET = "target_language"
    }
}
