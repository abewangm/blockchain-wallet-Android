package piuk.blockchain.android.ui.swipetoreceive

import io.reactivex.Single
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.base.UiState
import javax.inject.Inject

class SwipeToReceivePresenter @Inject constructor(
        private val dataManager: QrCodeDataManager,
        private val swipeToReceiveHelper: SwipeToReceiveHelper
) : BasePresenter<SwipeToReceiveView>() {

    private val bitcoinAddress: Single<String>
        get() = swipeToReceiveHelper.getNextAvailableBitcoinAddressSingle()
    private val ethereumAddress: Single<String>
        get() = swipeToReceiveHelper.getEthReceiveAddressSingle()

    override fun onViewReady() {
        view.setUiState(UiState.LOADING)

        val accountName: String
        val single: Single<String>
        val hasAddresses: Boolean

        if (view.cryptoCurrency == CryptoCurrencies.BTC) {
            accountName = swipeToReceiveHelper.getBitcoinAccountName()
            single = bitcoinAddress
            hasAddresses = !swipeToReceiveHelper.getBitcoinReceiveAddresses().isEmpty()
        } else {
            accountName = swipeToReceiveHelper.getEthAccountName()
            single = ethereumAddress
            hasAddresses = !swipeToReceiveHelper.getEthReceiveAddress().isNullOrEmpty()
        }

        // Check we actually have addresses stored
        if (!hasAddresses) {
            view.setUiState(UiState.EMPTY)
        } else {
            view.displayReceiveAccount(accountName)

            single.doOnSuccess { require(it.isNotEmpty()) { "Returned address is empty, no more addresses available" } }
                    .doOnSuccess { view.displayReceiveAddress(it) }
                    .flatMapObservable { dataManager.generateQrCode(it, DIMENSION_QR_CODE) }
                    .compose(RxUtil.addObservableToCompositeDisposable(this))
                    .subscribe(
                            {
                                view.displayQrCode(it)
                                view.setUiState(UiState.CONTENT)
                            },
                            { _ -> view.setUiState(UiState.FAILURE) })
        }
    }

    companion object {

        private const val DIMENSION_QR_CODE = 600

    }

}
