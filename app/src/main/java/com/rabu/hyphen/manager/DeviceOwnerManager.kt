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

    fun canBlockPrivateDnsConfig(): Boolean = Build.VERSION.SDK_INT >= ANDROID_16_API_LEVEL

    fun isPrivateDnsConfigBlocked(): Boolean =
        canBlockPrivateDnsConfig() && (isPrivateDnsBlockedGlobally() || isPrivateDnsBlockedLocally())

    private fun isPrivateDnsBlockedGlobally(): Boolean =
        runCatching {
            devicePolicyManager
                .getUserRestrictionsGlobally()
                .getBoolean(UserManager.DISALLOW_CONFIG_PRIVATE_DNS, false)
        }.getOrDefault(false)

    private fun isPrivateDnsBlockedLocally(): Boolean =
        runCatching {
            devicePolicyManager
                .getUserRestrictions(adminComponent)
                .getBoolean(UserManager.DISALLOW_CONFIG_PRIVATE_DNS, false)
        }.getOrDefault(false)

    fun setPrivateDnsConfigBlocked(blocked: Boolean): PrivateDnsBlockResult {
        if (!canBlockPrivateDnsConfig()) {
            return PrivateDnsBlockResult.Error("Ye DNS block feature sirf Android 16 users ke liye hai.")
        }

        return runCatching {
            if (blocked) {
                devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_PRIVATE_DNS)
            } else {
                devicePolicyManager.clearUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_PRIVATE_DNS)
            }
            PrivateDnsBlockResult.Success
        }.getOrElse { throwable ->
            PrivateDnsBlockResult.Error(throwable.message ?: "DNS policy apply nahi ho payi.")
        }
    }

    sealed interface PrivateDnsBlockResult {
        data object Success : PrivateDnsBlockResult
        data class Error(val message: String) : PrivateDnsBlockResult
    }

    companion object {
        const val OWNDROID_PACKAGE = "com.bintianqi.owndroid"
        const val OWNDROID_RECEIVER = "com.bintianqi.owndroid.Receiver"
        private const val ANDROID_16_API_LEVEL = 36
    }
}
