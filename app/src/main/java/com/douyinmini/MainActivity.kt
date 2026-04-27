package com.douyinmini

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.douyinmini.config.WebViewConfig
import com.douyinmini.ui.ErrorOverlayView
import com.douyinmini.ui.FullscreenVideoLayout
import com.douyinmini.util.NetworkMonitor
import com.douyinmini.util.ThemeUtils
import androidx.activity.addCallback
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var fullscreenContainer: FullscreenVideoLayout
    private lateinit var errorOverlay: ErrorOverlayView
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getPreferences(Context.MODE_PRIVATE)
        val savedDarkMode = prefs.getBoolean(KEY_DARK_MODE, false)
        AppCompatDelegate.setDefaultNightMode(
            if (savedDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        configureWebView()
        setupNetworkMonitor()
        setupBackPressHandler()
        loadDouyin()
    }

    private fun initViews() {
        webView = findViewById(R.id.webview)
        progressBar = findViewById(R.id.progress_bar)
        fullscreenContainer = findViewById(R.id.fullscreen_container)
        errorOverlay = findViewById(R.id.error_overlay)
        errorOverlay.setOnRetryListener {
            if (webView.url.isNullOrBlank()) {
                loadDouyin()
            } else {
                webView.reload()
            }
        }
    }

    private fun configureWebView() {
        WebViewConfig.configure(webView)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar.visibility = View.VISIBLE
                errorOverlay.hide()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    errorOverlay.show()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                }
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view != null && callback != null) {
                    fullscreenContainer.showFullscreen(view, callback)
                }
            }

            override fun onHideCustomView() {
                fullscreenContainer.hideFullscreen()
            }
        }

        ThemeUtils.applySystemTheme(webView)
    }

    private fun setupNetworkMonitor() {
        networkMonitor = NetworkMonitor(this)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkMonitor.isOnline.collect { online ->
                    if (!online) {
                        errorOverlay.show()
                    } else {
                        errorOverlay.hide()
                        if (webView.url.isNullOrBlank()) {
                            loadDouyin()
                        }
                    }
                }
            }
        }
    }

    private fun loadDouyin() {
        webView.loadUrl(DOUYIN_URL)
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this) {
            when {
                fullscreenContainer.isFullscreen() -> fullscreenContainer.hideFullscreen()
                webView.canGoBack() -> webView.goBack()
                else -> finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.resumeTimers()
    }

    override fun onPause() {
        webView.onPause()
        webView.pauseTimers()
        super.onPause()
    }

    override fun onDestroy() {
        networkMonitor.unregister()
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val DOUYIN_URL = "https://www.douyin.com"
        private const val KEY_DARK_MODE = "dark_mode"
    }
}
