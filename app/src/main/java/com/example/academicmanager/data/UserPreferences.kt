package com.example.academicmanager.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val NAME_KEY = stringPreferencesKey("user_name")
        val DEPARTMENT_KEY = stringPreferencesKey("user_department")
        val POSITION_KEY = stringPreferencesKey("user_position")
    }

    val userName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[NAME_KEY]
    }

    val userDepartment: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[DEPARTMENT_KEY]
    }

    val userPosition: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[POSITION_KEY]
    }

    suspend fun saveUser(name: String, department: String, position: String) {
        context.dataStore.edit { preferences ->
            preferences[NAME_KEY] = name
            preferences[DEPARTMENT_KEY] = department
            preferences[POSITION_KEY] = position
        }
    }

    val isUserSaved: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NAME_KEY] != null && preferences[DEPARTMENT_KEY] != null && preferences[POSITION_KEY] != null
    }
}
