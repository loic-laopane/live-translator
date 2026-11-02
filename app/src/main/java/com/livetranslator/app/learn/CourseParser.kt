package com.livetranslator.app.learn

import org.json.JSONArray
import org.json.JSONObject

/** Parses a course file from `assets/learn/<course>.json`. */
object CourseParser {

    fun parseCourse(json: String): Course {
        val root = JSONObject(json)
        return Course(
            id = root.getString("id"),
            name = root.getString("name"),
            language = root.getString("language"),
            lessons = root.getJSONArray("lessons").objects().map(::parseLesson),
        )
    }

    private fun parseLesson(o: JSONObject) = Lesson(
        id = o.getString("id"),
        title = o.getString("title"),
        level = Level.fromId(o.getString("level")),
        summary = o.optString("summary"),
        sections = o.getJSONArray("sections").objects().map { s ->
            LessonSection(
                title = s.getString("title"),
                body = s.optString("body"),
                examples = (s.optJSONArray("examples") ?: JSONArray()).objects().map { e ->
                    Example(
                        text = e.getString("text"),
                        pronunciation = e.optStringOrNull("pronunciation"),
                        translation = e.optString("translation"),
                    )
                },
            )
        },
        exercises = o.getJSONArray("exercises").objects().map(::parseExercise),
    )

    private fun parseExercise(e: JSONObject): Exercise {
        val prompt = e.getString("prompt")
        val text = e.optString("text")
        val pronunciation = e.optStringOrNull("pronunciation")
        val explanation = e.optString("explanation")
        return when (val type = e.getString("type")) {
            "choice" -> Exercise.Choice(
                prompt, text, pronunciation, explanation,
                options = e.getJSONArray("options").strings(),
                answer = e.getInt("answer"),
                translateOptions = e.optBoolean("translateOptions", false),
            )
            "input" -> Exercise.Input(
                prompt, text, pronunciation, explanation,
                answers = e.getJSONArray("answers").strings(),
            )
            else -> throw IllegalArgumentException("Type d'exercice inconnu : $type")
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key).takeIf { it.isNotBlank() } else null

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
}
