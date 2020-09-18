package com.sawolabs.androidsdk

import android.content.Context
import android.content.Intent

class Sawo(private val mContext:Context, private val apiKey: String) {
    fun login(identifierType: String, callBackClass: String) {
        val sawoWebSDKURL = "http://192.168.0.105:3000/?apiKey=$apiKey&identifierType=$identifierType"
        val intent = Intent(mContext, LoginActivity::class.java).apply {
            putExtra(SAWO_WEBSDK_URL, sawoWebSDKURL)
            putExtra(CALLBACK_CLASS, callBackClass)
        }
        mContext.startActivity(intent)
    }
}