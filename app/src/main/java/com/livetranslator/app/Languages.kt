package com.livetranslator.app

import com.google.mlkit.nl.translate.TranslateLanguage
import java.text.Collator
import java.util.Locale

data class Language(val code: String, val displayName: String)

object Languages {
    /**
     * All languages supported by ML Kit translation, named in the device language:
     * the user's native (device) language first, then the others in alphabetical order.
     */
    val all: List<Language> by lazy {
        ordered(
            TranslateLanguage.getAllLanguages().map { Language(it, displayName(it)) },
            nativeLanguage(),
            Locale.getDefault(),
        )
    }

    /** ML Kit code of the user's native language (the device language), if it is supported. */
    fun nativeLanguage(): String? =
        TranslateLanguage.fromLanguageTag(Locale.getDefault().toLanguageTag().substringBefore('-'))

    /**
     * Puts [nativeCode] first, then sorts the rest alphabetically with a locale-aware collator
     * (accents and case ignored, so "Égyptien" sorts with the E's, not after "Z").
     */
    fun ordered(languages: List<Language>, nativeCode: String?, locale: Locale): List<Language> {
        val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
        val (native, others) = languages.partition { it.code == nativeCode }
        return native + others.sortedWith { a, b -> collator.compare(a.displayName, b.displayName) }
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
