package com.munchkin.tracker.presentation.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "munchkin_settings")

data class SettingsState(
    val alwaysListenEnabled: Boolean = false,
    val ttsEnabled: Boolean = true,
    val hotword: String = "Эй Манчкин"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        val KEY_ALWAYS_LISTEN = booleanPreferencesKey("always_listen")
        val KEY_TTS_ENABLED   = booleanPreferencesKey("tts_enabled")
        val KEY_HOTWORD       = stringPreferencesKey("hotword")
    }

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            context.settingsDataStore.data
                .catch { emit(emptyPreferences()) }
                .collect { prefs ->
                    _state.value = SettingsState(
                        alwaysListenEnabled = prefs[KEY_ALWAYS_LISTEN] ?: false,
                        ttsEnabled          = prefs[KEY_TTS_ENABLED] ?: true,
                        hotword             = prefs[KEY_HOTWORD] ?: "Эй Манчкин"
                    )
                }
        }
    }

    fun setAlwaysListen(enabled: Boolean) = save { it[KEY_ALWAYS_LISTEN] = enabled }
    fun setTtsEnabled(enabled: Boolean)   = save { it[KEY_TTS_ENABLED] = enabled }
    fun setHotword(word: String)          = save { it[KEY_HOTWORD] = word }

    private fun save(block: (MutablePreferences) -> Unit) {
        viewModelScope.launch {
            context.settingsDataStore.edit { block(it) }
        }
    }
}