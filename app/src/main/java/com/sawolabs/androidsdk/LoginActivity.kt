package com.sawolabs.androidsdk

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient

class LoginActivity : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val mWebView: WebView = findViewById(R.id.webview)
        val sawoWebSDKURL = intent.getStringExtra(SAWO_WEBSDK_URL)
        val callBackClassName = intent.getStringExtra(CALLBACK_CLASS)
        mWebView.loadUrl(sawoWebSDKURL)
        mWebView.settings.javaScriptEnabled = true
        mWebView.settings.domStorageEnabled = true
        mWebView.settings.databaseEnabled = true
        mWebView.webViewClient = WebViewClient()
        mWebView.addJavascriptInterface(SawoWebSDKInterface(this, callBackClassName), "webSDKInterface")
    }
}