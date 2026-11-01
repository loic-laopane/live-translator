package com.livetranslator.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livetranslator.app.Languages
import com.livetranslator.app.Status
import com.livetranslator.app.TranslatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(vm: TranslatorViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Translator") },
                navigationIcon = {
                    Icon(
                        Icons.Default.Translate,
                        contentDescription = null,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TargetLanguagePicker(
                selected = state.targetLanguage,
                onSelect = vm::onTargetChange,
            )

            GrammarToggle(
                enabled = state.grammar.enabled,
                onChange = vm::onGrammarEnabledChange,
            )

            val grammarUpToDate = state.grammar.checkedText == state.input
            val errorColor = MaterialTheme.colorScheme.error

            OutlinedTextField(
                value = state.input,
                onValueChange = vm::onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                label = { Text("Écrivez dans n'importe quelle langue") },
                visualTransformation = if (grammarUpToDate && state.grammar.issues.isNotEmpty()) {
                    ErrorHighlightTransformation(state.grammar.issues, errorColor)
                } else {
                    VisualTransformation.None
                },
                supportingText = { DetectedLanguageLabel(state.detectedLanguage, state.status) },
                trailingIcon = {
                    if (state.input.isNotEmpty()) {
                        IconButton(onClick = vm::clear) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                },
            )

            GrammarSection(
                grammar = state.grammar,
                upToDate = grammarUpToDate,
                onApplyAll = vm::applyAllCorrections,
                onApply = vm::applyCorrection,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            Languages.displayName(state.targetLanguage),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (state.status is Status.Translating || state.status is Status.DownloadingModel) {
                            Spacer(Modifier.width(8.dp))
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = { copyToClipboard(context, state.translation) },
                            enabled = state.translation.isNotEmpty(),
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copier")
                        }
                    }
                    SelectionContainer {
                        Text(
                            text = state.translation.ifEmpty { "La traduction apparaîtra ici…" },
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (state.translation.isEmpty()) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.heightIn(min = 120.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetectedLanguageLabel(detected: String?, status: Status) {
    val text = when (status) {
        is Status.DownloadingModel ->
            "Téléchargement du modèle ${Languages.displayName(status.source)} → " +
                "${Languages.displayName(status.target)}… (une seule fois)"
        is Status.LanguageNotDetected -> "Langue non détectée, continuez à écrire…"
        is Status.Error -> "Erreur : ${status.message}"
        else -> detected?.let { "Langue détectée : ${Languages.displayName(it)}" } ?: ""
    }
    Text(
        text,
        color = if (status is Status.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun GrammarToggle(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Correction et règles de grammaire", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Vérifié par LanguageTool (le texte est envoyé à leur service)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetLanguagePicker(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = Languages.displayName(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text("Traduire vers") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val native = Languages.nativeLanguage()
            Languages.all.forEach { lang ->
                val isNative = lang.code == native
                DropdownMenuItem(
                    text = {
                        Text(
                            if (isNative) "${lang.displayName} (langue du téléphone)" else lang.displayName,
                            fontWeight = if (isNative) FontWeight.SemiBold else null,
                        )
                    },
                    onClick = {
                        onSelect(lang.code)
                        expanded = false
                    },
                )
                if (isNative) HorizontalDivider()
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("translation", text))
    Toast.makeText(context, "Copié", Toast.LENGTH_SHORT).show()
}
