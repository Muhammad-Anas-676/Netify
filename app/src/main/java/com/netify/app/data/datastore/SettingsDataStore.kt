package com.netify.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "netify_settings")

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/**
 * Persists user-facing preferences. Theme defaults to LIGHT the very first
 * time the app runs (no saved value yet); once the user picks Dark or
 * System, that choice is written here and restored on every future launch.
 */
class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val UNITS_MBPS = booleanPreferencesKey("units_mbps") // true = Mbps, false = MBps
        val LANGUAGE = stringPreferencesKey("language")
        val ISP_PLAN_MBPS = intPreferencesKey("isp_plan_mbps")
        val ALERT_THRESHOLD_PCT = intPreferencesKey("alert_threshold_pct")
        val AUTO_TEST_ENABLED = booleanPreferencesKey("auto_test_enabled")
        val AUTO_TEST_HOUR = intPreferencesKey("auto_test_hour")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.THEME_MODE]) {
            "DARK" -> ThemeMode.DARK
            "SYSTEM" -> ThemeMode.SYSTEM
            else -> ThemeMode.LIGHT // default on first-ever launch
        }
    }
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    val unitsMbps: Flow<Boolean> = context.dataStore.data.map { it[Keys.UNITS_MBPS] ?: true }
    suspend fun setUnitsMbps(mbps: Boolean) { context.dataStore.edit { it[Keys.UNITS_MBPS] = mbps } }

    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }
    suspend fun setLanguage(code: String) { context.dataStore.edit { it[Keys.LANGUAGE] = code } }

    val ispPlanMbps: Flow<Int> = context.dataStore.data.map { it[Keys.ISP_PLAN_MBPS] ?: 50 }
    suspend fun setIspPlanMbps(mbps: Int) { context.dataStore.edit { it[Keys.ISP_PLAN_MBPS] = mbps } }

    val alertThresholdPct: Flow<Int> = context.dataStore.data.map { it[Keys.ALERT_THRESHOLD_PCT] ?: 60 }
    suspend fun setAlertThresholdPct(pct: Int) { context.dataStore.edit { it[Keys.ALERT_THRESHOLD_PCT] = pct } }

    val autoTestEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_TEST_ENABLED] ?: false }
    suspend fun setAutoTestEnabled(enabled: Boolean) { context.dataStore.edit { it[Keys.AUTO_TEST_ENABLED] = enabled } }

    val autoTestHour: Flow<Int> = context.dataStore.data.map { it[Keys.AUTO_TEST_HOUR] ?: 9 }
    suspend fun setAutoTestHour(hour: Int) { context.dataStore.edit { it[Keys.AUTO_TEST_HOUR] = hour } }
}
