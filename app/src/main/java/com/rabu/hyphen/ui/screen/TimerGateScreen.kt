package com.rabu.hyphen.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rabu.hyphen.manager.TimerGateManager
import kotlinx.coroutines.delay

@Composable
fun TimerGateScreen(contentAfterTimer: @Composable () -> Unit) {
    val context = LocalContext.current
    val timerGateManager = remember(context) { TimerGateManager(context.applicationContext) }
    var hasCompletedTimer by remember { mutableStateOf(timerGateManager.hasCompletedTimer()) }
    var remainingSeconds by remember { mutableLongStateOf(timerGateManager.remainingSeconds()) }

    LaunchedEffect(hasCompletedTimer) {
        while (!hasCompletedTimer) {
            remainingSeconds = timerGateManager.remainingSeconds()
            hasCompletedTimer = timerGateManager.hasCompletedTimer()
            delay(1_000L)
        }
    }

    if (hasCompletedTimer) {
        contentAfterTimer()
    } else {
        TimerScreen(
            remainingSeconds = remainingSeconds,
            isTimerRunning = timerGateManager.timerEndTimeMillis() > System.currentTimeMillis(),
            onStartTimer = {
                timerGateManager.startSixtySecondTimer()
                remainingSeconds = timerGateManager.remainingSeconds()
                hasCompletedTimer = false
            },
        )
    }
}

@Composable
private fun TimerScreen(
    remainingSeconds: Long,
    isTimerRunning: Boolean,
    onStartTimer: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "60 Second Timer",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isTimerRunning) {
                    "$remainingSeconds seconds remaining"
                } else {
                    "Transfer ownership screen open karne ke liye pehle 60 second ka timer lagao."
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTimerRunning,
                onClick = onStartTimer,
            ) {
                Text("Start 60 second timer")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Timer complete hone tak ownership transfer wala UI locked rahega.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
