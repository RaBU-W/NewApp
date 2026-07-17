package com.rabu.hyphen

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Notification channel banana
        createNotificationChannel(this)

        // TimerService start karna
        val intent = Intent(this, TimerService::class.java)

        ContextCompat.startForegroundService(this, intent)

        setContent {
            Navigation()
        }
    }
}


fun createNotificationChannel(context: Context) {

    val channel = NotificationChannel(
        "timer",
        "Timer",
        NotificationManager.IMPORTANCE_LOW
    )

    val manager = context.getSystemService(
        NotificationManager::class.java
    )

    manager.createNotificationChannel(channel)
}