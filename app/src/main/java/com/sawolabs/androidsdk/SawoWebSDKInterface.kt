package com.sawolabs.androidsdk

import android.webkit.JavascriptInterface

class SawoWebSDKInterface(
    private val passPayload: (String) -> Unit,
    private val authenticateToEncrypt: (String) -> Unit,
    private val authenticateToDecrypt: () -> Unit
) {
    @JavascriptInterface
    fun handleOnSuccessCallback(message: String) {
        passPayload(message)
    }

    @JavascriptInterface
    fun saveKeys(message: String) {
        authenticateToEncrypt(message)
    }

    @JavascriptInterface
    fun getKeys() {
        authenticateToDecrypt()
    }
}