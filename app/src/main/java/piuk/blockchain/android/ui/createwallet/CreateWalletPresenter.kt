package piuk.blockchain.android.ui.createwallet

import android.content.Intent
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.util.PasswordUtil
import piuk.blockchain.android.R
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.recover.RecoverFundsActivity
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.PrefsUtil
import javax.inject.Inject

class CreateWalletPresenter @Inject constructor(
        private val payloadDataManager: PayloadDataManager,
        private val prefsUtil: PrefsUtil,
        private val appUtil: AppUtil
) : BasePresenter<CreateWalletView>() {

    var recoveryPhrase: String = ""
    var passwordStrength = 0

    override fun onViewReady() {
        // No-op
    }

    fun parseExtras(intent: Intent) {
        val mnemonic = intent.getStringExtra(RecoverFundsActivity.RECOVERY_PHRASE)

        if (mnemonic != null) recoveryPhrase = mnemonic

        if (!recoveryPhrase.isEmpty()) {
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
        when {
            !FormatsUtil.isValidEmailAddress(email) -> view.showToast(R.string.invalid_email, ToastCustom.TYPE_ERROR)
            password1.length < 4 -> view.showToast(R.string.invalid_password_too_short, ToastCustom.TYPE_ERROR)
            password1.length > 255 -> view.showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR)
            password1 != password2 -> view.showToast(R.string.password_mismatch_error, ToastCustom.TYPE_ERROR)
            passwordStrength < 50 -> view.showWeakPasswordDialog(email, password1)
            !recoveryPhrase.isEmpty() -> recoverWallet(email, password1)
            else -> createWallet(email, password1)
        }
    }

    fun createWallet(email: String, password: String) {
        appUtil.applyPRNGFixes()

        payloadDataManager.createHdWallet(password, view.getDefaultAccountName(), email)
                .doOnNext {
                    appUtil.isNewlyCreated = true
                    prefsUtil.setValue(PrefsUtil.KEY_GUID, payloadDataManager.wallet.guid)
                    appUtil.sharedKey = payloadDataManager.wallet.sharedKey
                }
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .doOnSubscribe { view.showProgressDialog(R.string.creating_wallet) }
                .doAfterTerminate { view.dismissProgressDialog() }
                .subscribe({
                    prefsUtil.setValue(PrefsUtil.KEY_EMAIL, email)
                    view.startPinEntryActivity()
                }, {
                    view.showToast(R.string.hd_error, ToastCustom.TYPE_ERROR)
                    appUtil.clearCredentialsAndRestart()
                })
    }

    fun recoverWallet(email: String, password: String) {
        payloadDataManager.restoreHdWallet(
                recoveryPhrase,
                view.getDefaultAccountName(),
                email,
                password)
                .doOnNext {
                    appUtil.isNewlyCreated = true
                    prefsUtil.setValue(PrefsUtil.KEY_GUID, payloadDataManager.wallet.guid)
                    appUtil.sharedKey = payloadDataManager.wallet.sharedKey
                }
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .doOnSubscribe { view.showProgressDialog(R.string.restoring_wallet) }
                .doAfterTerminate { view.dismissProgressDialog() }
                .subscribe({
                    prefsUtil.setValue(PrefsUtil.KEY_EMAIL, email)
                    prefsUtil.setValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, true)
                    view.startPinEntryActivity()
                }, { throwable ->
                    throwable.printStackTrace()
                    view.showToast(R.string.restore_failed, ToastCustom.TYPE_ERROR)
                })
    }

}
