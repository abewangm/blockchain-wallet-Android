package piuk.blockchain.android.ui.shapeshift.overview

import info.blockchain.wallet.shapeshift.data.Trade
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.util.annotations.Mockable
import timber.log.Timber
import java.util.concurrent.TimeUnit
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
                                pollForStatus(it.trades)
                                view.onStateUpdated(ShapeShiftState.Data(it.trades))
                            }
                        },
                        {
                            Timber.e(it)
                            view.onStateUpdated(ShapeShiftState.Error)
                        }
                )
    }

    private fun pollForStatus(trades: List<Trade>) {

        Observable.fromIterable(trades)
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .flatMap { trade -> createPollObservable(trade) }
                .subscribe(
                        {
                            //no-op
                        },
                        {
                            Timber.e(it)
                        })
    }

    private fun createPollObservable(trade: Trade) =
        Observable.interval(2, TimeUnit.SECONDS, Schedulers.io())
                .flatMap { shapeShiftDataManager.getTradeStatus(trade.quote.deposit) }
                .doOnNext { handleState(trade, it) }
                .takeUntil { isInFinalState(it.status) }

    private fun handleState(trade: Trade, tradeStatus: TradeStatusResponse) {
        Timber.d("vos handleState tradeStatus: " + tradeStatus.toJson())
        Timber.d("vos handleState trade: " + trade.toJson())
    }

    internal fun onRetryPressed() {
        onViewReady()
    }

    private fun isInFinalState(status: Trade.STATUS) = when (status) {
        Trade.STATUS.NO_DEPOSITS, Trade.STATUS.RECEIVED -> false
        Trade.STATUS.COMPLETE, Trade.STATUS.FAILED, Trade.STATUS.RESOLVED -> true
        else -> true
    }
}

sealed class ShapeShiftState {

    class Data(val trades: List<Trade>) : ShapeShiftState()
    object Empty : ShapeShiftState()
    object Error : ShapeShiftState()
    object Loading : ShapeShiftState()

}