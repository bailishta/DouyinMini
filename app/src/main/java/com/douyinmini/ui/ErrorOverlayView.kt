package com.douyinmini.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.douyinmini.R

class ErrorOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var retryCallback: (() -> Unit)? = null

    init {
        View.inflate(context, R.layout.view_error_overlay, this)
    }

    fun setOnRetryListener(callback: () -> Unit) {
        retryCallback = callback
        findViewById<Button>(R.id.btn_retry).setOnClickListener {
            hide()
            retryCallback?.invoke()
        }
    }

    fun show() {
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }
}
