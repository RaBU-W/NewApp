package com.rabu.hyphen.manager

import android.content.Context

class TimerGateManager(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun timerEndTimeMillis(): Long = preferences.getLong(KEY_TIMER_END_TIME, 0L)

    fun hasCompletedTimer(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val endTimeMillis = timerEndTimeMillis()
        return endTimeMillis > 0L && nowMillis >= endTimeMillis
    }

    fun remainingSeconds(nowMillis: Long = System.currentTimeMillis()): Long {
        val remainingMillis = timerEndTimeMillis() - nowMillis
        return (remainingMillis.coerceAtLeast(0L) + MILLIS_PER_SECOND - 1L) / MILLIS_PER_SECOND
    }

    fun startSixtySecondTimer(nowMillis: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putLong(KEY_TIMER_END_TIME, nowMillis + TIMER_DURATION_MILLIS)
            .apply()
    }

    companion object {
        const val TIMER_DURATION_SECONDS = 60L
        private const val MILLIS_PER_SECOND = 1_000L
        private const val TIMER_DURATION_MILLIS = TIMER_DURATION_SECONDS * MILLIS_PER_SECOND
        private const val PREFERENCES_NAME = "timer_gate"
        private const val KEY_TIMER_END_TIME = "timer_end_time"
    }
}
