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
        canBlockPrivateDnsConfig() && DNS_BLOCK_RESTRICTIONS.any { restriction ->
            hasRestrictionGlobally(restriction) || hasRestrictionLocally(restriction)
        }

    private fun hasRestrictionGlobally(restriction: String): Boolean =
        runCatching {
            devicePolicyManager
                .getUserRestrictionsGlobally()
                .getBoolean(restriction, false)
        }.getOrDefault(false)

    private fun hasRestrictionLocally(restriction: String): Boolean =
        runCatching {
            devicePolicyManager
                .getUserRestrictions(adminComponent)
                .getBoolean(restriction, false)
        }.getOrDefault(false)

    fun setPrivateDnsConfigBlocked(blocked: Boolean): PrivateDnsBlockResult {
        if (!canBlockPrivateDnsConfig()) {
            return PrivateDnsBlockResult.Error("Ye DNS block feature sirf Android 16 users ke liye hai.")
        }

        return runCatching {
            if (blocked) {
                DNS_BLOCK_RESTRICTIONS.forEach { restriction ->
                    devicePolicyManager.addUserRestriction(adminComponent, restriction)
                }
            } else {
                DNS_BLOCK_RESTRICTIONS.forEach { restriction ->
                    devicePolicyManager.clearUserRestriction(adminComponent, restriction)
                }
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
        private val DNS_BLOCK_RESTRICTIONS = listOf(
            UserManager.DISALLOW_CONFIG_PRIVATE_DNS,
            UserManager.DISALLOW_CONFIG_WIFI,
            UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS,
        )
    }
}
