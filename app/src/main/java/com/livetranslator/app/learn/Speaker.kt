package com.livetranslator.app.learn

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Reads examples aloud with the device text-to-speech engine. */
class Speaker(context: Context) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var pending: Pair<String, String>? = null

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        pending?.let { (text, lang) -> speak(text, lang) }
        pending = null
    }

    /**
     * Speaks [text] in [languageTag]. Returns false when no voice is available for that language
     * (the user can install one in the system text-to-speech settings).
     */
    fun speak(text: String, languageTag: String): Boolean {
        if (!ready) {
            pending = text to languageTag
            return true
        }
        val result = tts.setLanguage(Locale.forLanguageTag(languageTag))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) return false
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "example")
        return true
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
