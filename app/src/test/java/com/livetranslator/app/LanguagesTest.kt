package com.livetranslator.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LanguagesTest {

    private val languages = listOf(
        Language("en", "Anglais"),
        Language("ar", "Arabe"),
        Language("fr", "Français"),
        Language("et", "Estonien"),
        Language("es", "espagnol"),
        Language("el", "Grec"),
        Language("ga", "Écossais"),
        Language("zu", "Zoulou"),
    )

    @Test
    fun nativeLanguageFirstThenAlphabetical() {
        val result = Languages.ordered(languages, "fr", Locale.FRENCH).map { it.code }
        assertEquals(listOf("fr", "en", "ar", "ga", "es", "et", "el", "zu"), result)
    }

    @Test
    fun unknownNativeLanguageKeepsPlainAlphabeticalOrder() {
        val result = Languages.ordered(languages, null, Locale.FRENCH).map { it.code }
        assertEquals(listOf("en", "ar", "ga", "es", "et", "fr", "el", "zu"), result)
    }
}
