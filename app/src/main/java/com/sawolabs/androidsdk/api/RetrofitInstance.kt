package com.sawolabs.androidsdk.api

import com.sawolabs.androidsdk.util.Constants.Companion.BASE_URL
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {


    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    val registerDeviceApi: RegisterDeviceApi by lazy {
        retrofit.create(RegisterDeviceApi::class.java)
    }

    val secondaryDeviceApi: SecondaryDeviceApi by lazy {
        retrofit.create(SecondaryDeviceApi::class.java)
    }
}