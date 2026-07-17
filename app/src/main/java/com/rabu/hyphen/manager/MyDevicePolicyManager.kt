package com.rabu.hyphen.manager

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import com.rabu.hyphen.admin.MyDeviceAdminReceiver

class MyDevicePolicyManager(
    private val context: Context
) {

    private val devicePolicyManager =
        context.getSystemService(DevicePolicyManager::class.java)

    fun isDeviceOwner(): Boolean {
        return devicePolicyManager.isDeviceOwnerApp(context.packageName)
    }

    fun TransfarOwnerShipToOwndroid() {
        val admin = ComponentName(context, MyDeviceAdminReceiver::class.java)
        val target = ComponentName(
            "com.bintianqi.owndroid",
            "com.bintianqi.owndroid.Receiver"
        )

        devicePolicyManager.transferOwnership(admin, target, null)
    }
}