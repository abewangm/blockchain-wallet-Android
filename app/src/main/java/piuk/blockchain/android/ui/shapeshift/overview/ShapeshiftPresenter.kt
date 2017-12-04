package piuk.blockchain.android.ui.shapeshift.overview

import info.blockchain.wallet.shapeshift.data.Trade
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse
import io.reactivex.Observable
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.annotations.Mockable
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Mockable
class ShapeShiftPresenter @Inject constructor(
        private val shapeShiftDataManager: ShapeShiftDataManager,
        private val payloadDataManager: PayloadDataManager,
        private val prefsUtil: PrefsUtil,
        private val exchangeRateFactory: ExchangeRateFactory,
        private val currencyState: CurrencyState,
        private val walletOptionsDataManager: WalletOptionsDataManager
) : BasePresenter<ShapeShiftView>() {

    private val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(getBtcUnitType()) }

    override fun onViewReady() {

        if (walletOptionsDataManager.isAmericanStateSelectionRequired()) {
            view.showStateSelection()
        } else {
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
                                    val sortedTrades = it.trades.sortedWith(compareBy<Trade> { it.timestamp })
                                            .reversed()
                                            // TODO: Remove me when BCH added otherwise you won't see any transactions
                                            .filterNot { it.quote?.pair?.contains("bch", ignoreCase = true) ?: false }
                                    view.onStateUpdated(ShapeShiftState.Data(sortedTrades))
                                }
                            },
                            {
                                Timber.e(it)
                                view.onStateUpdated(ShapeShiftState.Error)
                            }
                    )
        }
    }

    internal fun onResume() {
        // Here we check the Fiat and Btc formats and let the UI handle any potential updates
        val btcUnitType = getBtcUnitType()
        monetaryUtil.updateUnit(btcUnitType)
        view.onExchangeRateUpdated(
                getLastBtcPrice(getFiatCurrency()),
                getLastEthPrice(getFiatCurrency()),
                currencyState.isDisplayingCryptoCurrency
        )
        view.onViewTypeChanged(currencyState.isDisplayingCryptoCurrency, btcUnitType)
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

    private fun createPollObservable(trade: Trade): Observable<TradeStatusResponse> {

        return shapeShiftDataManager.getTradeStatus(trade.quote.deposit)
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .repeatWhen { it.delay(10, TimeUnit.SECONDS) }
                .takeUntil { isInFinalState(it.status) }
                .doOnNext { handleState(trade, it) }
    }

    /**
     * Update kv-store if need. Handle UI update
     */
    private fun handleState(trade: Trade, tradeResponse: TradeStatusResponse) {

        if (trade.status != tradeResponse.status) {
            trade.status = tradeResponse.status
            trade.hashOut = tradeResponse.transaction

            updateMetadata(trade)
        }

        if (tradeResponse.incomingType.equals("bch", true)
                ||tradeResponse.outgoingType.equals("bch", true)) {
            // Remove trade
            // TODO: Remove trade
            // TODO: This page needs a complete rethink otherwise this will be terrible  
//            view.removeTrade(tradeResponse)
        } else {
            view.onTradeUpdate(trade, tradeResponse)
        }

    }

    fun updateMetadata(trade: Trade) {
        shapeShiftDataManager.updateTrade(trade)
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .subscribe(
                        { Timber.d("Update metadata entry complete") },
                        { Timber.e(it) }
                )
    }

    internal fun onRetryPressed() {
        onViewReady()
    }

    private fun isInFinalState(status: Trade.STATUS) = when (status) {
        Trade.STATUS.NO_DEPOSITS, Trade.STATUS.RECEIVED -> false
        Trade.STATUS.COMPLETE, Trade.STATUS.FAILED, Trade.STATUS.RESOLVED -> true
        else -> true
    }

    internal fun setViewType(isBtc: Boolean) {
        currencyState.isDisplayingCryptoCurrency = isBtc
        view.onViewTypeChanged(isBtc, getBtcUnitType())
    }

    private fun getLastBtcPrice(fiat: String) = exchangeRateFactory.getLastBtcPrice(fiat)

    private fun getLastEthPrice(fiat: String) = exchangeRateFactory.getLastEthPrice(fiat)

    private fun getBtcDisplayUnits() = monetaryUtil.getBtcUnits()[getBtcUnitType()]

    private fun getBtcUnitType() =
            prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)

    private fun getFiatCurrency() =
            prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
}

sealed class ShapeShiftState {

    class Data(val trades: List<Trade>) : ShapeShiftState()
    object Empty : ShapeShiftState()
    object Error : ShapeShiftState()
    object Loading : ShapeShiftState()

}