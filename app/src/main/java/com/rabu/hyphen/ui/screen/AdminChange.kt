package com.rabu.hyphen.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun AdminChange() {

    // Veriables
    val context = LocalContext.current
    val AdminChangeViewModelObject = AdminChangeViewModel(context)

    Column(modifier = Modifier.fillMaxSize().absolutePadding(top = 16.dp)) {
        Text("Aver Humara App Owner Hoga To ye Button enable hoga else Diseble")

        Button(
                onClick = { AdminChangeViewModelObject.transferOwner() },
                enabled = AdminChangeViewModelObject.isDeviceOwner
        ) { Text("Transfer Ownership") }
    }
}