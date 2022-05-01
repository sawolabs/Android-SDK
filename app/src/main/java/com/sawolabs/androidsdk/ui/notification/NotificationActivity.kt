package com.sawolabs.androidsdk.ui.notification

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.sawolabs.androidsdk.R
import com.sawolabs.androidsdk.databinding.ActivityNotificationBinding
import com.sawolabs.androidsdk.model.PushNotificationAdditionalData
import com.sawolabs.androidsdk.model.TrustedDevice
import com.sawolabs.androidsdk.repository.Repository
import com.sawolabs.androidsdk.ui.notification.viewmodel.NotificationViewModel
import com.sawolabs.androidsdk.ui.notification.viewmodel.NotificationViewModelFactory
import com.sawolabs.androidsdk.util.BiometricPromptUtils
import com.sawolabs.androidsdk.util.Constants.Companion.SHARED_PREF_DEVICE_ID_KEY
import com.sawolabs.androidsdk.util.Constants.Companion.SHARED_PREF_FILENAME
import com.sawolabs.androidsdk.util.Constants.Companion.TRUSTED_DEVICE_NOTIFICATION_ADDITIONAL_DATA
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import org.json.JSONObject

private const val TAG = "NotificationActivity"

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationBinding
    private lateinit var viewModel: NotificationViewModel
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var yesBtn: Button
    private lateinit var noBtn: Button
    private lateinit var deviceInformationText: TextView
    private lateinit var additionalData: PushNotificationAdditionalData


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val repository = Repository()
        val viewModelFactory = NotificationViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory)[NotificationViewModel::class.java]

        additionalData = Gson().fromJson(
            intent.getStringExtra(TRUSTED_DEVICE_NOTIFICATION_ADDITIONAL_DATA),
            PushNotificationAdditionalData::class.java
        )
        deviceInformationText = binding.textView
        yesBtn = binding.button2
        noBtn = binding.button3
        deviceInformationText.text = getString(
            R.string.trusted_device_notification_activity_text_view_1_text,
            "${additionalData.secondary_device_brand}  ${additionalData.secondary_device_model}"
        )


        biometricPrompt =
            BiometricPromptUtils.createBiometricPrompt(this, ::processCancel, ::processSuccess)
        promptInfo = BiometricPromptUtils.createPromptInfo(this)

        biometricPrompt.authenticate(promptInfo)
    }

    private fun processCancel() {
        Toast.makeText(
            applicationContext,
            getString(R.string.trusted_device_notification_activity_biometric_cancel),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun processSuccess(cryptoObject: BiometricPrompt.CryptoObject?) {
        yesBtn.visibility = View.VISIBLE
        noBtn.visibility = View.VISIBLE
        deviceInformationText.visibility = View.VISIBLE
        binding.textView2.visibility = View.VISIBLE
    }

    fun callApi(view: View) {
        when (view.id) {
            binding.button2.id -> {
                callApiSecondaryDevice(true)
                finish()
            }
            binding.button3.id -> {
                callApiSecondaryDevice(false)
                finish()
            }
            else -> {
                throw RuntimeException("Unknown button pressed")
            }
        }
    }

    private fun callApiSecondaryDevice(userTrustedDevice: Boolean) {
        val sharedPref = getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE)


        val trustedDevice = TrustedDevice(
            additionalData.trusted_id,
            additionalData.secondary_id,
            sharedPref.getString(SHARED_PREF_DEVICE_ID_KEY, null).toString(),
            if (userTrustedDevice) "allowed" else "denied"
        )
        // SENTRY Tag and Breadcrumb
        val activity = this.javaClass.simpleName
        Sentry.setTag("activity", activity)


        val breadcrumb = Breadcrumb()
        breadcrumb.message = "Retrofit call for secondary trusted device"
        breadcrumb.level = SentryLevel.INFO
        breadcrumb.setData("Activity Name", activity)
        Sentry.addBreadcrumb(breadcrumb)

        viewModel.sendTrustedResponse(trustedDevice = trustedDevice)
        viewModel.trustedDeviceResponse.observe(this) { response ->

            if (response.isSuccessful) {
                if (userTrustedDevice) {
                    Toast.makeText(
                        this@NotificationActivity,
                        getString(R.string.trusted_device_notification_activity_device_authorized),
                        Toast.LENGTH_LONG
                    ).show()

                } else if (userTrustedDevice) {
                    Toast.makeText(
                        this@NotificationActivity,
                        getString(R.string.trusted_device_notification_activity_device_rejected),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                try {
                    Log.d(
                        TAG,
                        "SecondaryTrustedDeviceApi: Server responded with error ${
                            JSONObject(
                                response.errorBody()!!.string()
                            )
                        }"
                    )
                } catch (e: Exception) {
                    Sentry.captureException(e)
                    Log.d(

                        TAG,
                        "SecondaryTrustedDeviceApi: Error in parsing server error response ${e.message}"
                    )
                }
            }

        }


    }


}

