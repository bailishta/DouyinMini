package com.douyinmini.util

import android.content.res.Configuration
import android.os.Build
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat

object ThemeUtils {

    @Suppress("DEPRECATION")
    fun applyDarkMode(webView: WebView, isDark: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WebSettingsCompat.setForceDark(
                webView.settings,
                if (isDark) WebSettingsCompat.FORCE_DARK_ON
                else WebSettingsCompat.FORCE_DARK_OFF
            )
        }
    }

    fun isSystemDarkMode(): Boolean {
        val mode = android.content.res.Resources.getSystem().configuration.uiMode
            .and(Configuration.UI_MODE_NIGHT_MASK)
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    fun applySystemTheme(webView: WebView) {
        applyDarkMode(webView, isSystemDarkMode())
    }
}
