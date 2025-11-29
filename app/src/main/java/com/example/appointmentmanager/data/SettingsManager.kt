package com.example.appointmentmanager.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map



class SettingsManager(private val context: Context){

    companion object {
        // Create DataStore instance
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "appointment_settings"
        )

        // Define keys
        private val KEY_DEFAULT_CAPACITY = intPreferencesKey("default_slot_capacity")

        private const val DEFAULT_VALUE = 2
        private const val MIN_CAPACITY = 1
        private const val MAX_CAPACITY = 20
    }


    //Get default capacity as Flow (reactive)
    val defaultCapacityFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_CAPACITY] ?: DEFAULT_VALUE
    }

    // Get default capacity immediately (for non-reactive use)
    suspend fun getDefaultCapacity(): Int {
        return defaultCapacityFlow.first()
    }

    //set default capacity
    suspend fun setDefaultCapacity(capacity: Int): Boolean {
        if (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY) {
            return false
        }

        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_DEFAULT_CAPACITY] = capacity
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }


    suspend fun resetDefaultCapacity() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_DEFAULT_CAPACITY)
        }
    }


}