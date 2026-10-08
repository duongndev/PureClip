package com.pureclip.app.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

object KeyboardUtils {

    /**
     * Hides the soft keyboard for the given [view] in an [activity].
     */
    fun hideKeyboard(activity: Activity, view: View) {
        WindowCompat.getInsetsController(activity.window, view).hide(WindowInsetsCompat.Type.ime())
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Hides the soft keyboard for a given [view].
     */
    fun hideKeyboard(view: View) {
        val context = view.context
        if (context is Activity) {
            hideKeyboard(context, view)
        } else {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}

/**
 * Extension function on [View] to hide the soft keyboard.
 */
fun View.hideKeyboard() {
    KeyboardUtils.hideKeyboard(this)
}

/**
 * Extension function on [Activity] to hide the soft keyboard.
 */
fun Activity.hideKeyboard(view: View? = currentFocus) {
    val targetView = view ?: window.decorView
    KeyboardUtils.hideKeyboard(this, targetView)
}
