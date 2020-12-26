package com.sawolabs.androidsdk

import android.webkit.JavascriptInterface

class SawoWebSDKInterface(
    private val passPayload: (String) -> Unit,
    private val authenticateToEncrypt: (String) -> Unit,
) {
    @JavascriptInterface
    fun handleOnSuccessCallback(message: String) {
        passPayload(message)
    }

    @JavascriptInterface
    fun saveKeysAndSessionId(message: String) {
        authenticateToEncrypt(message)
    }
}