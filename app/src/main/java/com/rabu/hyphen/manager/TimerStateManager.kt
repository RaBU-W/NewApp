package com.rabu.hyphen.manager

import android.content.Context
import kotlin.math.ceil

class TimerStateManager(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun startTimer(durationSeconds: Long, nowMillis: Long = System.currentTimeMillis()) {
        val safeDurationSeconds = durationSeconds.coerceIn(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS)
        preferences.edit()
            .putBoolean(KEY_IS_RUNNING, true)
            .putLong(KEY_DURATION_SECONDS, safeDurationSeconds)
            .putLong(KEY_REMAINING_SECONDS, safeDurationSeconds)
            .putLong(KEY_END_TIME_MILLIS, nowMillis + safeDurationSeconds * MILLIS_PER_SECOND)
            .apply()
    }

    fun saveRunningCountdown(nowMillis: Long = System.currentTimeMillis()): Long {
        if (!isTimerRunning()) return 0L
        val remainingSeconds = remainingSeconds(nowMillis)
        preferences.edit()
            .putLong(KEY_REMAINING_SECONDS, remainingSeconds)
            .apply()
        if (remainingSeconds <= 0L) completeTimer()
        return remainingSeconds
    }

    fun resumeAfterBoot(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!isTimerRunning()) return false
        val remainingSeconds = savedRemainingSeconds()
        if (remainingSeconds <= 0L) {
            completeTimer()
            return false
        }
        preferences.edit()
            .putLong(KEY_END_TIME_MILLIS, nowMillis + remainingSeconds * MILLIS_PER_SECOND)
            .apply()
        return true
    }

    fun completeTimer() {
        preferences.edit()
            .putBoolean(KEY_IS_RUNNING, false)
            .putLong(KEY_REMAINING_SECONDS, 0L)
            .putLong(KEY_END_TIME_MILLIS, 0L)
            .apply()
    }

    fun isTimerRunning(nowMillis: Long = System.currentTimeMillis()): Boolean =
        preferences.getBoolean(KEY_IS_RUNNING, false) && remainingSeconds(nowMillis) > 0L

    fun remainingSeconds(nowMillis: Long = System.currentTimeMillis()): Long {
        val endTimeMillis = preferences.getLong(KEY_END_TIME_MILLIS, 0L)
        if (endTimeMillis <= 0L) return savedRemainingSeconds()
        val remainingMillis = (endTimeMillis - nowMillis).coerceAtLeast(0L)
        return ceil(remainingMillis / MILLIS_PER_SECOND.toDouble()).toLong()
    }

    private fun savedRemainingSeconds(): Long = preferences.getLong(KEY_REMAINING_SECONDS, 0L)

    companion object {
        const val MIN_DURATION_SECONDS = 1L
        const val MAX_DURATION_SECONDS = 60L * 60L
        private const val MILLIS_PER_SECOND = 1_000L
        private const val PREFERENCES_NAME = "countdown_timer"
        private const val KEY_IS_RUNNING = "is_running"
        private const val KEY_DURATION_SECONDS = "duration_seconds"
        private const val KEY_REMAINING_SECONDS = "remaining_seconds"
        private const val KEY_END_TIME_MILLIS = "end_time_millis"
    }
}
