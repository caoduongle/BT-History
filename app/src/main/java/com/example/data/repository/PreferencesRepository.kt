package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bt_watcher_settings")

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val KEY_DISCONNECT_ALERT = booleanPreferencesKey("disconnect_alert")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_LAST_KNOWN_LOCATION_NAME = stringPreferencesKey("last_known_location_name")
    }

    val isServiceEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.KEY_SERVICE_ENABLED] ?: true
    }

    val isDisconnectAlertEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.KEY_DISCONNECT_ALERT] ?: true
    }

    val isOnboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.KEY_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_SERVICE_ENABLED] = enabled
        }
    }

    suspend fun setDisconnectAlertEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_DISCONNECT_ALERT] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_ONBOARDING_COMPLETED] = completed
        }
    }
}
