package com.livesplits.data.settings

import android.content.Context
import android.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.livesplits.domain.model.ComparisonMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Settings data class representing all app preferences.
 */
data class AppSettings(
    // App behavior
    val launchGames: Boolean = false,
    
    // Timer colors (stored as Int color values)
    val colorAhead: Int = Color.GREEN,
    val colorBehind: Int = Color.RED,
    val colorPb: Int = Color.parseColor("#0088FF"),
    
    // Timing settings
    val comparison: ComparisonMode = ComparisonMode.PB,
    val countdownMs: Long = 0L,
    
    // Display
    val showSplitName: Boolean = true,
    val showDelta: Boolean = true,
    val showMilliseconds: Boolean = true,
    val timerSize: Int = 48, // sp
    val showBackground: Boolean = true
)

/**
 * Repository for managing app settings using DataStore.
 */
class SettingsRepository(private val context: Context) {
    
    private val DataStore<Preferences>.settingsFlow: Flow<AppSettings>
        get() = data.map { preferences ->
            preferences.toAppSettings()
        }

    val settings: Flow<AppSettings> = context.dataStore.settingsFlow

    suspend fun updateLaunchGames(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAUNCH_GAMES] = enabled
        }
    }

    suspend fun updateColorAhead(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COLOR_AHEAD] = color
        }
    }

    suspend fun updateColorBehind(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COLOR_BEHIND] = color
        }
    }

    suspend fun updateColorPb(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COLOR_PB] = color
        }
    }

    suspend fun updateComparison(mode: ComparisonMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COMPARISON] = mode.name
        }
    }

    suspend fun updateCountdownMs(ms: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COUNTDOWN_MS] = ms
        }
    }

    suspend fun updateShowSplitName(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_SPLIT_NAME] = show
        }
    }

    suspend fun updateShowDelta(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_DELTA] = show
        }
    }

    suspend fun updateShowMilliseconds(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_MILLISECONDS] = show
        }
    }

    suspend fun updateTimerSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TIMER_SIZE] = size
        }
    }

    suspend fun updateShowBackground(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_BACKGROUND] = show
        }
    }

    // Timer position per game
    suspend fun saveTimerPosition(gameId: Long, x: Float, y: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.timerPositionX(gameId)] = x
            preferences[PreferencesKeys.timerPositionY(gameId)] = y
        }
    }

    fun getTimerPositionFlow(gameId: Long): Flow<Pair<Float, Float>> {
        return context.dataStore.data.map { preferences ->
            val x = preferences[PreferencesKeys.timerPositionX(gameId)] ?: 100f
            val y = preferences[PreferencesKeys.timerPositionY(gameId)] ?: 100f
            x to y
        }
    }

    // Current running timer info
    suspend fun saveCurrentTimerInfo(
        categoryId: Long,
        segments: List<Long>, // Segment IDs in order
        startTimeMs: Long
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_CATEGORY_ID] = categoryId
            preferences[PreferencesKeys.CURRENT_SEGMENTS] = segments.joinToString(",")
            preferences[PreferencesKeys.CURRENT_START_TIME] = startTimeMs
        }
    }

    suspend fun clearCurrentTimerInfo() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.CURRENT_CATEGORY_ID)
            preferences.remove(PreferencesKeys.CURRENT_SEGMENTS)
            preferences.remove(PreferencesKeys.CURRENT_START_TIME)
        }
    }

    suspend fun getCurrentTimerInfo(): Triple<Long?, String?, Long?> {
        val prefs = context.dataStore.data.first()
        return Triple(
            prefs[PreferencesKeys.CURRENT_CATEGORY_ID],
            prefs[PreferencesKeys.CURRENT_SEGMENTS],
            prefs[PreferencesKeys.CURRENT_START_TIME]
        )
    }
}

private object PreferencesKeys {
    val LAUNCH_GAMES = booleanPreferencesKey("launch_games")
    val COLOR_AHEAD = intPreferencesKey("color_ahead")
    val COLOR_BEHIND = intPreferencesKey("color_behind")
    val COLOR_PB = intPreferencesKey("color_pb")
    val COMPARISON = stringPreferencesKey("comparison")
    val COUNTDOWN_MS = longPreferencesKey("countdown_ms")
    val SHOW_SPLIT_NAME = booleanPreferencesKey("show_split_name")
    val SHOW_DELTA = booleanPreferencesKey("show_delta")
    val SHOW_MILLISECONDS = booleanPreferencesKey("show_milliseconds")
    val TIMER_SIZE = intPreferencesKey("timer_size")
    val SHOW_BACKGROUND = booleanPreferencesKey("show_background")
    
    val CURRENT_CATEGORY_ID = longPreferencesKey("current_category_id")
    val CURRENT_SEGMENTS = stringPreferencesKey("current_segments")
    val CURRENT_START_TIME = longPreferencesKey("current_start_time")
    
    fun timerPositionX(gameId: Long) = floatPreferencesKey("timer_position_x_$gameId")
    fun timerPositionY(gameId: Long) = floatPreferencesKey("timer_position_y_$gameId")
}

private fun Preferences.toAppSettings(): AppSettings {
    return AppSettings(
        launchGames = this[PreferencesKeys.LAUNCH_GAMES] ?: false,
        colorAhead = this[PreferencesKeys.COLOR_AHEAD] ?: Color.GREEN,
        colorBehind = this[PreferencesKeys.COLOR_BEHIND] ?: Color.RED,
        colorPb = this[PreferencesKeys.COLOR_PB] ?: Color.parseColor("#0088FF"),
        comparison = try {
            ComparisonMode.valueOf(this[PreferencesKeys.COMPARISON] ?: ComparisonMode.PB.name)
        } catch (e: IllegalArgumentException) {
            ComparisonMode.PB
        },
        countdownMs = this[PreferencesKeys.COUNTDOWN_MS] ?: 0L,
        showSplitName = this[PreferencesKeys.SHOW_SPLIT_NAME] ?: true,
        showDelta = this[PreferencesKeys.SHOW_DELTA] ?: true,
        showMilliseconds = this[PreferencesKeys.SHOW_MILLISECONDS] ?: true,
        timerSize = this[PreferencesKeys.TIMER_SIZE] ?: 48,
        showBackground = this[PreferencesKeys.SHOW_BACKGROUND] ?: true
    )
}
