package com.livetranslator.app.grammar

import org.junit.Assert.assertEquals
import org.junit.Test

class CorrectionsTest {

    private fun issue(offset: Int, length: Int, vararg replacements: String) = GrammarIssue(
        offset = offset,
        length = length,
        original = "",
        replacements = replacements.toList(),
        message = "",
        shortMessage = "",
        ruleId = "",
        ruleDescription = "",
        category = "",
        issueType = "",
        ruleUrl = null,
    )

    private val text = "Je suis allé a la plage, il fesait beau."
    private val aIssue = issue(13, 1, "à")
    private val fesaitIssue = issue(28, 6, "faisait", "fêtait")

    @Test
    fun correct_appliesFirstSuggestionOfEveryIssue() {
        val result = Corrections.correct(text, listOf(fesaitIssue, aIssue))
        assertEquals("Je suis allé à la plage, il faisait beau.", result.text)
        assertEquals(listOf(13..13, 28..34), result.changedRanges)
    }

    @Test
    fun correct_skipsIssuesWithoutSuggestionAndOverlaps() {
        val noSuggestion = issue(0, 2)
        val overlapping = issue(13, 3, "XXX")
        val result = Corrections.correct(text, listOf(noSuggestion, aIssue, overlapping))
        assertEquals("Je suis allé à la plage, il fesait beau.", result.text)
    }

    @Test
    fun applyOne_replacesAndShiftsFollowingIssues() {
        val (newText, remaining) = Corrections.applyOne(text, listOf(aIssue, fesaitIssue), fesaitIssue, "faisait")
        assertEquals("Je suis allé a la plage, il faisait beau.", newText)
        assertEquals(listOf(aIssue), remaining)

        val (newText2, remaining2) = Corrections.applyOne(text, listOf(aIssue, fesaitIssue), aIssue, "à la")
        assertEquals("Je suis allé à la la plage, il fesait beau.", newText2)
        assertEquals(31, remaining2.single().offset)
        assertEquals("fesait", newText2.substring(remaining2.single().offset, remaining2.single().end))
    }
}
