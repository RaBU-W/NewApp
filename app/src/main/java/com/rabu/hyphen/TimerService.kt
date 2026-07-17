package com.rabu.hyphen

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel


class TimerService : Service() {

    private var remainingTime: Int = 50

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground(1, createNotification(remainingTime))

        // Timer /Countdown
        serviceScope.launch {
            while (remainingTime > 0) {
                NotificationManagerCompat.from(this@TimerService)
                        .notify(1, createNotification(remainingTime))
                delay(1000)
                remainingTime--
            }
            stopSelf()
        }
        // return
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotification(remainingTime: Int): Notification {
        val notification =
                NotificationCompat.Builder(this, "timer")
                        .setContentTitle("Timer Is Running")
                        .setContentText("$remainingTime Left")
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .build()

        return notification
    }
}
