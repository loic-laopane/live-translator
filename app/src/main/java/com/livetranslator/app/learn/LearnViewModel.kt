package com.livetranslator.app.learn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.nl.translate.TranslateLanguage
import com.livetranslator.app.LiveTranslatorEngine
import com.livetranslator.app.UserSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface LearnScreenState {
    data object Subjects : LearnScreenState
    data class LessonView(val subjectId: String, val lessonId: String) : LearnScreenState
    data class Quiz(
        val subjectId: String,
        val lessonId: String,
        val index: Int = 0,
        /** Option chosen for the current question, null while not answered. */
        val selected: Int? = null,
        val score: Int = 0,
        val finished: Boolean = false,
    ) : LearnScreenState
}

data class LessonTranslation(
    val translating: Boolean = false,
    /** Lesson translated into the native language; null when not needed or not available. */
    val lesson: Lesson? = null,
    val showOriginal: Boolean = false,
    val failed: Boolean = false,
)

data class LearnUiState(
    val subjects: List<Subject> = emptyList(),
    val levelFilter: Level? = null,
    /** Best quiz score per lesson id. */
    val progress: Map<String, Int> = emptyMap(),
    val screen: LearnScreenState = LearnScreenState.Subjects,
    val translation: LessonTranslation = LessonTranslation(),
)

class LearnViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CourseRepository(application)
    private val progressStore = ProgressStore(application)
    private val engine = LiveTranslatorEngine()

    private val _state = MutableStateFlow(LearnUiState())
    val state: StateFlow<LearnUiState> = _state.asStateFlow()

    private val translationCache = mutableMapOf<Pair<String, String>, Lesson>()
    private var translationJob: Job? = null

    init {
        UserSettings.init(application)
        viewModelScope.launch {
            val subjects = withContext(Dispatchers.IO) { repository.loadSubjects() }
            val ids = subjects.flatMap { s -> s.lessons.map { it.id } }
            _state.update { it.copy(subjects = subjects, progress = progressStore.all(ids)) }
        }
    }

    fun subject(id: String): Subject? = _state.value.subjects.firstOrNull { it.id == id }

    fun lesson(subjectId: String, lessonId: String): Lesson? =
        subject(subjectId)?.lessons?.firstOrNull { it.id == lessonId }

    fun setLevelFilter(level: Level?) = _state.update { it.copy(levelFilter = level) }

    fun openLesson(subjectId: String, lessonId: String) {
        _state.update { it.copy(screen = LearnScreenState.LessonView(subjectId, lessonId)) }
        translateIfNeeded(subjectId, lessonId)
    }

    fun startQuiz(subjectId: String, lessonId: String) {
        _state.update { it.copy(screen = LearnScreenState.Quiz(subjectId, lessonId)) }
    }

    fun toggleOriginal() = _state.update {
        it.copy(translation = it.translation.copy(showOriginal = !it.translation.showOriginal))
    }

    fun selectOption(option: Int) {
        val quiz = _state.value.screen as? LearnScreenState.Quiz ?: return
        if (quiz.selected != null || quiz.finished) return
        val question = lesson(quiz.subjectId, quiz.lessonId)?.quiz?.getOrNull(quiz.index) ?: return
        val score = quiz.score + if (option == question.answer) 1 else 0
        _state.update { it.copy(screen = quiz.copy(selected = option, score = score)) }
    }

    fun nextQuestion() {
        val quiz = _state.value.screen as? LearnScreenState.Quiz ?: return
        val total = lesson(quiz.subjectId, quiz.lessonId)?.quiz?.size ?: return
        if (quiz.index + 1 < total) {
            _state.update { it.copy(screen = quiz.copy(index = quiz.index + 1, selected = null)) }
        } else {
            progressStore.saveScore(quiz.lessonId, quiz.score)
            _state.update {
                it.copy(
                    screen = quiz.copy(finished = true),
                    progress = it.progress + (quiz.lessonId to maxOf(quiz.score, it.progress[quiz.lessonId] ?: 0)),
                )
            }
        }
    }

    fun restartQuiz() {
        val quiz = _state.value.screen as? LearnScreenState.Quiz ?: return
        startQuiz(quiz.subjectId, quiz.lessonId)
    }

    /** Back navigation inside the Learn tab. Returns false when already at the top level. */
    fun back(): Boolean {
        when (val screen = _state.value.screen) {
            is LearnScreenState.Quiz -> _state.update {
                it.copy(screen = LearnScreenState.LessonView(screen.subjectId, screen.lessonId))
            }
            is LearnScreenState.LessonView -> {
                translationJob?.cancel()
                _state.update { it.copy(screen = LearnScreenState.Subjects, translation = LessonTranslation()) }
            }
            LearnScreenState.Subjects -> return false
        }
        return true
    }

    /**
     * Lessons are written in French. When the user's native language is another one, the lesson
     * and its quiz are translated on-device, except for language subjects (French, English) whose
     * examples and answers must stay as written.
     */
    private fun translateIfNeeded(subjectId: String, lessonId: String) {
        translationJob?.cancel()
        val subject = subject(subjectId) ?: return
        val lesson = lesson(subjectId, lessonId) ?: return
        val native = UserSettings.nativeLanguage.value
        if (!subject.translatable || native == null || native == SOURCE_LANGUAGE) {
            _state.update { it.copy(translation = LessonTranslation()) }
            return
        }
        translationCache[lessonId to native]?.let { cached ->
            _state.update { it.copy(translation = LessonTranslation(lesson = cached)) }
            return
        }
        _state.update { it.copy(translation = LessonTranslation(translating = true)) }
        translationJob = viewModelScope.launch {
            try {
                val translated = translateLesson(lesson, native)
                translationCache[lessonId to native] = translated
                _state.update { it.copy(translation = LessonTranslation(lesson = translated)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(translation = LessonTranslation(failed = true)) }
            }
        }
    }

    private suspend fun translateLesson(lesson: Lesson, target: String): Lesson {
        suspend fun tr(text: String): String = translateMultiline(text, target)
        return lesson.copy(
            title = tr(lesson.title),
            summary = tr(lesson.summary),
            sections = lesson.sections.map { LessonSection(tr(it.title), tr(it.body)) },
            quiz = lesson.quiz.map { q ->
                q.copy(
                    question = tr(q.question),
                    options = q.options.map { if (TranslationRules.isFormula(it)) it else tr(it) },
                    explanation = tr(q.explanation),
                )
            },
        )
    }

    /** Translates line by line so that bullet lists and paragraphs keep their layout. */
    private suspend fun translateMultiline(text: String, target: String): String =
        text.lines().map { line ->
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> line
                trimmed.startsWith("• ") -> "• " + engine.translate(trimmed.removePrefix("• "), SOURCE_LANGUAGE, target)
                else -> engine.translate(trimmed, SOURCE_LANGUAGE, target)
            }
        }.joinToString("\n")

    override fun onCleared() {
        engine.close()
    }

    companion object {
        private const val SOURCE_LANGUAGE = TranslateLanguage.FRENCH
    }
}
