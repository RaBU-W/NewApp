package com.rabu.hyphen.manager

import android.app.admin.DevicePolicyManager
import android.app.admin.IDevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.annotation.RequiresApi
import com.rabu.hyphen.admin.MyDeviceAdminReceiver
import com.rabu.hyphen.service.PrivateDnsEnforcerService
import kotlinx.coroutines.flow.MutableStateFlow

class DeviceOwnerManager(private val context: Context) {
    private val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)!!
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)
    private val owndroidComponent = ComponentName(OWNDROID_PACKAGE, OWNDROID_RECEIVER)
    private val lastPrivateDnsStatus = MutableStateFlow("Not tested")

    fun getLastPrivateDnsStatus(): String = lastPrivateDnsStatus.value

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
            val message = "Private DNS ke liye Android 10+ required hai."
            setPrivateDnsDiagnosticStatus(finalError = message)
            return PrivateDnsEnforcementResult.Error(message)
        }

        lastPrivateDnsStatus.value = "Checking Device Owner..."
        val owner = isDeviceOwner()
        val active = devicePolicyManager.isAdminActive(adminComponent)
        setPrivateDnsDiagnosticStatus(owner = owner, active = active, finalError = null)

        if (!owner) {
            val message = "App Device Owner nahi hai"
            setPrivateDnsDiagnosticStatus(owner = owner, active = active, finalError = message)
            return PrivateDnsEnforcementResult.Error(message)
        }

        if (!active) {
            val message = "Device Admin receiver active nahi hai"
            setPrivateDnsDiagnosticStatus(owner = owner, active = active, finalError = message)
            return PrivateDnsEnforcementResult.Error(message)
        }

        return applyPrivateDnsLikeOwnDroid()
    }

    @Suppress("PrivateApi")
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun applyPrivateDnsLikeOwnDroid(): PrivateDnsEnforcementResult {
        val mode = DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME
        val host = REQUIRED_PRIVATE_DNS_HOST
        val owner = isDeviceOwner()
        val active = devicePolicyManager.isAdminActive(adminComponent)

        return try {
            lastPrivateDnsStatus.value = "Applying host=$host, component=$adminComponent"
            val field = DevicePolicyManager::class.java.getDeclaredField("mService")
            field.isAccessible = true

            val service = field.get(devicePolicyManager) as IDevicePolicyManager
            val result = service.setGlobalPrivateDns(adminComponent, mode, host)

            if (result == DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR) {
                setPrivateDnsDiagnosticStatus(
                    owner = owner,
                    active = active,
                    mode = mode,
                    serviceClass = service.javaClass.name,
                    dpmResult = result,
                    finalError = null,
                )
                PrivateDnsEnforcementResult.Success
            } else {
                val message = "OwnDroid-style DPM call failed. Result=$result"
                setPrivateDnsDiagnosticStatus(
                    owner = owner,
                    active = active,
                    mode = mode,
                    serviceClass = service.javaClass.name,
                    dpmResult = result,
                    exception = null,
                    finalError = message,
                )
                PrivateDnsEnforcementResult.Error(message)
            }
        } catch (e: Exception) {
            val message = "${e.javaClass.simpleName}: ${e.message}"
            setPrivateDnsDiagnosticStatus(
                owner = owner,
                active = active,
                mode = mode,
                dpmResult = null,
                exception = message,
                finalError = message,
            )
            PrivateDnsEnforcementResult.Error(message)
        }
    }


    private fun setPrivateDnsDiagnosticStatus(
        owner: Boolean = isDeviceOwner(),
        active: Boolean = devicePolicyManager.isAdminActive(adminComponent),
        mode: Int = DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME,
        dpmClass: String = devicePolicyManager.javaClass.name,
        serviceClass: String = "Not available",
        dpmResult: Int? = null,
        exception: String? = null,
        finalError: String?,
    ) {
        val dpmResultText = dpmResult?.toString() ?: "Not tested"
        val exceptionText = exception ?: "None"
        val finalErrorText = finalError ?: "None"
        val componentText = "${adminComponent.packageName}/${adminComponent.className}"

        lastPrivateDnsStatus.value = StringBuilder()
            .append("Device Owner: ").append(owner).append('\n')
            .append("Admin active: ").append(active).append('\n')
            .append("DPM class: ").append(dpmClass).append('\n')
            .append("Service class: ").append(serviceClass).append('\n')
            .append("Admin component: ").append(componentText).append('\n')
            .append("Mode: ").append(mode).append('\n')
            .append("Hostname: ").append(REQUIRED_PRIVATE_DNS_HOST).append('\n')
            .append("DPM result code: ").append(dpmResultText).append('\n')
            .append("Exception: ").append(exceptionText).append('\n')
            .append("Final readable error: ").append(finalErrorText)
            .toString()
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
    }
}
