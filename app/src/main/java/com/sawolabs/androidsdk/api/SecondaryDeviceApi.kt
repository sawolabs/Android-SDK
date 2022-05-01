package com.sawolabs.androidsdk.api

import com.sawolabs.androidsdk.model.TrustedDevice
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface SecondaryDeviceApi {
    @Headers("content-type: application/json")
    @POST("/api/v1/secondary_trusted_device/")
    suspend fun sendTrustedResponse(@Body trustedDevice: TrustedDevice): Response<Any>
}