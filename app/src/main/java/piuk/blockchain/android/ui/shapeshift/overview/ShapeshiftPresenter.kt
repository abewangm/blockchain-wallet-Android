package piuk.blockchain.android.ui.shapeshift.overview

import info.blockchain.wallet.shapeshift.data.Trade
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.util.annotations.Mockable
import timber.log.Timber
import javax.inject.Inject

@Mockable
class ShapeShiftPresenter @Inject constructor(
        private val shapeShiftDataManager: ShapeShiftDataManager,
        private val payloadDataManager: PayloadDataManager
) : BasePresenter<ShapeShiftView>() {

    override fun onViewReady() {

        payloadDataManager.metadataNodeFactory
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .doOnSubscribe { view.onStateUpdated(ShapeShiftState.Loading) }
                .doOnError { view.onStateUpdated(ShapeShiftState.Error) }
                .flatMap { shapeShiftDataManager.initShapeshiftTradeData(it.metadataNode) }
                .subscribe(
                        {
                            if (it.trades.isEmpty()) {
                                view.onStateUpdated(ShapeShiftState.Empty)
                            } else {
                                view.onStateUpdated(ShapeShiftState.Data(it.trades))
                            }
                        },
                        {
                            Timber.e(it)
                            view.onStateUpdated(ShapeShiftState.Error)
                        }
                )
    }

    internal fun onRetryPressed() {
        onViewReady()
    }
}

sealed class ShapeShiftState {

    class Data(val trades: List<Trade>) : ShapeShiftState()
    object Empty : ShapeShiftState()
    object Error : ShapeShiftState()
    object Loading : ShapeShiftState()

}