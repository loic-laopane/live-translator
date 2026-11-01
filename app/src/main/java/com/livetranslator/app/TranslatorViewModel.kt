package com.livetranslator.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.nl.translate.TranslateLanguage
import com.livetranslator.app.grammar.CorrectedText
import com.livetranslator.app.grammar.Corrections
import com.livetranslator.app.grammar.GrammarIssue
import com.livetranslator.app.grammar.LanguageToolClient
import com.livetranslator.app.grammar.RateLimitedException
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
import java.util.Locale

data class TranslatorUiState(
    val input: String = "",
    val targetLanguage: String = Languages.defaultTarget(),
    val detectedLanguage: String? = null,
    val translation: String = "",
    val status: Status = Status.Idle,
    val grammar: GrammarUiState = GrammarUiState(),
)

sealed interface Status {
    data object Idle : Status
    data object Translating : Status
    data class DownloadingModel(val source: String, val target: String) : Status
    data object LanguageNotDetected : Status
    data class Error(val message: String) : Status
}

data class GrammarUiState(
    val enabled: Boolean = true,
    val checking: Boolean = false,
    /** The text the [issues] refer to. Corrections can only be applied while the input still matches it. */
    val checkedText: String = "",
    val issues: List<GrammarIssue> = emptyList(),
    val corrected: CorrectedText? = null,
    val error: String? = null,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = LiveTranslatorEngine()
    private val grammarClient = LanguageToolClient()
    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val inputFlow = MutableStateFlow("")
    private val targetFlow = MutableStateFlow(
        prefs.getString(KEY_TARGET, null) ?: Languages.defaultTarget()
    )
    private val grammarEnabledFlow = MutableStateFlow(prefs.getBoolean(KEY_GRAMMAR, true))

    private val _state = MutableStateFlow(
        TranslatorUiState(
            targetLanguage = targetFlow.value,
            grammar = GrammarUiState(enabled = grammarEnabledFlow.value),
        )
    )
    val state: StateFlow<TranslatorUiState> = _state.asStateFlow()

    /** Last language confidently detected; used as fallback for very short / ambiguous input. */
    private var lastDetected: String? = null

    /** Device language as an ML Kit code, used to explain grammar rules in the user's own language. */
    private val deviceLanguage: String? = TranslateLanguage.fromLanguageTag(Locale.getDefault().language)

    init {
        viewModelScope.launch {
            combine(inputFlow.debounce(DEBOUNCE_MS), targetFlow) { text, target -> text to target }
                .distinctUntilChanged()
                .mapLatest { (text, target) -> translate(text, target) }
                .collect { }
        }
        viewModelScope.launch {
            // Longer debounce: the grammar check is a network call with a per-minute quota.
            combine(inputFlow.debounce(GRAMMAR_DEBOUNCE_MS), grammarEnabledFlow) { text, on -> text to on }
                .distinctUntilChanged()
                .mapLatest { (text, on) -> checkGrammar(text, on) }
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

    fun onGrammarEnabledChange(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GRAMMAR, enabled).apply()
        _state.update { it.copy(grammar = GrammarUiState(enabled = enabled)) }
        grammarEnabledFlow.value = enabled
    }

    fun clear() = onInputChange("")

    /** Replaces the input by the fully corrected version. */
    fun applyAllCorrections() {
        val current = _state.value
        val corrected = current.grammar.corrected ?: return
        if (current.input != current.grammar.checkedText) return
        _state.update {
            it.copy(grammar = it.grammar.copy(checkedText = corrected.text, issues = emptyList(), corrected = null))
        }
        onInputChange(corrected.text)
    }

    /** Applies one suggestion for one issue, keeping the other issues in place. */
    fun applyCorrection(issue: GrammarIssue, replacement: String) {
        val current = _state.value
        if (current.input != current.grammar.checkedText) return
        val (newText, remaining) = Corrections.applyOne(current.input, current.grammar.issues, issue, replacement)
        _state.update {
            it.copy(grammar = it.grammar.copy(checkedText = newText, issues = remaining, corrected = correctedOrNull(newText, remaining)))
        }
        onInputChange(newText)
    }

    private suspend fun translate(text: String, target: String) {
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

    private suspend fun checkGrammar(text: String, enabled: Boolean) {
        if (!enabled || text.isBlank()) {
            _state.update { it.copy(grammar = GrammarUiState(enabled = enabled)) }
            return
        }
        _state.update { it.copy(grammar = it.grammar.copy(checking = true, error = null)) }
        try {
            val result = grammarClient.check(text, deviceLanguage)
            _state.update {
                it.copy(
                    grammar = GrammarUiState(
                        enabled = true,
                        checkedText = text,
                        issues = result.issues,
                        corrected = correctedOrNull(text, result.issues),
                    )
                )
            }
            explainInDeviceLanguage(text, result.languageCode, result.issues)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RateLimitedException) {
            _state.update { it.copy(grammar = it.grammar.copy(checking = false, error = "Trop de vérifications : patientez quelques secondes.")) }
        } catch (e: Exception) {
            _state.update { it.copy(grammar = it.grammar.copy(checking = false, error = "Correction indisponible (connexion ?)")) }
        }
    }

    /**
     * LanguageTool explains rules in the language of the text. When that is not the user's language,
     * translate the explanations on-device so that the rules stay understandable.
     */
    private suspend fun explainInDeviceLanguage(text: String, languageCode: String?, issues: List<GrammarIssue>) {
        val source = languageCode?.substringBefore('-')?.let { TranslateLanguage.fromLanguageTag(it) } ?: return
        val target = deviceLanguage ?: return
        if (source == target || issues.isEmpty()) return

        val translations = mutableMapOf<String, String?>()
        for (message in issues.map { it.message }.distinct()) {
            translations[message] = try {
                engine.translate(message, source, target)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
        _state.update { state ->
            if (state.grammar.checkedText != text) return@update state
            state.copy(
                grammar = state.grammar.copy(
                    issues = state.grammar.issues.map { it.copy(translatedMessage = translations[it.message]) },
                )
            )
        }
    }

    private fun correctedOrNull(text: String, issues: List<GrammarIssue>): CorrectedText? =
        if (issues.any { it.replacements.isNotEmpty() }) Corrections.correct(text, issues) else null

    override fun onCleared() {
        engine.close()
    }

    companion object {
        private const val DEBOUNCE_MS = 350L
        private const val GRAMMAR_DEBOUNCE_MS = 900L
        private const val KEY_TARGET = "target_language"
        private const val KEY_GRAMMAR = "grammar_enabled"
    }
}
