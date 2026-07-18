package com.rabu.hyphen.manager

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.annotation.RequiresApi
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
        when (mode) {
            DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME -> {
                require(!host.isNullOrBlank()) { "Private DNS hostname empty hai" }
                val result = devicePolicyManager.setGlobalPrivateDnsModeSpecifiedHost(adminComponent, host)
                check(result == DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR) {
                    "Private DNS rejected. Result code: $result"
                }
            }

            DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC -> {
                val result = devicePolicyManager.setGlobalPrivateDnsModeOpportunistic(adminComponent)
                check(result == DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR) {
                    "Private DNS rejected. Result code: $result"
                }
            }

            DevicePolicyManager.PRIVATE_DNS_MODE_OFF -> {
                setPrivateDnsUsingDpmService(mode, null)
            }

            else -> error("Unsupported Private DNS mode: $mode")
        }
    }

    @Suppress("PrivateApi")
    private fun setPrivateDnsUsingDpmService(mode: Int, host: String?) {
        val serviceField = DevicePolicyManager::class.java.getDeclaredField("mService")
        serviceField.isAccessible = true

        val service = serviceField.get(devicePolicyManager)
            ?: error("DevicePolicyManager service unavailable")

        val method = service.javaClass.methods.firstOrNull {
            it.name == "setGlobalPrivateDns" && it.parameterTypes.size == 3
        } ?: error("setGlobalPrivateDns method nahi mila")

        val result = method.invoke(service, adminComponent, mode, host) as Int
        check(result == DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR) {
            "Private DNS rejected. Result code: $result"
        }
    }

    sealed interface PrivateDnsEnforcementResult {
        data object Success : PrivateDnsEnforcementResult
        data class Error(val message: String) : PrivateDnsEnforcementResult
    }

    companion object {
        const val OWNDROID_PACKAGE = "com.bintianqi.owndroid"
        const val OWNDROID_RECEIVER = "com.bintianqi.owndroid.Receiver"
        const val REQUIRED_PRIVATE_DNS_HOST = "c121f1.dns.nextdns.io"
        private const val PREFERENCES_NAME = "device_owner_policies"
        private const val KEY_PRIVATE_DNS_ENFORCEMENT_ENABLED = "private_dns_enforcement_enabled"
    }
}
