package com.sawolabs.androidsdk.repository

import com.sawolabs.androidsdk.api.RetrofitInstance
import com.sawolabs.androidsdk.model.Device
import com.sawolabs.androidsdk.model.TrustedDevice
import retrofit2.Response

class Repository {

    suspend fun registerDevice(device: Device): Response<Void> {
        return RetrofitInstance.registerDeviceApi.registerDevice(device = device)
    }

    suspend fun sendTrustedResponse(trustedDevice: TrustedDevice): Response<Any> {
        return RetrofitInstance.secondaryDeviceApi.sendTrustedResponse(trustedDevice = trustedDevice)
    }

}