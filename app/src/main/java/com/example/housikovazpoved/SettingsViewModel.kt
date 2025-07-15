package com.example.housikovazpoved

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Definice datového úložiště pro naši aplikaci
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Datová třída pro stav nastavení
data class SettingsState(
    val hapticsEnabled: Boolean = true
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = getApplication<Application>().dataStore
    private val HAPTICS_ENABLED_KEY = booleanPreferencesKey("haptics_enabled")

    // StateFlow, které sleduje aktuální nastavení
    val uiState: StateFlow<SettingsState> = dataStore.data
        .map { preferences ->
            SettingsState(
                hapticsEnabled = preferences[HAPTICS_ENABLED_KEY] ?: true
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsState()
        )

    // Funkce pro přepnutí haptiky
    fun toggleHaptics(isEnabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[HAPTICS_ENABLED_KEY] = isEnabled
            }
        }
    }
}