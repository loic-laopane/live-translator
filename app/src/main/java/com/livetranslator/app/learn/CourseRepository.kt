package com.livetranslator.app.learn

import android.content.Context

/** Loads every subject shipped in `assets/learn/`. */
class CourseRepository(private val context: Context) {

    fun loadSubjects(): List<Subject> {
        val assets = context.assets
        return (assets.list(DIR) ?: emptyArray())
            .filter { it.endsWith(".json") }
            .sorted()
            .map { file -> assets.open("$DIR/$file").bufferedReader().use { CourseParser.parseSubject(it.readText()) } }
            .sortedBy { SUBJECT_ORDER.indexOf(it.id).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }
    }

    companion object {
        private const val DIR = "learn"
        private val SUBJECT_ORDER = listOf(
            "francais", "maths", "anglais", "physique-chimie", "svt", "histoire-geo", "philosophie",
        )
    }
}

/** Best quiz score per lesson, kept on the device. */
class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    fun bestScore(lessonId: String): Int? =
        prefs.getInt("best_$lessonId", -1).takeIf { it >= 0 }

    fun all(lessonIds: List<String>): Map<String, Int> =
        lessonIds.mapNotNull { id -> bestScore(id)?.let { id to it } }.toMap()

    fun saveScore(lessonId: String, score: Int) {
        if (score > (bestScore(lessonId) ?: -1)) prefs.edit().putInt("best_$lessonId", score).apply()
    }
}
