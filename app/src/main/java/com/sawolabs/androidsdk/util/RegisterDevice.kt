package com.sawolabs.androidsdk.util

import android.content.SharedPreferences
import android.os.Build
import com.onesignal.OneSignal
import com.sawolabs.androidsdk.model.Device
import com.sawolabs.androidsdk.util.Constants.Companion.SHARED_PREF_DEVICE_ID_KEY
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel

object RegisterDevice {
    fun registerDevice(sharedPreferences: SharedPreferences): Device {
        val deviceData: Device
        val device = OneSignal.getDeviceState()

        val oneSignalDeviceId = device?.userId
        val oneSignaldeviceToken = device?.pushToken


        if ((oneSignalDeviceId != null) and (oneSignaldeviceToken != null)) {
            if (oneSignalDeviceId != null) {
                sharedPreferences.edit().putString(
                    SHARED_PREF_DEVICE_ID_KEY, oneSignalDeviceId
                ).apply()
            }
        }

        deviceData = Device(
            device_token = oneSignaldeviceToken.toString(),
            device_id = oneSignalDeviceId.toString(),
            device_brand = Build.MANUFACTURER.toString(),
            device_model = Build.MODEL.toString(),
            device_name = getDeviceName(),
            sdk_variant = "android"

        )

        // SENTRY Tag and Breadcrumb
        val activity = this.javaClass.simpleName
        Sentry.setTag("activity", activity)


        val breadcrumb = Breadcrumb()
        breadcrumb.message = "Retrofit call to register device information"
        breadcrumb.level = SentryLevel.INFO
        breadcrumb.setData("Activity Name", activity)
        Sentry.addBreadcrumb(breadcrumb)

        return deviceData
    }

    private fun getDeviceName(): String {
        return "${capitalize(Build.MANUFACTURER)} ${capitalize(Build.MODEL)}"
    }

    private fun capitalize(s: String?): String {
        if (s == null || s.isEmpty()) {
            return ""
        }
        return if (Character.isUpperCase(s[0])) {
            s
        } else {
            Character.toUpperCase(s[0]).toString() + s.substring(1)
        }
    }
}