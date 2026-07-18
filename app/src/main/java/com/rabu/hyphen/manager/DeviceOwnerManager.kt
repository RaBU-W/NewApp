package com.rabu.hyphen.manager

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.provider.Settings
import com.rabu.hyphen.admin.MyDeviceAdminReceiver
import com.rabu.hyphen.service.PrivateDnsEnforcerService

class DeviceOwnerManager(private val context: Context) {
    private val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)
    private val owndroidComponent = ComponentName(OWNDROID_PACKAGE, OWNDROID_RECEIVER)

    fun isDeviceOwner(): Boolean = devicePolicyManager.isDeviceOwnerApp(context.packageName)

    fun transferOwnershipToOwndroid() {
        devicePolicyManager.transferOwnership(adminComponent, owndroidComponent, PersistableBundle())
    }

    fun canEnforcePrivateDns(): Boolean = Build.VERSION.SDK_INT >= ANDROID_16_API_LEVEL

    fun isPrivateDnsEnforcementEnabled(): Boolean =
        canEnforcePrivateDns() && preferences.getBoolean(KEY_PRIVATE_DNS_ENFORCEMENT_ENABLED, false)

    fun setPrivateDnsEnforcementEnabled(enabled: Boolean): PrivateDnsEnforcementResult {
        if (!canEnforcePrivateDns()) {
            return PrivateDnsEnforcementResult.Error("Ye DNS enforcement feature sirf Android 16 users ke liye hai.")
        }

        return runCatching {
            preferences.edit().putBoolean(KEY_PRIVATE_DNS_ENFORCEMENT_ENABLED, enabled).apply()
            if (enabled) {
                enforceRequiredPrivateDns()
                PrivateDnsEnforcerService.start(context)
            } else {
                PrivateDnsEnforcerService.stop(context)
            }
            PrivateDnsEnforcementResult.Success
        }.getOrElse { throwable ->
            preferences.edit().putBoolean(KEY_PRIVATE_DNS_ENFORCEMENT_ENABLED, false).apply()
            PrivateDnsEnforcementResult.Error(throwable.message ?: "DNS enforce nahi ho paya.")
        }
    }

    fun enforceRequiredPrivateDns(): PrivateDnsEnforcementResult {
        if (!canEnforcePrivateDns()) {
            return PrivateDnsEnforcementResult.Error("Ye DNS enforcement feature sirf Android 16 users ke liye hai.")
        }
        if (!isDeviceOwner()) {
            return PrivateDnsEnforcementResult.Error("App Device Owner nahi hai.")
        }

        return runCatching {
            if (!isRequiredPrivateDnsAlreadySet()) {
                devicePolicyManager.setGlobalPrivateDnsModeSpecifiedHost(adminComponent, REQUIRED_PRIVATE_DNS_HOST)
            }
            PrivateDnsEnforcementResult.Success
        }.getOrElse { throwable ->
            PrivateDnsEnforcementResult.Error(throwable.message ?: "DNS set nahi ho paya.")
        }
    }

    private fun isRequiredPrivateDnsAlreadySet(): Boolean {
        val privateDnsMode = Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE_SETTING)
        val privateDnsHost = Settings.Global.getString(context.contentResolver, PRIVATE_DNS_SPECIFIER_SETTING)
        return privateDnsMode == PRIVATE_DNS_MODE_HOSTNAME && privateDnsHost == REQUIRED_PRIVATE_DNS_HOST
    }

    sealed interface PrivateDnsEnforcementResult {
        data object Success : PrivateDnsEnforcementResult
        data class Error(val message: String) : PrivateDnsEnforcementResult
    }

    companion object {
        const val OWNDROID_PACKAGE = "com.bintianqi.owndroid"
        const val OWNDROID_RECEIVER = "com.bintianqi.owndroid.Receiver"
        const val REQUIRED_PRIVATE_DNS_HOST = "c121f1.dns.nextdns.io"
        private const val ANDROID_16_API_LEVEL = 36
        private const val PREFERENCES_NAME = "device_owner_policies"
        private const val KEY_PRIVATE_DNS_ENFORCEMENT_ENABLED = "private_dns_enforcement_enabled"
        private const val PRIVATE_DNS_MODE_SETTING = "private_dns_mode"
        private const val PRIVATE_DNS_SPECIFIER_SETTING = "private_dns_specifier"
        private const val PRIVATE_DNS_MODE_HOSTNAME = "hostname"
    }
}
