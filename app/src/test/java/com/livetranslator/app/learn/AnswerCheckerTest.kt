package com.livetranslator.app.learn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerCheckerTest {

    @Test
    fun ignoresCaseSpacesAndFinalPunctuation() {
        assertTrue(AnswerChecker.isCorrect("  ich sehe   den Hund. ", listOf("Ich sehe den Hund")))
        assertTrue(AnswerChecker.isCorrect("Somos españoles!", listOf("Somos españoles")))
        assertTrue(AnswerChecker.isCorrect("你好。", listOf("你好")))
    }

    @Test
    fun keepsAccentsAndWords() {
        assertFalse(AnswerChecker.isCorrect("Ich sehe der Hund", listOf("Ich sehe den Hund")))
        assertFalse(AnswerChecker.isCorrect("ecrites", listOf("écrites")))
        assertFalse(AnswerChecker.isCorrect("   ", listOf("den")))
    }

    @Test
    fun acceptsAlternatives() {
        assertTrue(AnswerChecker.isCorrect("We're happy", listOf("We are happy", "We're happy")))
    }
}
