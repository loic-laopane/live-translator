package com.livetranslator.app.grammar

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class RateLimitedException : IOException("Trop de vérifications en peu de temps")

/**
 * Spelling / grammar checker backed by LanguageTool (https://languagetool.org).
 *
 * The public API is free and needs no key (limits: ~20 requests and 75 KB per minute per IP).
 * `language=auto` lets LanguageTool detect the language of the text by itself.
 */
class LanguageToolClient(
    private val endpoint: String = DEFAULT_ENDPOINT,
) {

    /** @param motherTongue the user's own language (ISO code), enables "false friends" rules. */
    suspend fun check(text: String, motherTongue: String?): GrammarResult = withContext(Dispatchers.IO) {
        val params = buildMap {
            put("text", text)
            put("language", "auto")
            put("preferredVariants", PREFERRED_VARIANTS)
            if (!motherTongue.isNullOrBlank()) put("motherTongue", motherTongue)
        }
        val body = params.entries.joinToString("&") { (k, v) ->
            "$k=${URLEncoder.encode(v, "UTF-8")}"
        }

        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    LanguageToolParser.parse(json, text)
                }
                429 -> throw RateLimitedException()
                else -> throw IOException("LanguageTool a répondu $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://api.languagetool.org/v2/check"
        private const val PREFERRED_VARIANTS = "en-US,de-DE,pt-PT,ca-ES"
        private const val TIMEOUT_MS = 10_000
    }
}
