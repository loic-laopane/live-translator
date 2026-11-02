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
    data object Courses : LearnScreenState
    data class LessonView(val courseId: String, val lessonId: String) : LearnScreenState
    data class Exercises(
        val courseId: String,
        val lessonId: String,
        val index: Int = 0,
        /** Option chosen for a multiple-choice exercise. */
        val selected: Int? = null,
        /** Answer typed for an input exercise. */
        val typed: String? = null,
        /** Null while the current exercise is not answered yet. */
        val correct: Boolean? = null,
        val score: Int = 0,
        val finished: Boolean = false,
    ) : LearnScreenState
}

data class LessonTranslation(
    val translating: Boolean = false,
    /** Lesson with its explanations translated into the native language; null when not needed. */
    val lesson: Lesson? = null,
    val showOriginal: Boolean = false,
    val failed: Boolean = false,
)

data class LearnUiState(
    val courses: List<Course> = emptyList(),
    val levelFilter: Level? = null,
    /** Best exercise score per lesson id. */
    val progress: Map<String, Int> = emptyMap(),
    val screen: LearnScreenState = LearnScreenState.Courses,
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
            val courses = withContext(Dispatchers.IO) { repository.loadCourses() }
            val ids = courses.flatMap { c -> c.lessons.map { it.id } }
            _state.update { it.copy(courses = courses, progress = progressStore.all(ids)) }
        }
    }

    fun course(id: String): Course? = _state.value.courses.firstOrNull { it.id == id }

    fun lesson(courseId: String, lessonId: String): Lesson? =
        course(courseId)?.lessons?.firstOrNull { it.id == lessonId }

    fun setLevelFilter(level: Level?) = _state.update { it.copy(levelFilter = level) }

    fun openLesson(courseId: String, lessonId: String) {
        _state.update { it.copy(screen = LearnScreenState.LessonView(courseId, lessonId)) }
        translateIfNeeded(courseId, lessonId)
    }

    fun startExercises(courseId: String, lessonId: String) {
        _state.update { it.copy(screen = LearnScreenState.Exercises(courseId, lessonId)) }
    }

    fun toggleOriginal() = _state.update {
        it.copy(translation = it.translation.copy(showOriginal = !it.translation.showOriginal))
    }

    fun selectOption(option: Int) {
        val ex = _state.value.screen as? LearnScreenState.Exercises ?: return
        if (ex.correct != null || ex.finished) return
        val exercise = currentExercise(ex) as? Exercise.Choice ?: return
        val correct = option == exercise.answer
        _state.update { it.copy(screen = ex.copy(selected = option, correct = correct, score = ex.score + if (correct) 1 else 0)) }
    }

    fun submitAnswer(typed: String) {
        val ex = _state.value.screen as? LearnScreenState.Exercises ?: return
        if (ex.correct != null || ex.finished || typed.isBlank()) return
        val exercise = currentExercise(ex) as? Exercise.Input ?: return
        val correct = AnswerChecker.isCorrect(typed, exercise.answers)
        _state.update { it.copy(screen = ex.copy(typed = typed, correct = correct, score = ex.score + if (correct) 1 else 0)) }
    }

    fun next() {
        val ex = _state.value.screen as? LearnScreenState.Exercises ?: return
        val total = lesson(ex.courseId, ex.lessonId)?.exercises?.size ?: return
        if (ex.index + 1 < total) {
            _state.update { it.copy(screen = ex.copy(index = ex.index + 1, selected = null, typed = null, correct = null)) }
        } else {
            progressStore.saveScore(ex.lessonId, ex.score)
            _state.update {
                it.copy(
                    screen = ex.copy(finished = true),
                    progress = it.progress + (ex.lessonId to maxOf(ex.score, it.progress[ex.lessonId] ?: 0)),
                )
            }
        }
    }

    fun restartExercises() {
        val ex = _state.value.screen as? LearnScreenState.Exercises ?: return
        startExercises(ex.courseId, ex.lessonId)
    }

    /** Back navigation inside the Learn tab. Returns false when already at the top level. */
    fun back(): Boolean {
        when (val screen = _state.value.screen) {
            is LearnScreenState.Exercises -> _state.update {
                it.copy(screen = LearnScreenState.LessonView(screen.courseId, screen.lessonId))
            }
            is LearnScreenState.LessonView -> {
                translationJob?.cancel()
                _state.update { it.copy(screen = LearnScreenState.Courses, translation = LessonTranslation()) }
            }
            LearnScreenState.Courses -> return false
        }
        return true
    }

    private fun currentExercise(ex: LearnScreenState.Exercises): Exercise? =
        lesson(ex.courseId, ex.lessonId)?.exercises?.getOrNull(ex.index)

    /**
     * Explanations are written in French. When the user's native language is another one, they are
     * translated on-device. Text in the language being learned (examples, sentences to complete,
     * answer options) is never translated.
     */
    private fun translateIfNeeded(courseId: String, lessonId: String) {
        translationJob?.cancel()
        val lesson = lesson(courseId, lessonId) ?: return
        val native = UserSettings.nativeLanguage.value
        if (native == null || native == SOURCE_LANGUAGE) {
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
            sections = lesson.sections.map { s ->
                s.copy(
                    title = tr(s.title),
                    body = tr(s.body),
                    examples = s.examples.map { it.copy(translation = tr(it.translation)) },
                )
            },
            exercises = lesson.exercises.map { e ->
                when (e) {
                    is Exercise.Choice -> e.copy(
                        prompt = tr(e.prompt),
                        explanation = tr(e.explanation),
                        options = if (e.translateOptions) e.options.map { tr(it) } else e.options,
                    )
                    is Exercise.Input -> e.copy(prompt = tr(e.prompt), explanation = tr(e.explanation))
                }
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
