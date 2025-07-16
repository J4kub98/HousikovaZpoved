package com.example.housikovazpoved

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gameViewModel.loadQuestions(applicationContext)

        setContent {
            // Zde použijeme barvu pozadí z našeho nového AppleTheme
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = AppleTheme.Background
            ) {
                // Tady voláme naši hlavní Composable funkci, která řídí celou navigaci
                AppNavigation(
                    gameViewModel = gameViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}