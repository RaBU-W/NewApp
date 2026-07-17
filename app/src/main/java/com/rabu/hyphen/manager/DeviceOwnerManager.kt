package com.rabu.hyphen.manager

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.os.UserManager
import android.provider.Settings
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
        canBlockPrivateDnsConfig() &&
            runCatching {
                devicePolicyManager
                    .getUserRestrictionsGlobally()
                    .getBoolean(UserManager.DISALLOW_CONFIG_PRIVATE_DNS, false)
            }.getOrDefault(false)

    fun setPrivateDnsConfigBlocked(blocked: Boolean): PrivateDnsBlockResult {
        if (!canBlockPrivateDnsConfig()) {
            return PrivateDnsBlockResult.Error("Ye DNS block feature sirf Android 16 users ke liye hai.")
        }

        return runCatching {
            if (blocked) {
                keepCurrentPrivateDnsModeManaged()
                devicePolicyManager.addUserRestrictionGlobally(UserManager.DISALLOW_CONFIG_PRIVATE_DNS)
            } else {
                devicePolicyManager.clearUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_PRIVATE_DNS)
            }
            PrivateDnsBlockResult.Success
        }.getOrElse { throwable ->
            PrivateDnsBlockResult.Error(throwable.message ?: "DNS policy apply nahi ho payi.")
        }
    }

    private fun keepCurrentPrivateDnsModeManaged() {
        val privateDnsMode = Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE_SETTING)
        val privateDnsHost = Settings.Global.getString(context.contentResolver, PRIVATE_DNS_SPECIFIER_SETTING)

        if (privateDnsMode == PRIVATE_DNS_MODE_HOSTNAME && !privateDnsHost.isNullOrBlank()) {
            devicePolicyManager.setGlobalPrivateDnsModeSpecifiedHost(adminComponent, privateDnsHost)
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
        private const val PRIVATE_DNS_MODE_SETTING = "private_dns_mode"
        private const val PRIVATE_DNS_SPECIFIER_SETTING = "private_dns_specifier"
        private const val PRIVATE_DNS_MODE_HOSTNAME = "hostname"
    }
}
