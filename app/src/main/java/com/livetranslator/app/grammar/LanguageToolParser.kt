package com.livetranslator.app.grammar

import org.json.JSONObject

/** Parses the JSON answer of the LanguageTool `/v2/check` endpoint. */
object LanguageToolParser {

    fun parse(json: String, text: String): GrammarResult {
        val root = JSONObject(json)
        val language = root.optJSONObject("language")
        val detected = language?.optJSONObject("detectedLanguage")
        val languageCode = language?.optString("code")?.takeIf { it.isNotBlank() }
            ?: detected?.optString("code")?.takeIf { it.isNotBlank() }
        val languageName = language?.optString("name")?.takeIf { it.isNotBlank() }

        val matches = root.optJSONArray("matches")
        val issues = buildList {
            if (matches != null) {
                for (i in 0 until matches.length()) {
                    val m = matches.getJSONObject(i)
                    val offset = m.optInt("offset", -1)
                    val length = m.optInt("length", 0)
                    if (offset < 0 || offset + length > text.length) continue

                    val replacementsJson = m.optJSONArray("replacements")
                    val replacements = buildList {
                        if (replacementsJson != null) {
                            for (r in 0 until minOf(replacementsJson.length(), MAX_REPLACEMENTS)) {
                                val value = replacementsJson.getJSONObject(r).optString("value")
                                if (value.isNotEmpty() || length > 0) add(value)
                            }
                        }
                    }

                    val rule = m.optJSONObject("rule")
                    val category = rule?.optJSONObject("category")
                    val urls = rule?.optJSONArray("urls")
                    val ruleUrl = urls?.optJSONObject(0)?.optString("value")?.takeIf { it.isNotBlank() }

                    add(
                        GrammarIssue(
                            offset = offset,
                            length = length,
                            original = text.substring(offset, offset + length),
                            replacements = replacements,
                            message = cleanMessage(m.optString("message")),
                            shortMessage = cleanMessage(m.optString("shortMessage")),
                            ruleId = rule?.optString("id").orEmpty(),
                            ruleDescription = rule?.optString("description").orEmpty(),
                            category = category?.optString("name").orEmpty(),
                            issueType = rule?.optString("issueType").orEmpty(),
                            ruleUrl = ruleUrl,
                        )
                    )
                }
            }
        }
        return GrammarResult(text, languageCode, languageName, issues)
    }

    /** LanguageTool marks suggestions inside messages with `<suggestion>` tags: turn them into quotes. */
    internal fun cleanMessage(message: String): String =
        message.replace("<suggestion>", "« ").replace("</suggestion>", " »").trim()

    private const val MAX_REPLACEMENTS = 5
}
