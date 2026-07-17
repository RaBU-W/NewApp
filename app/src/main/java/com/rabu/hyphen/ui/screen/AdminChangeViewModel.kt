package com.rabu.hyphen.ui.screen

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rabu.hyphen.manager.MyDevicePolicyManager

class AdminChangeViewModel(context: Context) {
    val MyDevicePolicyManagerObject = MyDevicePolicyManager(context)

    var isDeviceOwner by mutableStateOf(false)

    init{
        isDeviceOwner = MyDevicePolicyManagerObject.isDeviceOwner()
    }

    fun transferOwner() {
        MyDevicePolicyManagerObject.TransfarOwnerShipToOwndroid()
        isDeviceOwner = MyDevicePolicyManagerObject.isDeviceOwner()
    }
}