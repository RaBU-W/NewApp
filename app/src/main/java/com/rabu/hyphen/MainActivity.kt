package com.rabu.hyphen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rabu.hyphen.manager.DeviceOwnerManager
import com.rabu.hyphen.ui.screen.OwnershipTransferScreen
import com.rabu.hyphen.ui.screen.TimerGateScreen
import com.rabu.hyphen.ui.theme.HyphenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HyphenTheme {
                TimerGateScreen { startCountdown ->
                    OwnershipTransferScreen(
                        onStartCountdown = startCountdown,
                        showDnsBlockedNotice = intent?.action == DeviceOwnerManager.PRIVATE_DNS_SETTINGS_ACTION,
                    )
                }
            }
        }
    }
}
