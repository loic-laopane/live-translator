package com.livetranslator.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livetranslator.app.Languages
import java.util.Locale

/** First-launch screen: asks the user's native language. */
@Composable
fun NativeLanguageScreen(onChoose: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Bienvenue !", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Quelle est votre langue maternelle ?",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Choose your native language · Elige tu lengua materna",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Elle sert à vous expliquer les règles de grammaire et à traduire les cours dans votre langue. " +
                "Vous pourrez la changer à tout moment.",
            style = MaterialTheme.typography.bodyMedium,
        )
        LanguageList(
            selected = null,
            suggested = Languages.deviceLanguage(),
            onChoose = onChoose,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Dialog used from the settings to change the native language. */
@Composable
fun NativeLanguageDialog(current: String?, onChoose: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ma langue maternelle") },
        text = {
            LanguageList(
                selected = current,
                suggested = current ?: Languages.deviceLanguage(),
                onChoose = onChoose,
                modifier = Modifier.heightIn(max = 420.dp),
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}

/** Searchable language list, [suggested] first then alphabetical order. */
@Composable
private fun LanguageList(
    selected: String?,
    suggested: String?,
    onChoose: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val languages = remember(suggested) { Languages.all(suggested) }
    val filtered = remember(languages, query) {
        val q = query.trim().lowercase(Locale.getDefault())
        if (q.isEmpty()) languages else languages.filter { it.displayName.lowercase(Locale.getDefault()).contains(q) }
    }
    Column(modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Rechercher une langue") },
        )
        Spacer(Modifier.size(8.dp))
        LazyColumn {
            items(filtered, key = { it.code }) { lang ->
                val isSuggested = lang.code == suggested && query.isBlank()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChoose(lang.code) }
                        .padding(vertical = 14.dp, horizontal = 4.dp),
                ) {
                    Text(
                        lang.displayName,
                        modifier = Modifier.weight(1f),
                        fontWeight = if (isSuggested || lang.code == selected) FontWeight.SemiBold else null,
                    )
                    if (lang.code == selected) {
                        Icon(Icons.Default.Check, contentDescription = "Sélectionnée", tint = MaterialTheme.colorScheme.primary)
                    } else if (isSuggested) {
                        Text("Suggérée", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                if (isSuggested) HorizontalDivider()
            }
        }
    }
}
