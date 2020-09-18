package com.sawolabs.androidsdk

import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface

class SawoWebSDKInterface(private val mContext: Context, private  val callBackClassName: String) {
    @JavascriptInterface
    fun handleOnSuccessCallback(message: String) {
        val intent = Intent(mContext, Class.forName(callBackClassName)).apply {
            putExtra(LOGIN_SUCCESS_MESSAGE, message)
        }
        mContext.startActivity(intent)
    }
}