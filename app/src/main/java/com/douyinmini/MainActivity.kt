package com.douyinmini

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                Log.d(TAG, "导航URL: $url")
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                Log.d(TAG, "页面开始加载: $url")
                progressBar.visibility = View.VISIBLE
                errorOverlay.hide()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                injectLayoutFix(view, url)
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                injectLayoutFix(view, url)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    val code = error?.errorCode ?: -1
                    val desc = error?.description?.toString() ?: "未知错误"
                    Log.w(TAG, "页面加载错误 code=$code desc=$desc url=${request.url}")
                    errorOverlay.show()
                } else if (error?.errorCode == ERROR_HOST_LOOKUP ||
                    error?.errorCode == ERROR_CONNECT ||
                    error?.errorCode == ERROR_TIMEOUT) {
                    Log.w(TAG, "子资源加载失败 code=${error.errorCode} url=${request?.url}")
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                val statusCode = errorResponse?.statusCode ?: 0
                if (request?.isForMainFrame == true && statusCode >= 400) {
                    Log.w(TAG, "HTTP错误 status=$statusCode url=${request.url}")
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
                try {
                    if (view != null && callback != null) {
                        fullscreenContainer.showFullscreen(view, callback)
                    } else if (view != null && callback == null) {
                        Log.w(TAG, "onShowCustomView: callback为null，直播全屏可能异常")
                        fullscreenContainer.showFullscreen(view, object : CustomViewCallback {
                            override fun onCustomViewHidden() {}
                        })
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onShowCustomView异常: ${e.message}", e)
                }
            }

            override fun onHideCustomView() {
                try {
                    fullscreenContainer.hideFullscreen()
                } catch (e: Exception) {
                    Log.e(TAG, "onHideCustomView异常: ${e.message}", e)
                }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                if (consoleMessage != null &&
                    (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR ||
                     consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.WARNING)) {
                    Log.w(TAG, "Console[${consoleMessage.messageLevel()}] line=${consoleMessage.lineNumber()}: ${consoleMessage.message()}")
                }
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request != null) {
                    val resources = request.resources
                    for (resource in resources) {
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE == resource ||
                            PermissionRequest.RESOURCE_VIDEO_CAPTURE == resource) {
                            Log.w(TAG, "直播请求媒体权限，已自动授权")
                        }
                    }
                    request.grant(request.resources)
                }
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                return false
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

    private fun injectLayoutFix(view: WebView?, url: String?) {
        if (url.isNullOrBlank()) return
        val isLivePage = url.contains("live.douyin.com") ||
                url.contains("/live/") ||
                url.contains("webcast")
        if (!isLivePage) return
        view?.evaluateJavascript("""
            (function fixLayout() {
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) {
                    meta = document.createElement('meta');
                    meta.name = 'viewport';
                    document.head.appendChild(meta);
                }
                meta.content = 'width=' + window.screen.width + ', initial-scale=1.0, maximum-scale=1.0, user-scalable=no';
                if (!document.getElementById('_dv_fix')) {
                    var style = document.createElement('style');
                    style.id = '_dv_fix';
                    style.textContent = [
                        'html,body{overflow-x:hidden!important}',
                        '#douyin-navigation{display:none!important}',
                        '#_douyin_live_scroll_container_{left:0!important;margin-left:0!important;width:100vw!important}',
                        '[class*="live-chat"],[class*="LiveChat"],[class*="chatRoom"],[class*="ChatRoom"]{display:none!important}',
                        '[class*="liveRight"],[class*="LiveRight"],[class*=rightPanel],[class*=RightPanel]{display:none!important}',
                        '.webcast-chat-room{display:none!important}',
                        'video{max-width:100vw!important}',
                        '[class*=playerContainer]{width:100%!important;left:0!important}'
                    ].join(' ');
                    document.head.appendChild(style);
                }
                // Move the content area to start from left edge
                var scrollCt = document.getElementById('_douyin_live_scroll_container_');
                if (scrollCt) {
                    scrollCt.style.left = '0';
                    scrollCt.style.marginLeft = '0';
                    scrollCt.style.width = window.innerWidth + 'px';
                }
                var nav = document.getElementById('douyin-navigation');
                if (nav) { nav.style.display = 'none'; }
                // Hide chat panels
                var chatEls = document.querySelectorAll('[class*="chat"], [class*="Chat"], [class*="rightPanel"], [class*="RightPanel"], [class*="sidebar"], [class*="Sidebar"]');
                for (var i = 0; i < chatEls.length; i++) {
                    var el = chatEls[i];
                    var rect = el.getBoundingClientRect();
                    if (rect.width > 100 && rect.width < window.innerWidth * 0.6 && rect.x > window.innerWidth * 0.5) {
                        el.style.display = 'none';
                    }
                }
                setTimeout(fixLayout, 3000);
            })();
        """.trimIndent(), null)
    }

    private fun loadDouyin() {
        val url = intent?.data?.toString() ?: DOUYIN_URL
        webView.loadUrl(url)
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
        private const val TAG = "DouyinMini"
        private const val DOUYIN_URL = "https://www.douyin.com"
        private const val KEY_DARK_MODE = "dark_mode"
    }
}
