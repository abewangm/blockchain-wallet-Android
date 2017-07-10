package piuk.blockchain.android.ui.auth

import android.support.annotation.StringRes
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.customviews.ToastCustom

interface CredentialsView : View {

    fun setTitleText(text : Int)

    fun setNextText(text : Int)

    fun setEntropyStrength(score: Int)

    fun setEntropyLevel(level: Int)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun showWeakPasswordDialog(email: String, password: String)

    fun startNextActivity(email: String, password: String)
}
