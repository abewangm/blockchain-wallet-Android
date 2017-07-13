package piuk.blockchain.android.ui.createwallet

import android.content.Intent
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.util.PasswordUtil
import piuk.blockchain.android.R
import piuk.blockchain.android.data.datamanagers.AuthDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.recover.RecoverFundsActivity
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import javax.inject.Inject

class CreateWalletPresenter : BasePresenter<CreateWalletView>() {

    var recoveryPhrase: String = ""
    var passwordStrength = 0

    @Inject lateinit var authDataManager: AuthDataManager
    @Inject lateinit var prefsUtil: PrefsUtil
    @Inject lateinit var appUtil: AppUtil
    @Inject lateinit var stringUtils: StringUtils

    init {
        Injector.getInstance().dataManagerComponent.inject(this)
    }

    override fun onViewReady() {

    }

    fun parseExtras(intent: Intent) {

        val meh = intent.getStringExtra(RecoverFundsActivity.RECOVERY_PHRASE)

        if(meh != null)recoveryPhrase = meh

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
        } else if (!recoveryPhrase.isEmpty()) {
            recoverWallet(email, password1)
        } else {
            createWallet(email, password1)
        }
    }

    fun createWallet(email: String, password: String) {
        appUtil.applyPRNGFixes()

        authDataManager.createHdWallet(password, view.getDefaultAccountName(), email)
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .doOnSubscribe({ view.showProgressDialog(R.string.creating_wallet) })
                .doAfterTerminate({ view.dismissProgressDialog() })
                .subscribe({
                    prefsUtil.setValue(PrefsUtil.KEY_EMAIL, email)
                    view.startPinEntryActivity()
                }, { throwable ->
                    view.showToast(R.string.hd_error, ToastCustom.TYPE_ERROR)
                    appUtil.clearCredentialsAndRestart()
                })
    }

    fun recoverWallet(email: String, password: String) {
        authDataManager.restoreHdWallet(email, password, recoveryPhrase)
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .doOnSubscribe({ view.showProgressDialog(R.string.restoring_wallet) })
                .doAfterTerminate({
                    view.dismissProgressDialog()
                })
                .subscribe({
                    prefsUtil.setValue(PrefsUtil.KEY_EMAIL, email)
                    prefsUtil.setValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, true)
                    view.startPinEntryActivity()
                }, { throwable ->
                    throwable.printStackTrace()
                    view.showToast(R.string.restore_failed, ToastCustom.TYPE_ERROR)
                });

    }
}
