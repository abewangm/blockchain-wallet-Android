package piuk.blockchain.android.ui.swipetoreceive

import io.reactivex.exceptions.Exceptions
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.base.UiState
import javax.inject.Inject


class SwipeToReceivePresenter : BasePresenter<SwipeToReceiveView>() {

    @Inject lateinit var dataManager: QrCodeDataManager
    @Inject lateinit var swipeToReceiveHelper: SwipeToReceiveHelper

    init {
        Injector.getInstance().dataManagerComponent.inject(this)
    }

    override fun onViewReady() {
        view.setUiState(UiState.LOADING)

        // Check we actually have addresses stored
        if (swipeToReceiveHelper.getReceiveAddresses().isEmpty()) {
            view.setUiState(UiState.EMPTY)
        } else {
            view.displayReceiveAccount(swipeToReceiveHelper.getAccountName())

            swipeToReceiveHelper.getNextAvailableAddress()
                    .doOnNext { s ->
                        if (s.isEmpty()) {
                            throw Exceptions.propagate(Throwable(
                                    "Returned address is empty, no more addresses available"))
                        }
                    }
                    .doOnNext { address -> view.displayReceiveAddress(address) }
                    .flatMap { address -> dataManager.generateQrCode(address, DIMENSION_QR_CODE) }
                    .compose(RxUtil.addObservableToCompositeDisposable(this))
                    .subscribe(
                            { bitmap ->
                                view.displayQrCode(bitmap)
                                view.setUiState(UiState.CONTENT)
                            },
                            { _ -> view.setUiState(UiState.FAILURE) })
        }
    }

    companion object {
        private val DIMENSION_QR_CODE = 600
    }

}
