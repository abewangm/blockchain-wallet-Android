package piuk.blockchain.android.ui.login

import piuk.blockchain.android.R
import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.PrefsUtil
import javax.inject.Inject
import javax.net.ssl.SSLPeerUnverifiedException

class LoginPresenter : BasePresenter<LoginView>() {

    @Inject lateinit var appUtil: AppUtil
    @Inject lateinit var payloadDataManager: PayloadDataManager
    @Inject lateinit var prefsUtil: PrefsUtil

    init {
        Injector.getInstance().dataManagerComponent.inject(this)
    }

    override fun onViewReady() {
    }

    fun pairWithQR(raw: String?) {
        appUtil.clearCredentials()

        payloadDataManager.handleQrCode(raw)
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doOnSubscribe({ view.showProgressDialog(R.string.please_wait) })
                .doOnComplete({ appUtil.sharedKey = payloadDataManager.wallet.sharedKey })
                .doAfterTerminate({ view.dismissProgressDialog() })
                .subscribe({
                    prefsUtil.setValue(PrefsUtil.KEY_GUID, payloadDataManager.wallet.guid)
                    prefsUtil.setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true)
                    prefsUtil.setValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, true)
                    view.startPinEntryActivity()
                }, { throwable ->
                    if (throwable is SSLPeerUnverifiedException) {
                        // BaseActivity handles message
                        appUtil.clearCredentials()
                    } else {
                        view.showToast(R.string.pairing_failed, ToastCustom.TYPE_ERROR)
                        appUtil.clearCredentialsAndRestart()
                    }
                })


    }
}