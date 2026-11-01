package com.livetranslator.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.livetranslator.app.ui.LiveTranslatorTheme
import com.livetranslator.app.ui.TranslatorScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiveTranslatorTheme {
                TranslatorScreen()
            }
        }
    }
}
