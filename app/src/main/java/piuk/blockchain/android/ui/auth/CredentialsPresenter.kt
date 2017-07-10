package piuk.blockchain.android.ui.auth

import android.content.Intent
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.util.PasswordUtil
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.customviews.ToastCustom

class CredentialsPresenter : BasePresenter<CredentialsView>() {

    var recoveringFunds = false
    var passwordStrength = 0

    override fun onViewReady() {

    }

    fun parseExtras(intent: Intent) {
        recoveringFunds = intent.getBooleanExtra(LandingActivity.KEY_INTENT_RECOVERING_FUNDS, false)
        if (recoveringFunds) {
            view.setTitleText(R.string.recover_funds)
            view.setNextText(R.string.dialog_continue)
        } else {
            view.setTitleText(R.string.new_wallet)
            view.setNextText(R.string.create_wallet)
        }
    }

    fun calculateEntropy(password: String) {
        passwordStrength = Math.round(PasswordUtil.getStrength(password)).toInt()
        view.setEntropyStrength(passwordStrength)

        when (passwordStrength) {
            in 0..25 -> view.setEntropyLevel(0)
            in 26..50 -> view.setEntropyLevel(1)
            in 51..75 -> view.setEntropyLevel(2)
            in 76..100 -> view.setEntropyLevel(3)
        }
    }

    fun validateCredentials(email: String, password1: String, password2: String) {

        if (!FormatsUtil.isValidEmailAddress(email)) {
            view.showToast(R.string.invalid_email, ToastCustom.TYPE_ERROR)
        } else if (password1.length < 4) {
            view.showToast(R.string.invalid_password_too_short, ToastCustom.TYPE_ERROR)
        } else if (password1.length > 255) {
            view.showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR)
        } else if (password1 != password2) {
            view.showToast(R.string.password_mismatch_error, ToastCustom.TYPE_ERROR)
        } else if (passwordStrength < 50) {
            view.showWeakPasswordDialog(email, password1)
        } else {
            view.startNextActivity(email, password1)
        }
    }
}
