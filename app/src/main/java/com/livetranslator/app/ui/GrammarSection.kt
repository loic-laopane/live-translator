package com.livetranslator.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.livetranslator.app.GrammarUiState
import com.livetranslator.app.grammar.GrammarIssue

/**
 * Corrections block: corrected version of the text, then one card per mistake with the
 * suggestions and the grammar rule / pitfall that explains it.
 */
@Composable
fun GrammarSection(
    grammar: GrammarUiState,
    upToDate: Boolean,
    onApplyAll: () -> Unit,
    onApply: (GrammarIssue, String) -> Unit,
) {
    if (!grammar.enabled) return

    when {
        grammar.error != null -> Text(
            grammar.error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )

        !upToDate || grammar.checking -> if (grammar.checkedText.isNotBlank() || grammar.checking) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Vérification de l'orthographe et de la grammaire…", style = MaterialTheme.typography.bodySmall)
            }
        }

        grammar.checkedText.isBlank() -> Unit

        grammar.issues.isEmpty() -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Aucune faute détectée", style = MaterialTheme.typography.bodyMedium)
        }

        else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            grammar.corrected?.let { corrected ->
                CorrectedVersionCard(corrected.text, corrected.changedRanges, onApplyAll)
            }
            Text(
                if (grammar.issues.size == 1) "1 faute à corriger" else "${grammar.issues.size} fautes à corriger",
                style = MaterialTheme.typography.titleSmall,
            )
            grammar.issues.forEach { issue ->
                IssueCard(issue, onApply)
            }
        }
    }
}

@Composable
private fun CorrectedVersionCard(text: String, changed: List<IntRange>, onApplyAll: () -> Unit) {
    val highlight = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        textDecoration = TextDecoration.Underline,
    )
    val annotated = buildAnnotatedString {
        append(text)
        changed.forEach { addStyle(highlight, it.first, it.last + 1) }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Spellcheck, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Version corrigée", style = MaterialTheme.typography.labelLarge)
            }
            Text(annotated, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onApplyAll) { Text("Remplacer mon texte") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IssueCard(issue: GrammarIssue, onApply: (GrammarIssue, String) -> Unit) {
    var expanded by rememberSaveable(issue.ruleId, issue.offset) { mutableStateOf(true) }
    val uriHandler = LocalUriHandler.current

    OutlinedCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        issueTypeLabel(issue),
                        style = MaterialTheme.typography.labelSmall,
                        color = issueColor(issue),
                    )
                    Text(
                        buildAnnotatedString {
                            pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = MaterialTheme.colorScheme.error))
                            append(issue.original.ifBlank { "␣" })
                            pop()
                            issue.replacements.firstOrNull()?.let {
                                append("  →  ")
                                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                append(it.ifEmpty { "(supprimer)" })
                                pop()
                            }
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Masquer la règle" else "Voir la règle",
                )
            }

            if (issue.replacements.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    issue.replacements.take(3).forEach { replacement ->
                        AssistChip(
                            onClick = { onApply(issue, replacement) },
                            label = { Text(replacement.ifEmpty { "(supprimer)" }) },
                        )
                    }
                }
            }

            if (expanded) {
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        issue.ruleDescription.ifBlank { issue.shortMessage.ifBlank { "Règle" } },
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Text(issue.translatedMessage ?: issue.message, style = MaterialTheme.typography.bodyMedium)
                if (issue.translatedMessage != null) {
                    Text(
                        issue.message,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (issue.category.isNotBlank()) {
                    Text(
                        "Catégorie : ${issue.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                issue.ruleUrl?.let { url ->
                    TextButton(onClick = { runCatching { uriHandler.openUri(url) } }) {
                        Text("En savoir plus sur cette règle")
                    }
                }
            }
        }
    }
}

private fun issueTypeLabel(issue: GrammarIssue): String = when (issue.issueType) {
    "misspelling" -> "ORTHOGRAPHE"
    "grammar" -> "GRAMMAIRE"
    "typographical" -> "TYPOGRAPHIE"
    "style", "locale-violation", "register" -> "STYLE"
    "duplication" -> "RÉPÉTITION"
    else -> issue.category.uppercase().ifBlank { "À VÉRIFIER" }
}

@Composable
private fun issueColor(issue: GrammarIssue): Color = when (issue.issueType) {
    "misspelling", "grammar" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.tertiary
}

/** Underlines the mistakes directly in the input field (identity offset mapping: text is unchanged). */
class ErrorHighlightTransformation(
    private val issues: List<GrammarIssue>,
    private val color: Color,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val style = SpanStyle(textDecoration = TextDecoration.Underline, color = color)
        val annotated = buildAnnotatedString {
            append(text)
            issues.forEach { issue ->
                val start = issue.offset.coerceIn(0, text.length)
                val end = issue.end.coerceIn(start, text.length)
                if (end > start) addStyle(style, start, end)
            }
        }
        return TransformedText(annotated, OffsetMapping.Identity)
    }

    override fun equals(other: Any?): Boolean =
        other is ErrorHighlightTransformation && other.issues == issues && other.color == color

    override fun hashCode(): Int = issues.hashCode() * 31 + color.hashCode()
}
