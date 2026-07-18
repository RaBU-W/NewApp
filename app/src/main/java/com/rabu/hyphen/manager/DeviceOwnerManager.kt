package com.rabu.hyphen.manager

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.rabu.hyphen.admin.MyDeviceAdminReceiver
import com.rabu.hyphen.service.PrivateDnsEnforcerService

class DeviceOwnerManager(private val context: Context) {
    private val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)
    private val actualDeviceOwnerComponent: ComponentName?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            devicePolicyManager.getDeviceOwnerComponentOnAnyUser()
        } else {
            adminComponent
        }
    private val owndroidComponent = ComponentName(OWNDROID_PACKAGE, OWNDROID_RECEIVER)

    fun isDeviceOwner(): Boolean = devicePolicyManager.isDeviceOwnerApp(context.packageName)

    fun transferOwnershipToOwndroid() {
        devicePolicyManager.transferOwnership(adminComponent, owndroidComponent, PersistableBundle())
    }

    fun canEnforcePrivateDns(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun isPrivateDnsEnforcementEnabled(): Boolean =
        canEnforcePrivateDns() && preferences.getBoolean(KEY_PRIVATE_DNS_ENFORCEMENT_ENABLED, false)

    fun setPrivateDnsEnforcementEnabled(enabled: Boolean): PrivateDnsEnforcementResult {
        if (!canEnforcePrivateDns()) {
            return PrivateDnsEnforcementResult.Error("Private DNS ke liye Android 10+ required hai.")
        }

        if (!enabled) {
            preferences.edit().putBoolean(KEY_PRIVATE_DNS_ENFORCEMENT_ENABLED, false).apply()
            PrivateDnsEnforcerService.stop(context)
            return PrivateDnsEnforcementResult.Success
        }

        val result = enforceRequiredPrivateDns()
        if (result is PrivateDnsEnforcementResult.Error) {
            preferences.edit().putBoolean(KEY_PRIVATE_DNS_ENFORCEMENT_ENABLED, false).apply()
            PrivateDnsEnforcerService.stop(context)
            return result
        }

        preferences.edit().putBoolean(KEY_PRIVATE_DNS_ENFORCEMENT_ENABLED, true).apply()
        PrivateDnsEnforcerService.start(context)
        return PrivateDnsEnforcementResult.Success
    }

    fun enforceRequiredPrivateDns(): PrivateDnsEnforcementResult {
        if (!canEnforcePrivateDns()) {
            return PrivateDnsEnforcementResult.Error("Private DNS ke liye Android 10+ required hai.")
        }
        if (!isDeviceOwner()) {
            return PrivateDnsEnforcementResult.Error("App Device Owner nahi hai.")
        }

        return runCatching {
            logDeviceOwnerComponents()
            applyPrivateDnsWithDevicePolicyService(
                mode = DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME,
                host = REQUIRED_PRIVATE_DNS_HOST,
            )
            PrivateDnsEnforcementResult.Success
        }.getOrElse { throwable ->
            PrivateDnsEnforcementResult.Error(throwable.stackTraceToString())
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun applyPrivateDnsWithDevicePolicyService(mode: Int, host: String?) {
        val ownerComponent = actualDeviceOwnerComponent
            ?: error("Actual Device Owner component nahi mila")

        when (mode) {
            DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME -> {
                require(!host.isNullOrBlank()) { "Private DNS hostname empty hai" }
                val result = devicePolicyManager.setGlobalPrivateDnsModeSpecifiedHost(ownerComponent, host)
                check(result == DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR) {
                    "Private DNS rejected. Result code: $result"
                }
            }

            DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC -> {
                val result = devicePolicyManager.setGlobalPrivateDnsModeOpportunistic(ownerComponent)
                check(result == DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR) {
                    "Private DNS rejected. Result code: $result"
                }
            }

            else -> error("Unsupported Private DNS mode: $mode")
        }
    }

    private fun logDeviceOwnerComponents() {
        Log.d(LOG_TAG, "Package is device owner: ${isDeviceOwner()}")
        Log.d(LOG_TAG, "Configured admin component: $adminComponent")
        Log.d(LOG_TAG, "Actual owner component: $actualDeviceOwnerComponent")
    }

    sealed interface PrivateDnsEnforcementResult {
        data object Success : PrivateDnsEnforcementResult
        data class Error(val message: String) : PrivateDnsEnforcementResult
    }

    companion object {
        const val OWNDROID_PACKAGE = "com.bintianqi.owndroid"
        const val OWNDROID_RECEIVER = "com.bintianqi.owndroid.Receiver"
        const val REQUIRED_PRIVATE_DNS_HOST = "dns.google"
        private const val PREFERENCES_NAME = "device_owner_policies"
        private const val KEY_PRIVATE_DNS_ENFORCEMENT_ENABLED = "private_dns_enforcement_enabled"
        private const val LOG_TAG = "PrivateDns"
    }
}
