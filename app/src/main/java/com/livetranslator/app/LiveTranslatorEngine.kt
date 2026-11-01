package com.livetranslator.app

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import java.io.Closeable

/**
 * Wraps ML Kit on-device language identification and translation.
 *
 * The source language is never provided by the user: it is detected from the text itself.
 */
class LiveTranslatorEngine : Closeable {

    private val languageIdentifier = LanguageIdentification.getClient(
        LanguageIdentificationOptions.Builder()
            .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
            .build()
    )

    /** One translator per (source, target) pair, reused across keystrokes. */
    private val translators = mutableMapOf<Pair<String, String>, Translator>()

    /** Pairs whose models are known to be present on the device. */
    private val readyPairs = mutableSetOf<Pair<String, String>>()

    /**
     * Returns the ML Kit translate language code detected for [text],
     * or null if the language could not be determined or is not supported for translation.
     */
    suspend fun detectLanguage(text: String): String? {
        val tag = languageIdentifier.identifyLanguage(text).await()
        if (tag == UNDETERMINED_TAG) return null
        return TranslateLanguage.fromLanguageTag(tag)
    }

    fun isModelReady(source: String, target: String): Boolean = (source to target) in readyPairs

    /**
     * Translates [text] from [source] to [target], downloading the required models first if needed.
     */
    suspend fun translate(text: String, source: String, target: String): String {
        if (source == target) return text
        val key = source to target
        val translator = synchronized(translators) {
            translators.getOrPut(key) {
                Translation.getClient(
                    TranslatorOptions.Builder()
                        .setSourceLanguage(source)
                        .setTargetLanguage(target)
                        .build()
                )
            }
        }
        if (key !in readyPairs) {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            readyPairs += key
        }
        return translator.translate(text).await()
    }

    override fun close() {
        languageIdentifier.close()
        synchronized(translators) {
            translators.values.forEach { it.close() }
            translators.clear()
        }
    }

    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.4f
        private const val UNDETERMINED_TAG = "und"
    }
}
