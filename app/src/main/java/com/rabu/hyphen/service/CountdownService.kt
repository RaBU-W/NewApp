package com.rabu.hyphen.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rabu.hyphen.manager.TimerStateManager

class CountdownService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerStateManager: TimerStateManager

    private val ticker = object : Runnable {
        override fun run() {
            val remainingSeconds = timerStateManager.saveRunningCountdown()
            if (remainingSeconds <= 0L) {
                timerStateManager.completeTimer()
                stopSelf()
                return
            }
            NotificationManagerCompat.from(this@CountdownService)
                .notify(NOTIFICATION_ID, createNotification(remainingSeconds))
            handler.postDelayed(this, SAVE_INTERVAL_MILLIS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        timerStateManager = TimerStateManager(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val durationSeconds = intent?.getLongExtra(EXTRA_DURATION_SECONDS, 0L) ?: 0L
        if (durationSeconds > 0L) {
            timerStateManager.startTimer(durationSeconds)
        }

        val remainingSeconds = timerStateManager.saveRunningCountdown()
        if (remainingSeconds <= 0L) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification(remainingSeconds))
        handler.removeCallbacks(ticker)
        handler.postDelayed(ticker, SAVE_INTERVAL_MILLIS)
        return START_STICKY
    }

    override fun onDestroy() {
        timerStateManager.saveRunningCountdown()
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(remainingSeconds: Long): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Countdown timer running")
            .setContentText(formatRemainingTime(remainingSeconds))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Countdown timer",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_DURATION_SECONDS = "duration_seconds"
        private const val CHANNEL_ID = "countdown_timer"
        private const val NOTIFICATION_ID = 1001
        private const val SAVE_INTERVAL_MILLIS = 5_000L

        fun start(context: Context, durationSeconds: Long? = null) {
            val intent = Intent(context, CountdownService::class.java)
            durationSeconds?.let { intent.putExtra(EXTRA_DURATION_SECONDS, it) }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
    }
}

fun formatRemainingTime(totalSeconds: Long): String {
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
