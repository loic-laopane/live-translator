package com.livetranslator.app

import com.google.mlkit.nl.translate.TranslateLanguage
import java.text.Collator
import java.util.Locale

data class Language(val code: String, val displayName: String)

object Languages {
    private val allUnordered: List<Language> by lazy {
        TranslateLanguage.getAllLanguages().map { Language(it, displayName(it)) }
    }

    /**
     * All languages supported by ML Kit translation, named in the device language:
     * [nativeCode] (the user's native language) first, then the others in alphabetical order.
     */
    fun all(nativeCode: String?): List<Language> = ordered(allUnordered, nativeCode, Locale.getDefault())

    /** ML Kit code of the device language, if it is supported. Used as default native language. */
    fun deviceLanguage(): String? =
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

    /** Default target: English, unless the user's language is already English, then French. */
    fun defaultTarget(nativeCode: String? = deviceLanguage()): String =
        if (nativeCode == TranslateLanguage.ENGLISH) TranslateLanguage.FRENCH else TranslateLanguage.ENGLISH
}
