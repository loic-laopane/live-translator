package com.livetranslator.app.grammar

/** One spelling / grammar problem found in the text, with the rule that explains it. */
data class GrammarIssue(
    /** Position of the faulty fragment in the checked text (UTF-16 chars, like Kotlin strings). */
    val offset: Int,
    val length: Int,
    /** The faulty fragment as written. */
    val original: String,
    /** Suggested replacements, best first. May be empty (problem detected, no automatic fix). */
    val replacements: List<String>,
    /** Explanation of the rule / pitfall, in the language of the text. */
    val message: String,
    val shortMessage: String,
    val ruleId: String,
    /** Name of the rule, e.g. "Confusion a/à". */
    val ruleDescription: String,
    /** Rule family, e.g. "Grammaire", "Fautes de frappe". */
    val category: String,
    /** "misspelling", "grammar", "typographical", "style"... */
    val issueType: String,
    /** Link to a page explaining the rule in depth, when available. */
    val ruleUrl: String?,
    /** [message] translated into the user's native language, when the text is in another language. */
    val translatedMessage: String? = null,
    val translatedRuleDescription: String? = null,
    val translatedCategory: String? = null,
) {
    val end: Int get() = offset + length

    fun overlaps(other: GrammarIssue): Boolean = offset < other.end && other.offset < end
}

data class GrammarResult(
    val text: String,
    /** Language code detected by LanguageTool, e.g. "fr", "en-US". */
    val languageCode: String?,
    val languageName: String?,
    val issues: List<GrammarIssue>,
)

/** Text with every first suggestion applied, and the ranges (in the new text) that were changed. */
data class CorrectedText(val text: String, val changedRanges: List<IntRange>)

object Corrections {

    /**
     * Applies the first replacement of every issue that has one. Overlapping issues are skipped
     * (the first one, by position, wins) so the result is always consistent.
     */
    fun correct(text: String, issues: List<GrammarIssue>): CorrectedText {
        val sb = StringBuilder()
        val ranges = mutableListOf<IntRange>()
        var cursor = 0
        for (issue in issues.filter { it.replacements.isNotEmpty() }.sortedBy { it.offset }) {
            if (issue.offset < cursor || issue.offset < 0 || issue.end > text.length) continue
            sb.append(text, cursor, issue.offset)
            val replacement = issue.replacements.first()
            val start = sb.length
            sb.append(replacement)
            if (replacement.isNotEmpty()) ranges += start until sb.length
            cursor = issue.end
        }
        sb.append(text, cursor, text.length)
        return CorrectedText(sb.toString(), ranges)
    }

    /**
     * Applies a single [replacement] for [issue] and returns the new text together with the
     * remaining issues, whose offsets are shifted so they still point at the right fragments.
     */
    fun applyOne(
        text: String,
        issues: List<GrammarIssue>,
        issue: GrammarIssue,
        replacement: String,
    ): Pair<String, List<GrammarIssue>> {
        if (issue.offset < 0 || issue.end > text.length) return text to issues
        val newText = text.replaceRange(issue.offset, issue.end, replacement)
        val delta = replacement.length - issue.length
        val remaining = issues
            .filter { it != issue && !it.overlaps(issue) }
            .map { if (it.offset >= issue.end) it.copy(offset = it.offset + delta) else it }
        return newText to remaining
    }
}
