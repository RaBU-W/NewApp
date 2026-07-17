package com.rabu.hyphen.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.rabu.hyphen.manager.TimerStateManager
import com.rabu.hyphen.service.CountdownService
import com.rabu.hyphen.service.formatRemainingTime
import kotlinx.coroutines.delay

@Composable
fun TimerGateScreen(contentWhenTimerIdle: @Composable (startCountdown: (Long) -> Unit) -> Unit) {
    val context = LocalContext.current
    val timerStateManager = remember(context) { TimerStateManager(context.applicationContext) }
    var isTimerRunning by remember { mutableStateOf(timerStateManager.isTimerRunning()) }
    var remainingSeconds by remember { mutableLongStateOf(timerStateManager.remainingSeconds()) }

    fun refreshTimerState() {
        isTimerRunning = timerStateManager.isTimerRunning()
        remainingSeconds = timerStateManager.remainingSeconds()
    }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning) {
            timerStateManager.saveRunningCountdown()
            refreshTimerState()
            delay(1_000L)
        }
    }

    if (isTimerRunning) {
        RunningTimerScreen(remainingSeconds = remainingSeconds)
    } else {
        contentWhenTimerIdle { durationSeconds ->
            timerStateManager.startTimer(durationSeconds)
            CountdownService.start(context.applicationContext)
            refreshTimerState()
        }
    }
}

@Composable
private fun RunningTimerScreen(remainingSeconds: Long) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Timer chal raha hai",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = formatRemainingTime(remainingSeconds),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Timer complete hone tak ownership transfer UI locked rahega. App band hone par bhi foreground service countdown save karti rahegi.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
