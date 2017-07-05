package piuk.blockchain.android.ui.pairing_code

import android.graphics.Bitmap

import javax.inject.Inject

import io.reactivex.Observable
import okhttp3.ResponseBody
import piuk.blockchain.android.R
import piuk.blockchain.android.data.datamanagers.AuthDataManager
import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.StringUtils

class PairingCodePresenter : BasePresenter<PairingCodeView>() {

    @Inject lateinit var qrCodeDataManager: QrCodeDataManager
    @Inject lateinit var stringUtils: StringUtils
    @Inject lateinit var payloadDataManager: PayloadDataManager
    @Inject lateinit var authDataManager: AuthDataManager

    init {
        Injector.getInstance().dataManagerComponent.inject(this)
    }

    override fun onViewReady() {
        //no op
    }

    internal val firstStep: String
        get() = String.format(stringUtils.getString(R.string.pairing_code_instruction_1), WEB_WALLET_URL)

    fun generatePairingQr() {
        pairingEncryptionPasswordObservable
                .doOnSubscribe { disposable -> view.showProgressSpinner() }
                .doAfterTerminate { view.hideProgressSpinner() }
                .flatMap { encryptionPassword -> generatePairingCodeObservable(encryptionPassword.string()) }
                .compose(RxUtil.addObservableToCompositeDisposable<Bitmap>(this))
                .subscribe(
                        { bitmap -> view.onQrLoaded(bitmap) }
                ) { throwable -> view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR) }
    }

    private val pairingEncryptionPasswordObservable: Observable<ResponseBody>
        get() {
            val guid = payloadDataManager!!.wallet.guid
            return authDataManager!!.getPairingEncryptionPassword(guid)
        }

    private fun generatePairingCodeObservable(encryptionPhrase: String): Observable<Bitmap> {

        val guid = payloadDataManager!!.wallet.guid
        val sharedKey = payloadDataManager!!.wallet.sharedKey
        val password = payloadDataManager!!.tempPassword

        return qrCodeDataManager!!.generatePairingCode(
                guid,
                password,
                sharedKey,
                encryptionPhrase,
                600)
    }

    companion object {

        private val WEB_WALLET_URL = "blockchain.info/wallet/login"
    }
}
