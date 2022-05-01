package com.sawolabs.androidsdk.ui.login

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.onesignal.OSSubscriptionObserver
import com.onesignal.OSSubscriptionStateChanges
import com.onesignal.OneSignal
import com.sawolabs.androidsdk.R
import com.sawolabs.androidsdk.api.SawoWebSDKInterface
import com.sawolabs.androidsdk.databinding.ActivityLoginBinding
import com.sawolabs.androidsdk.repository.Repository
import com.sawolabs.androidsdk.ui.login.viewmodel.LoginViewModel
import com.sawolabs.androidsdk.ui.login.viewmodel.LoginViewModelFactory
import com.sawolabs.androidsdk.util.BiometricPromptUtils
import com.sawolabs.androidsdk.util.CheckConnectionStatus.isOnline
import com.sawolabs.androidsdk.util.Constants.Companion.CALLBACK_CLASS
import com.sawolabs.androidsdk.util.Constants.Companion.LOGIN_SUCCESS_MESSAGE
import com.sawolabs.androidsdk.util.Constants.Companion.SAWO_WEBSDK_URL
import com.sawolabs.androidsdk.util.Constants.Companion.SHARED_PREF_DEVICE_ID_KEY
import com.sawolabs.androidsdk.util.Constants.Companion.SHARED_PREF_ENC_PAIR_KEY
import com.sawolabs.androidsdk.util.Constants.Companion.SHARED_PREF_FILENAME
import com.sawolabs.androidsdk.util.CryptographyManager
import com.sawolabs.androidsdk.util.RegisterDevice.registerDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


private const val TAG = "LoginActivity"

class LoginActivity : AppCompatActivity(), OSSubscriptionObserver {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var viewModel: LoginViewModel
    private lateinit var sharedPref: SharedPreferences
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var cryptographyManager: CryptographyManager
    private lateinit var mWebView: WebView
    private lateinit var dataToEncrypt: String
    private lateinit var callBackClassName: String
    private lateinit var sawoWebSDKURL: String
    private lateinit var mProgressBar: ProgressBar
    private val encryptedData
        get() = cryptographyManager.getEncryptedDataFromSharedPrefs(
            applicationContext,
            SHARED_PREF_FILENAME,
            Context.MODE_PRIVATE,
            SHARED_PREF_ENC_PAIR_KEY
        )
    private var readyToEncrypt: Boolean = false
    private val secretKeyName = "SAWO_BIOMETRIC_ENCRYPTION_KEY"
    private var keyExistInStorage: Boolean = false
    private var canStoreKeyInStorage: Boolean = false


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE)
        OneSignal.addSubscriptionObserver(this)
        val deviceData = registerDevice(sharedPref)
        sawoWebSDKURL = intent.getStringExtra(SAWO_WEBSDK_URL).toString()
        callBackClassName = intent.getStringExtra(CALLBACK_CLASS).toString()
        cryptographyManager = CryptographyManager()
        biometricPrompt = BiometricPromptUtils.createBiometricPrompt(
            this, ::processCancel, ::processData
        )
        promptInfo = BiometricPromptUtils.createPromptInfo(this)
        mWebView = binding.webview
        mProgressBar = binding.progressBar
        keyExistInStorage = cryptographyManager.isDataExistInSharedPrefs(
            this, SHARED_PREF_FILENAME, Context.MODE_PRIVATE, SHARED_PREF_ENC_PAIR_KEY
        )
        canStoreKeyInStorage =
            BiometricManager.from(applicationContext)
                .canAuthenticate(BIOMETRIC_STRONG) == BiometricManager
                .BIOMETRIC_SUCCESS

        if (!isOnline(this)) {
            Toast.makeText(this, "Internet connection unavailable", Toast.LENGTH_LONG).show()
            mWebView.destroy()
        }
        sawoWebSDKURL += "&keysExistInStorage=${keyExistInStorage}&canStoreKeyInStorage=${canStoreKeyInStorage}"
        mWebView.apply {
            this.settings.javaScriptEnabled = true
            this.settings.domStorageEnabled = true
            this.settings.databaseEnabled = true
            this.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    view?.loadUrl(request?.url.toString())
                    return super.shouldOverrideUrlLoading(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    mProgressBar.visibility = View.GONE
                    mWebView.visibility = View.VISIBLE
                }
            }
        }

        val repository = Repository()
        val viewModelFactory = LoginViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory)[LoginViewModel::class.java]

        viewModel.registerDevice(deviceData)
        viewModel.registerDeviceResponse.observe(this) { deviceResponse ->
            if (deviceResponse.isSuccessful) {
                Log.d("Registered", deviceResponse.body().toString())
            }else{
                Log.d("Registration Error: ", deviceResponse.errorBody().toString())
            }
        }

        lifecycleScope.launch {
            mWebView.addJavascriptInterface(
                SawoWebSDKInterface(
                    ::passPayloadToCallbackActivity,
                    ::authenticateToEncrypt,
                    ::authenticateToDecrypt,
                    sharedPref.getString(SHARED_PREF_DEVICE_ID_KEY, null).toString()
                ),
                "webSDKInterface"
            )

            delay(1000L)
            mWebView.loadUrl(sawoWebSDKURL)
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        mWebView.destroy()
    }


    private fun processCancel() {
        Toast.makeText(
            this, R.string.prompt_cancel_toast, Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private fun passPayloadToCallbackActivity(message: String) {
        val intent = Intent(this, Class.forName(callBackClassName)).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(LOGIN_SUCCESS_MESSAGE, message)
        }
        startActivity(intent)
        finish()
    }

    private fun authenticateToEncrypt(message: String) {
        readyToEncrypt = true
        dataToEncrypt = message
        if (canStoreKeyInStorage) {
            runOnUiThread(Runnable {
                val cipher = cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            })
        }
    }

    private fun authenticateToDecrypt() {
        readyToEncrypt = false
        if (canStoreKeyInStorage && encryptedData != null) {
            runOnUiThread(Runnable {
                encryptedData?.let { encryptedData ->
                    val cipher = cryptographyManager.getInitializedCipherForDecryption(
                        secretKeyName,
                        encryptedData.initializationVector
                    )
                    biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
                }
            })
        }
    }

    private fun processData(cryptoObject: BiometricPrompt.CryptoObject?) {
        if (readyToEncrypt) {
            runOnUiThread(Runnable {
                mWebView.evaluateJavascript(
                    "(function() { window.dispatchEvent(new CustomEvent('keysFromAndroid', {'detail': \'${dataToEncrypt}\'})); })();",
                    null
                )
            })
            val encryptedData =
                cryptographyManager.encryptData(dataToEncrypt, cryptoObject?.cipher!!)
            cryptographyManager.saveEncryptedDataToSharedPrefs(
                encryptedData,
                applicationContext,
                SHARED_PREF_FILENAME,
                Context.MODE_PRIVATE,
                SHARED_PREF_ENC_PAIR_KEY
            )
        } else {
            if (encryptedData != null) {
                encryptedData?.let { encryptedData ->
                    val data = cryptographyManager.decryptData(
                        encryptedData.ciphertext,
                        cryptoObject?.cipher!!
                    )
                    runOnUiThread(Runnable {
                        mWebView.evaluateJavascript(
                            "(function() { window.dispatchEvent(new CustomEvent('keysFromAndroid', {'detail': \'${data}\'})); })();",
                            null
                        )
                    })
                }
            }
        }
    }

    override fun onOSSubscriptionChanged(stateChanges: OSSubscriptionStateChanges) {
        Log.d(TAG, "OSSubscriptionStateChanged, calling registerDevice")
        registerDevice(sharedPref)
    }

}