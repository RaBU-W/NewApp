package com.rabu.hyphen.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rabu.hyphen.manager.DeviceOwnerManager
import com.rabu.hyphen.manager.TimerStateManager

@Composable
fun OwnershipTransferScreen(onStartCountdown: (Long) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val manager = remember(context) { DeviceOwnerManager(context.applicationContext) }
    var isDeviceOwner by remember { mutableStateOf(manager.isDeviceOwner()) }
    var statusMessage by remember { mutableStateOf(ownerStatusText(isDeviceOwner)) }
    var countdownSecondsText by remember { mutableStateOf("60") }
    var isPrivateDnsBlocked by remember { mutableStateOf(manager.isPrivateDnsConfigBlocked()) }
    val countdownSeconds = countdownSecondsText.toLongOrNull()
    val isCountdownValid = countdownSeconds != null && countdownSeconds in TimerStateManager.MIN_DURATION_SECONDS..TimerStateManager.MAX_DURATION_SECONDS

    fun refreshOwnerState() {
        isDeviceOwner = manager.isDeviceOwner()
        isPrivateDnsBlocked = manager.isPrivateDnsConfigBlocked()
        statusMessage = ownerStatusText(isDeviceOwner)
    }

    DisposableEffect(lifecycleOwner, manager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshOwnerState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Owndroid Ownership Transfer",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = isDeviceOwner,
                onClick = {
                    manager.transferOwnershipToOwndroid()
                    refreshOwnerState()
                },
            ) {
                Text("Transfer ownership to Owndroid")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Button tabhi enable hoga jab ye app Device Owner hoga. Tap karte hi ownership Owndroid receiver ko wapas transfer hogi.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "DNS configuration lock",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Switch(
                checked = isPrivateDnsBlocked,
                enabled = isDeviceOwner && manager.canBlockPrivateDnsConfig(),
                onCheckedChange = { blocked ->
                    manager.setPrivateDnsConfigBlocked(blocked)
                    isPrivateDnsBlocked = manager.isPrivateDnsConfigBlocked()
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = dnsLockDescription(
                    isDeviceOwner = isDeviceOwner,
                    canControlPrivateDns = manager.canBlockPrivateDnsConfig(),
                    isPrivateDnsBlocked = isPrivateDnsBlocked,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Custom countdown lock",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = countdownSecondsText,
                onValueChange = { value -> countdownSecondsText = value.filter(Char::isDigit) },
                label = { Text("Seconds: 1 to 3600") },
                singleLine = true,
                isError = countdownSecondsText.isNotBlank() && !isCountdownValid,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = isCountdownValid,
                onClick = { countdownSeconds?.let(onStartCountdown) },
            ) {
                Text("Start countdown")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Timer sirf aap start karoge tab chalega. Timer running hoga tab app open karne par sirf countdown screen dikhegi; timer khatam hote hi ye transfer screen wapas aa jayegi.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun ownerStatusText(isDeviceOwner: Boolean): String =
    if (isDeviceOwner) {
        "Device Owner permission mil gayi hai. Ab ownership Owndroid ko transfer kar sakte ho."
    } else {
        "Abhi ye app Device Owner nahi hai. Pehle Owndroid se ownership is app ko transfer karo."
    }


private fun dnsLockDescription(
    isDeviceOwner: Boolean,
    canControlPrivateDns: Boolean,
    isPrivateDnsBlocked: Boolean,
): String = when {
    !canControlPrivateDns -> "Ye DNS block feature sirf Android 16 users ke liye hai."
    !isDeviceOwner -> "Toggle tabhi enable hoga jab ye app Device Owner hoga."
    isPrivateDnsBlocked -> "DNS configuration blocked hai. Jo DNS/Private DNS abhi set hai wahi rahega; user Settings se usse change nahi kar payega jab tak toggle off na ho."
    else -> "Toggle on karne par app current DNS/Private DNS ko same rakhkar Settings se DNS changes block karega."
}
