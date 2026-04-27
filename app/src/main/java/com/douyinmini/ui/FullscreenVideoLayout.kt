package com.douyinmini.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.View
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.douyinmini.R

class FullscreenVideoLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var customView: View? = null
    private var callback: WebChromeClient.CustomViewCallback? = null

    fun showFullscreen(view: View, cb: WebChromeClient.CustomViewCallback) {
        customView?.let { hideFullscreen() }

        customView = view
        callback = cb

        val container = findViewById<FrameLayout>(R.id.video_container)
        container.addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        container.visibility = View.VISIBLE

        val activity = context as? Activity ?: return
        hideSystemBars(activity, true)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }

    fun hideFullscreen() {
        val container = findViewById<FrameLayout>(R.id.video_container)
        customView?.let { container.removeView(it) }
        customView = null
        callback?.onCustomViewHidden()
        callback = null
        container.visibility = View.GONE

        val activity = context as? Activity ?: return
        hideSystemBars(activity, false)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun hideSystemBars(activity: Activity, hide: Boolean) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (hide) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun isFullscreen(): Boolean {
        return findViewById<FrameLayout>(R.id.video_container).visibility == View.VISIBLE
    }
}
