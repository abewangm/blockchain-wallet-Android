@file:JvmName("ContextExtensions")

package piuk.blockchain.android.util.extensions

import android.content.Context
import android.support.annotation.StringRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import piuk.blockchain.android.ui.customviews.ToastCustom

/**
 * Shows a [ToastCustom] from a given [Context]. By default, the Toast is of type
 * [ToastCustom.TYPE_GENERAL] but can be overloaded if needed.
 *
 * @param text The text to display, as a [String]
 * @param type An optional [ToastCustom.ToastType] which can be omitted for general Toasts
 */
fun Context.toast(text: String, @ToastCustom.ToastType type: String = ToastCustom.TYPE_GENERAL) {
    ToastCustom.makeText(this, text, ToastCustom.LENGTH_SHORT, type)
}

/**
 * Shows a [ToastCustom] from a given [Context]. By default, the Toast is of type
 * [ToastCustom.TYPE_GENERAL] but can be overloaded if needed.
 *
 * @param text The text to display, as a String resource [Int]
 * @param type An optional [ToastCustom.ToastType] which can be omitted for general Toasts
 */
fun Context.toast(@StringRes text: Int, @ToastCustom.ToastType type: String = ToastCustom.TYPE_GENERAL) {
    ToastCustom.makeText(this, getString(text), ToastCustom.LENGTH_SHORT, type)
}