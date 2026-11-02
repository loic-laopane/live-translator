package com.livetranslator.app.learn

/** Levels of the Common European Framework of Reference for Languages (CEFR). */
enum class Level(val id: String, val label: String) {
    A1("a1", "A1 · Débutant"),
    A2("a2", "A2 · Élémentaire"),
    B1("b1", "B1 · Intermédiaire"),
    B2("b2", "B2 · Avancé");

    companion object {
        fun fromId(id: String): Level =
            entries.firstOrNull { it.id == id } ?: throw IllegalArgumentException("Niveau inconnu : $id")
    }
}

/** A language course (English, German, Chinese…). */
data class Course(
    val id: String,
    /** Name shown to the user, e.g. "Allemand". */
    val name: String,
    /** ML Kit / BCP-47 code of the language taught, used for text-to-speech. */
    val language: String,
    val lessons: List<Lesson>,
)

data class Lesson(
    val id: String,
    val title: String,
    val level: Level,
    val summary: String,
    val sections: List<LessonSection>,
    val exercises: List<Exercise>,
)

/**
 * [title] and [body] explain the rule (written in French, translated into the user's native
 * language). [examples] are in the language taught and are never translated.
 */
data class LessonSection(val title: String, val body: String, val examples: List<Example>)

data class Example(
    /** Sentence in the language taught. */
    val text: String,
    /** Optional pronunciation help (e.g. pinyin for Chinese). */
    val pronunciation: String?,
    /** Meaning, in French (translated into the user's native language). */
    val translation: String,
)

sealed interface Exercise {
    /** Instruction, in French (translated into the user's native language). */
    val prompt: String
    /** Sentence or word in the language taught (never translated); may be empty. */
    val text: String
    val pronunciation: String?
    /** Why the answer is right, shown after answering. */
    val explanation: String

    /** Multiple choice. */
    data class Choice(
        override val prompt: String,
        override val text: String,
        override val pronunciation: String?,
        override val explanation: String,
        val options: List<String>,
        val answer: Int,
        /** True when the options are French meanings (to translate), false when they are in the language taught. */
        val translateOptions: Boolean,
    ) : Exercise

    /** The learner types the answer. */
    data class Input(
        override val prompt: String,
        override val text: String,
        override val pronunciation: String?,
        override val explanation: String,
        /** Accepted answers; the first one is shown as the solution. */
        val answers: List<String>,
    ) : Exercise
}

/** Checks typed answers: case, extra spaces and final punctuation are ignored, accents are not. */
object AnswerChecker {
    private val TRAILING_PUNCTUATION = Regex("[\\s.!?。！？,，]+$")
    private val SPACES = Regex("\\s+")

    fun normalize(answer: String): String =
        answer.trim().replace(TRAILING_PUNCTUATION, "").replace(SPACES, " ").lowercase()

    fun isCorrect(typed: String, accepted: List<String>): Boolean {
        val t = normalize(typed)
        return t.isNotEmpty() && accepted.any { normalize(it) == t }
    }
}
