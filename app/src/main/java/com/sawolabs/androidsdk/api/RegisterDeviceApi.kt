package com.sawolabs.androidsdk.api

import com.sawolabs.androidsdk.model.Device
import com.sawolabs.androidsdk.model.RegisterDeviceResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RegisterDeviceApi {

    @POST("/api/v1/players")
    suspend fun registerDevice(@Body device: Device): Response<RegisterDeviceResponse>

}