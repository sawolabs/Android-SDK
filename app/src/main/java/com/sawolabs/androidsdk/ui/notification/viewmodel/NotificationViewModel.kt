package com.sawolabs.androidsdk.ui.notification.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sawolabs.androidsdk.model.RegisterDeviceResponse
import com.sawolabs.androidsdk.model.TrustedDevice
import com.sawolabs.androidsdk.repository.Repository
import io.sentry.Sentry
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception

class NotificationViewModel(private val repository: Repository) : ViewModel() {

    val trustedDeviceResponse: MutableLiveData<Response<Any>> =
        MutableLiveData()

    fun sendTrustedResponse(trustedDevice: TrustedDevice) {
        viewModelScope.launch {
            try {
                val response = repository.sendTrustedResponse(trustedDevice)
                if (response.isSuccessful) {
                    trustedDeviceResponse.value = response
                } else {
                    Sentry.captureMessage(response.message())
                }

            } catch (e: Exception) {
                Sentry.captureException(e)
            }

        }
    }


}