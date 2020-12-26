package com.sawolabs.androidsdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt

class LoginActivity : AppCompatActivity() {
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var cryptographyManager: CryptographyManager
    private lateinit var mWebView: WebView
    private lateinit var dataToEncrypt: String
    private lateinit var callBackClassName: String
    private lateinit var sawoWebSDKURL: String
    private val encryptedData get() = cryptographyManager.getEncryptedDataFromSharedPrefs(
        applicationContext,
        SHARED_PREF_FILENAME,
        Context.MODE_PRIVATE,
        SHARED_PREF_ENC_PAIR_KEY
    )
    private var readyToEncrypt: Boolean = false
    private val secretKeyName = "SAWO_BIOMETRIC_ENCRYPTION_KEY"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        sawoWebSDKURL = intent.getStringExtra(SAWO_WEBSDK_URL)
        callBackClassName = intent.getStringExtra(CALLBACK_CLASS)
        cryptographyManager = CryptographyManager()
        biometricPrompt = BiometricPromptUtils.createBiometricPrompt(this, ::processData)
        promptInfo = BiometricPromptUtils.createPromptInfo(this)
        mWebView = findViewById(R.id.webview)
        mWebView.loadUrl(sawoWebSDKURL)
        mWebView.settings.javaScriptEnabled = true
        mWebView.settings.domStorageEnabled = true
        mWebView.settings.databaseEnabled = true
        mWebView.webViewClient = WebViewClient()
        mWebView.addJavascriptInterface(
            SawoWebSDKInterface(::passPayloadToCallbackActivity, ::authenticateToEncrypt),
            "webSDKInterface"
        )
    }

    override fun onPause() {
        super.onPause()
        Log.d("LoginActivity", "paused")
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
        if (BiometricManager.from(applicationContext).canAuthenticate() == BiometricManager
                .BIOMETRIC_SUCCESS) {
            val cipher = cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun authenticateToDecrypt() {
        readyToEncrypt = false
        if (BiometricManager.from(applicationContext).canAuthenticate() == BiometricManager
                .BIOMETRIC_SUCCESS && encryptedData != null) {
            encryptedData?.let { encryptedData ->  val cipher = cryptographyManager.getInitializedCipherForDecryption(secretKeyName, encryptedData.initializationVector)
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))}
        }
    }

    private fun processData(cryptoObject: BiometricPrompt.CryptoObject?) {
        if (readyToEncrypt) {
            val encryptedData = cryptographyManager.encryptData(dataToEncrypt, cryptoObject?.cipher!!)
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
                    val data = cryptographyManager.decryptData(encryptedData.ciphertext, cryptoObject?.cipher!!)
                    mWebView.evaluateJavascript("javascript: updateFromNative(\"${data}\")",null)
                }
            }
        }
    }
}