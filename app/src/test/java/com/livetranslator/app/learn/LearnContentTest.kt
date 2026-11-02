package com.livetranslator.app.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Validates every language course shipped in `src/main/assets/learn`. */
class LearnContentTest {

    private val courses: List<Course> by lazy {
        val dir = File("src/main/assets/learn")
        assertTrue("Dossier introuvable : ${dir.absolutePath}", dir.isDirectory)
        dir.listFiles { f -> f.extension == "json" }!!.sortedBy { it.name }
            .map { CourseParser.parseCourse(it.readText()) }
    }

    @Test
    fun everyCourseParses() {
        val ids = courses.map { it.id }.toSet()
        assertTrue(ids.containsAll(listOf("anglais", "allemand", "espagnol", "chinois")))
        courses.forEach { assertTrue("${it.id} sans leçon", it.lessons.isNotEmpty()) }
    }

    @Test
    fun onlyLanguageCourses() {
        val languages = courses.map { it.language }
        assertEquals(languages.size, languages.toSet().size)
        assertFalse(courses.any { it.id in listOf("maths", "physique-chimie", "svt", "histoire-geo", "philosophie") })
    }

    @Test
    fun idsAreUnique() {
        val lessonIds = courses.flatMap { c -> c.lessons.map { it.id } }
        assertEquals(lessonIds.size, lessonIds.toSet().size)
    }

    @Test
    fun lessonsAreComplete() {
        courses.flatMap { it.lessons }.forEach { lesson ->
            assertTrue("${lesson.id} : titre vide", lesson.title.isNotBlank())
            assertTrue("${lesson.id} : pas de section", lesson.sections.isNotEmpty())
            lesson.sections.forEach {
                assertTrue("${lesson.id} : section vide", it.body.isNotBlank() || it.examples.isNotEmpty())
            }
            assertTrue("${lesson.id} : pas d'exemple", lesson.sections.any { it.examples.isNotEmpty() })
            assertTrue("${lesson.id} : trop peu d'exercices", lesson.exercises.size >= 4)
        }
    }

    @Test
    fun exercisesAreValid() {
        courses.flatMap { it.lessons }.forEach { lesson ->
            lesson.exercises.forEach { e ->
                val where = "${lesson.id} / ${e.prompt} ${e.text}"
                assertTrue("$where : consigne vide", e.prompt.isNotBlank())
                assertTrue("$where : explication manquante", e.explanation.isNotBlank())
                when (e) {
                    is Exercise.Choice -> {
                        assertTrue("$where : moins de 2 choix", e.options.size >= 2)
                        assertTrue("$where : réponse hors limites", e.answer in e.options.indices)
                        assertEquals("$where : choix en double", e.options.size, e.options.toSet().size)
                    }
                    is Exercise.Input -> {
                        assertTrue("$where : aucune réponse acceptée", e.answers.isNotEmpty())
                        // The expected answer must be accepted by the checker itself.
                        assertTrue("$where : réponse refusée", AnswerChecker.isCorrect(e.answers.first(), e.answers))
                    }
                }
            }
        }
    }
}
