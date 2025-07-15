package com.example.housikovazpoved

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.housikovazpoved.ui.theme.AppNavigation // Důležitý je správný import

class MainActivity : ComponentActivity() {

    // ViewModels zůstavají, starají se o logiku hry a nastavení
    private val gameViewModel: GameViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Načtení otázek při startu
        gameViewModel.loadQuestions(applicationContext)

        setContent {
            // Aplikujeme povrch, který vyplní celou obrazovku
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                // Zde voláme naši hlavní Composable funkci s navigací
                AppNavigation(
                    gameViewModel = gameViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}