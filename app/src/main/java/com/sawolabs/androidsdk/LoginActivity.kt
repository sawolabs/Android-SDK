package com.sawolabs.androidsdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
    private var keyExistInStorage: Boolean = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        sawoWebSDKURL = intent.getStringExtra(SAWO_WEBSDK_URL)
        callBackClassName = intent.getStringExtra(CALLBACK_CLASS)
        cryptographyManager = CryptographyManager()
        biometricPrompt = BiometricPromptUtils.createBiometricPrompt(
            this, ::processCancel, ::processData)
        promptInfo = BiometricPromptUtils.createPromptInfo(this)
        mWebView = findViewById(R.id.webview)
        keyExistInStorage = cryptographyManager.isDataExistInSharedPrefs(
            this, SHARED_PREF_FILENAME, Context.MODE_PRIVATE, SHARED_PREF_ENC_PAIR_KEY)
        sawoWebSDKURL += "&keysExistInStorage=${keyExistInStorage}"
        mWebView.loadUrl(sawoWebSDKURL)
        mWebView.settings.javaScriptEnabled = true
        mWebView.settings.domStorageEnabled = true
        mWebView.settings.databaseEnabled = true
        mWebView.webViewClient = WebViewClient()
        mWebView.addJavascriptInterface(
            SawoWebSDKInterface(
                ::passPayloadToCallbackActivity, ::authenticateToEncrypt, ::authenticateToDecrypt),
            "webSDKInterface"
        )
    }

    private fun processCancel() {
        Toast.makeText(
            this, R.string.prompt_cancel_toast, Toast.LENGTH_LONG).show()
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
        if (BiometricManager.from(applicationContext).canAuthenticate() == BiometricManager
                .BIOMETRIC_SUCCESS) {
            runOnUiThread(Runnable {
                val cipher = cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            })
        }
    }

    private fun authenticateToDecrypt() {
        readyToEncrypt = false
        if (BiometricManager.from(applicationContext).canAuthenticate() == BiometricManager
                .BIOMETRIC_SUCCESS && encryptedData != null) {
            runOnUiThread(Runnable {
                encryptedData?.let { encryptedData ->  val cipher = cryptographyManager.getInitializedCipherForDecryption(secretKeyName, encryptedData.initializationVector)
                    biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))}
            })
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
                    runOnUiThread(Runnable { mWebView.evaluateJavascript(
                        "(function() { window.dispatchEvent(new CustomEvent('keysFromAndroid', {'detail': \'${data}\'})); })();", null
                    )})
                }
            }
        }
    }
}