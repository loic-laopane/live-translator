package com.livetranslator.app.learn

import org.json.JSONArray
import org.json.JSONObject

/** Parses a subject file from `assets/learn/<subject>.json`. */
object CourseParser {

    fun parseSubject(json: String): Subject {
        val root = JSONObject(json)
        return Subject(
            id = root.getString("id"),
            name = root.getString("name"),
            translatable = root.optBoolean("translatable", true),
            lessons = root.getJSONArray("lessons").objects().map(::parseLesson),
        )
    }

    private fun parseLesson(o: JSONObject) = Lesson(
        id = o.getString("id"),
        title = o.getString("title"),
        level = Level.fromId(o.getString("level")),
        summary = o.optString("summary"),
        sections = o.getJSONArray("sections").objects().map {
            LessonSection(it.getString("title"), it.getString("body"))
        },
        quiz = o.getJSONArray("quiz").objects().map { q ->
            QuizQuestion(
                question = q.getString("question"),
                options = q.getJSONArray("options").strings(),
                answer = q.getInt("answer"),
                explanation = q.optString("explanation"),
            )
        },
    )

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
}
