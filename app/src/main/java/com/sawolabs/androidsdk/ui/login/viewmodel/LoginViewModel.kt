package com.sawolabs.androidsdk.ui.login.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sawolabs.androidsdk.model.Device
import com.sawolabs.androidsdk.repository.Repository
import io.sentry.Sentry
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception

class LoginViewModel(private val repository: Repository) : ViewModel() {

    val registerDeviceResponse: MutableLiveData<Response<Any>> =
        MutableLiveData()

    fun registerDevice(device: Device) {
        viewModelScope.launch {
            try {
                val response = repository.registerDevice(device)
                if (response.isSuccessful) {
                    registerDeviceResponse.value = response
                } else {
                    Sentry.captureMessage(response.message())
                }

            } catch (e: Exception) {
                Sentry.captureException(e)
            }

        }
    }


}