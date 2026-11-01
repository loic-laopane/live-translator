package com.livetranslator.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.livetranslator.app.ui.LiveTranslatorTheme
import com.livetranslator.app.ui.LiveTranslatorApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UserSettings.init(this)
        enableEdgeToEdge()
        setContent {
            LiveTranslatorTheme {
                LiveTranslatorApp()
            }
        }
    }
}
