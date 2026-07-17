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

@Composable
fun OwnershipTransferScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val manager = remember(context) { DeviceOwnerManager(context.applicationContext) }
    var isDeviceOwner by remember { mutableStateOf(manager.isDeviceOwner()) }
    var statusMessage by remember { mutableStateOf(ownerStatusText(isDeviceOwner)) }

    fun refreshOwnerState() {
        isDeviceOwner = manager.isDeviceOwner()
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
        }
    }
}

private fun ownerStatusText(isDeviceOwner: Boolean): String =
    if (isDeviceOwner) {
        "Device Owner permission mil gayi hai. Ab ownership Owndroid ko transfer kar sakte ho."
    } else {
        "Abhi ye app Device Owner nahi hai. Pehle Owndroid se ownership is app ko transfer karo."
    }
