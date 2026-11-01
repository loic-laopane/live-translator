package com.livetranslator.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.livetranslator.app.Languages
import com.livetranslator.app.UserSettings

private enum class Tab(val label: String) { TRANSLATE("Traduire"), LEARN("Apprendre") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTranslatorApp() {
    val nativeLanguage by UserSettings.nativeLanguage.collectAsState()
    var tab by rememberSaveable { mutableStateOf(Tab.TRANSLATE) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Translator") },
                actions = {
                    nativeLanguage?.let { code ->
                        IconButton(onClick = { showLanguageDialog = true }) {
                            Icon(
                                Icons.Default.RecordVoiceOver,
                                contentDescription = "Ma langue maternelle : ${Languages.displayName(code)}",
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (nativeLanguage != null) {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = {
                                Icon(
                                    if (t == Tab.TRANSLATE) Icons.Default.Translate else Icons.Default.School,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when {
            nativeLanguage == null -> NativeLanguageScreen(
                onChoose = UserSettings::setNativeLanguage,
                modifier = contentModifier,
            )
            tab == Tab.TRANSLATE -> TranslatorScreen(nativeLanguage, contentModifier)
            else -> LearnScreen(contentModifier)
        }
    }

    if (showLanguageDialog) {
        NativeLanguageDialog(
            current = nativeLanguage,
            onChoose = {
                UserSettings.setNativeLanguage(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false },
        )
    }
}
