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
import androidx.core.content.ContextCompat
import com.rabu.hyphen.manager.DeviceOwnerManager

class PrivateDnsEnforcerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var manager: DeviceOwnerManager

    private val ticker = object : Runnable {
        override fun run() {
            if (!manager.isPrivateDnsEnforcementEnabled()) {
                stopSelf()
                return
            }
            Thread { manager.enforceRequiredPrivateDns() }.start()
            handler.postDelayed(this, ENFORCE_INTERVAL_MILLIS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        manager = DeviceOwnerManager(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!manager.isPrivateDnsEnforcementEnabled()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        handler.removeCallbacks(ticker)
        ticker.run()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Private DNS enforcement running")
            .setContentText("DNS will be reset to ${DeviceOwnerManager.REQUIRED_PRIVATE_DNS_HOST} every 5 seconds.")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Private DNS enforcement",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "private_dns_enforcer"
        private const val NOTIFICATION_ID = 1002
        private const val ENFORCE_INTERVAL_MILLIS = 5_000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, PrivateDnsEnforcerService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PrivateDnsEnforcerService::class.java))
        }
    }
}
