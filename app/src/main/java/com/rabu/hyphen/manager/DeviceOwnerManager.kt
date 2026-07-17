package com.rabu.hyphen.manager

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.os.UserManager
import com.rabu.hyphen.admin.MyDeviceAdminReceiver

class DeviceOwnerManager(private val context: Context) {
    private val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)

    private val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)
    private val owndroidComponent = ComponentName(OWNDROID_PACKAGE, OWNDROID_RECEIVER)

    fun isDeviceOwner(): Boolean = devicePolicyManager.isDeviceOwnerApp(context.packageName)

    fun transferOwnershipToOwndroid() {
        devicePolicyManager.transferOwnership(adminComponent, owndroidComponent, PersistableBundle())
    }

    fun canControlPrivateDns(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    fun isPrivateDnsConfigDisabled(): Boolean =
        canControlPrivateDns() &&
            devicePolicyManager
                .getUserRestrictions(adminComponent)
                .getBoolean(UserManager.DISALLOW_CONFIG_PRIVATE_DNS, false)

    fun setPrivateDnsConfigDisabled(disabled: Boolean) {
        if (!canControlPrivateDns()) return

        if (disabled) {
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_PRIVATE_DNS)
        } else {
            devicePolicyManager.clearUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_PRIVATE_DNS)
        }
    }

    companion object {
        const val OWNDROID_PACKAGE = "com.bintianqi.owndroid"
        const val OWNDROID_RECEIVER = "com.bintianqi.owndroid.Receiver"
    }
}
