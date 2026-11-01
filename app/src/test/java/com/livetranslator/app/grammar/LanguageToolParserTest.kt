package com.livetranslator.app.grammar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageToolParserTest {

    private val text = "Je suis allé a la plage."

    private val json = """
        {
          "software": {"name": "LanguageTool", "version": "6.6"},
          "language": {
            "name": "French", "code": "fr",
            "detectedLanguage": {"name": "French", "code": "fr", "confidence": 0.99}
          },
          "matches": [
            {
              "message": "Confusion probable entre « a » (verbe avoir) et <suggestion>à</suggestion> (préposition).",
              "shortMessage": "Confusion a/à",
              "replacements": [{"value": "à"}],
              "offset": 13,
              "length": 1,
              "context": {"text": "Je suis allé a la plage.", "offset": 13, "length": 1},
              "sentence": "Je suis allé a la plage.",
              "rule": {
                "id": "A_A_ACCENT",
                "description": "Confusion a/à",
                "issueType": "grammar",
                "urls": [{"value": "https://example.org/a-ou-a"}],
                "category": {"id": "GRAMMAR", "name": "Grammaire"}
              }
            },
            {
              "message": "Hors limites",
              "replacements": [],
              "offset": 100,
              "length": 3,
              "rule": {"id": "X", "description": "x", "category": {"id": "X", "name": "X"}}
            }
          ]
        }
    """.trimIndent()

    @Test
    fun parse_readsLanguageAndIssues() {
        val result = LanguageToolParser.parse(json, text)
        assertEquals("fr", result.languageCode)
        assertEquals("French", result.languageName)
        assertEquals(1, result.issues.size)

        val issue = result.issues.single()
        assertEquals(13, issue.offset)
        assertEquals("a", issue.original)
        assertEquals(listOf("à"), issue.replacements)
        assertEquals("Confusion a/à", issue.ruleDescription)
        assertEquals("Grammaire", issue.category)
        assertEquals("grammar", issue.issueType)
        assertEquals("https://example.org/a-ou-a", issue.ruleUrl)
        assertTrue(issue.message.contains("« à »"))
        assertTrue(!issue.message.contains("<suggestion>"))
    }

    @Test
    fun parse_handlesNoMatches() {
        val result = LanguageToolParser.parse("""{"language":{"code":"en-US","name":"English (US)"},"matches":[]}""", "Hello")
        assertEquals("en-US", result.languageCode)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun parse_missingUrlIsNull() {
        val json = """{"matches":[{"message":"m","offset":0,"length":2,"replacements":[{"value":"Ja"}],"rule":{"id":"R"}}]}"""
        val issue = LanguageToolParser.parse(json, "Je").issues.single()
        assertNull(issue.ruleUrl)
        assertEquals("", issue.category)
    }
}
