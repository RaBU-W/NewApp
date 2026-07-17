package com.rabu.hyphen.data

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map



val Context.dataStore by preferencesDataStore(name = "timer_data")

val TIMER_SECONDS = intPreferencesKey("timer_seconds")


suspend fun Context.saveTimerSeconds(seconds: Int) {
    dataStore.edit { prefs ->
        prefs[TIMER_SECONDS] = seconds
    }
}

val Context.timerSeconds: Flow<Int>
    get() = dataStore.data.map { prefs ->
        prefs[TIMER_SECONDS] ?: 0
    }