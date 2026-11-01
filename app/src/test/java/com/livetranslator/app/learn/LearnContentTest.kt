package com.livetranslator.app.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Validates every lesson shipped in `src/main/assets/learn`. */
class LearnContentTest {

    private val subjects: List<Subject> by lazy {
        val dir = File("src/main/assets/learn")
        assertTrue("Dossier introuvable : ${dir.absolutePath}", dir.isDirectory)
        dir.listFiles { f -> f.extension == "json" }!!.sortedBy { it.name }
            .map { CourseParser.parseSubject(it.readText()) }
    }

    @Test
    fun everySubjectFileParses() {
        assertTrue(subjects.size >= 7)
        subjects.forEach { assertTrue("${it.id} sans leçon", it.lessons.isNotEmpty()) }
    }

    @Test
    fun idsAreUnique() {
        val subjectIds = subjects.map { it.id }
        assertEquals(subjectIds.size, subjectIds.toSet().size)
        val lessonIds = subjects.flatMap { s -> s.lessons.map { it.id } }
        assertEquals(lessonIds.size, lessonIds.toSet().size)
    }

    @Test
    fun everyLevelIsCovered() {
        val levels = subjects.flatMap { s -> s.lessons.map { it.level } }.toSet()
        assertEquals(Level.entries.toSet(), levels)
    }

    @Test
    fun lessonsAreComplete() {
        subjects.flatMap { it.lessons }.forEach { lesson ->
            assertTrue("${lesson.id} : titre vide", lesson.title.isNotBlank())
            assertTrue("${lesson.id} : pas de section", lesson.sections.isNotEmpty())
            lesson.sections.forEach { assertTrue("${lesson.id} : section vide", it.body.isNotBlank()) }
            assertTrue("${lesson.id} : quiz trop court", lesson.quiz.size >= 3)
        }
    }

    @Test
    fun quizQuestionsAreValid() {
        subjects.flatMap { it.lessons }.forEach { lesson ->
            lesson.quiz.forEach { q ->
                val where = "${lesson.id} / ${q.question}"
                assertTrue("$where : moins de 2 choix", q.options.size >= 2)
                assertTrue("$where : réponse hors limites", q.answer in q.options.indices)
                assertEquals("$where : choix en double", q.options.size, q.options.toSet().size)
                assertTrue("$where : explication manquante", q.explanation.isNotBlank())
            }
        }
    }

    @Test
    fun languageSubjectsAreNeverTranslated() {
        subjects.filter { it.id == "francais" || it.id == "anglais" }.forEach {
            assertFalse("${it.id} ne doit pas être traduit", it.translatable)
        }
    }

    @Test
    fun formulasAreDetected() {
        assertTrue(TranslationRules.isFormula("2x + 3"))
        assertTrue(TranslationRules.isFormula("e²"))
        assertTrue(TranslationRules.isFormula("44 g/mol"))
        assertFalse(TranslationRules.isFormula("La mitochondrie"))
        assertFalse(TranslationRules.isFormula("décroissante"))
    }
}
