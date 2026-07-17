package com.rabu.hyphen.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rabu.hyphen.manager.TimerStateManager
import com.rabu.hyphen.service.CountdownService

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val timerStateManager = TimerStateManager(context.applicationContext)
        if (timerStateManager.resumeAfterBoot()) {
            CountdownService.start(context.applicationContext)
        }
    }
}
