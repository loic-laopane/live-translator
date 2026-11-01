package com.livetranslator.app

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide user settings. The native language is asked on first launch: it is used to explain
 * grammar rules, to translate lessons and quizzes, and is listed first in language pickers.
 */
object UserSettings {
    private const val PREFS = "settings"
    private const val KEY_NATIVE = "native_language"

    private lateinit var prefs: SharedPreferences
    private val _nativeLanguage = MutableStateFlow<String?>(null)

    /** ML Kit code of the user's native language, or null until the user has chosen it. */
    val nativeLanguage: StateFlow<String?> = _nativeLanguage.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _nativeLanguage.value = prefs.getString(KEY_NATIVE, null)
    }

    fun setNativeLanguage(code: String) {
        prefs.edit().putString(KEY_NATIVE, code).apply()
        _nativeLanguage.value = code
    }
}
