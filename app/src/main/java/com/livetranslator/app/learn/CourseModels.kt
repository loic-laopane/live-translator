package com.livetranslator.app.learn

/** School levels covered by the lessons (French lycée). */
enum class Level(val id: String, val label: String) {
    SECONDE("seconde", "Seconde"),
    PREMIERE("premiere", "Première"),
    TERMINALE("terminale", "Terminale");

    companion object {
        fun fromId(id: String): Level =
            entries.firstOrNull { it.id == id } ?: throw IllegalArgumentException("Niveau inconnu : $id")
    }
}

data class Subject(
    val id: String,
    val name: String,
    /**
     * False for language subjects (French, English): their examples and quiz answers are about
     * the language itself, so they must never be machine-translated.
     */
    val translatable: Boolean,
    val lessons: List<Lesson>,
)

data class Lesson(
    val id: String,
    val title: String,
    val level: Level,
    val summary: String,
    val sections: List<LessonSection>,
    val quiz: List<QuizQuestion>,
)

data class LessonSection(val title: String, val body: String)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    /** Index of the correct option in [options]. */
    val answer: Int,
    /** Why the answer is right: shown after answering. */
    val explanation: String,
)
