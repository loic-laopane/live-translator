package com.livetranslator.app

import com.google.mlkit.nl.translate.TranslateLanguage
import java.util.Locale

data class Language(val code: String, val displayName: String)

object Languages {
    /** All languages supported by ML Kit translation, named in the device language, sorted. */
    val all: List<Language> by lazy {
        TranslateLanguage.getAllLanguages()
            .map { Language(it, displayName(it)) }
            .sortedBy { it.displayName.lowercase(Locale.getDefault()) }
    }

    fun displayName(code: String): String {
        val locale = Locale.forLanguageTag(code)
        val name = locale.getDisplayLanguage(Locale.getDefault())
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            .ifBlank { code }
    }

    /** Default target: English, unless the device is already in English, then French. */
    fun defaultTarget(): String {
        val device = Locale.getDefault().language
        return if (device == TranslateLanguage.ENGLISH) TranslateLanguage.FRENCH else TranslateLanguage.ENGLISH
    }
}
