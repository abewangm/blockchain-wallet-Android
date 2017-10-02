package piuk.blockchain.android.ui.fingerprint

import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import piuk.blockchain.android.ui.base.View

interface FingerprintView : View {

    fun getBundle(): Bundle?

    fun setCancelButtonText(@StringRes text: Int)

    fun setDescriptionText(@StringRes text: Int)

    fun setStatusText(@StringRes text: Int)

    fun setStatusText(text: String)

    fun setStatusTextColor(@ColorRes color: Int)

    fun setIcon(@DrawableRes drawable: Int)

    fun onFatalError()

    fun onAuthenticated(data: String?)

    fun onRecoverableError()

    fun onCanceled()
}