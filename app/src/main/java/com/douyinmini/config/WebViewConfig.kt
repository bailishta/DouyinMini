package com.douyinmini.config

import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView

object WebViewConfig {

    fun configure(webView: WebView) {
        webView.apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            overScrollMode = WebView.OVER_SCROLL_NEVER

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                setSupportZoom(false)
                loadsImagesAutomatically = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                allowFileAccess = false
                allowContentAccess = false
                setSupportMultipleWindows(true)
                userAgentString = buildUserAgent()
                useWideViewPort = true
                loadWithOverviewMode = true
            }
        }
    }

    private fun buildUserAgent(): String {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
