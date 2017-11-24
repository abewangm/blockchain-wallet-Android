package piuk.blockchain.android.ui.shapeshift.detail

import info.blockchain.wallet.shapeshift.ShapeShiftPairs
import info.blockchain.wallet.shapeshift.data.Trade
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.shapeshift.models.TradeDetailUiState
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.annotations.Mockable
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Mockable
class ShapeShiftDetailPresenter @Inject constructor(
        private val shapeShiftDataManager: ShapeShiftDataManager,
        private val stringUtils: StringUtils
) : BasePresenter<ShapeShiftDetailView>() {

    private val decimalFormat by unsafeLazy {
        DecimalFormat().apply {
            minimumIntegerDigits = 1
            minimumFractionDigits = 1
            maximumFractionDigits = 8
        }
    }

    override fun onViewReady() {
        // Poll for results
        shapeShiftDataManager.findTrade(view.depositAddress)
                .doOnNext { updateUiAmounts(it) }
                .doOnError {
                    view.showToast(R.string.shapeshift_trade_not_found, ToastCustom.TYPE_ERROR)
                    view.finishPage()
                }
                .flatMap { Observable.interval(5, TimeUnit.SECONDS, Schedulers.io()) }

                // TODO: Render UI 
                // TODO: Show loading in progress  

                .flatMap { shapeShiftDataManager.getTradeStatus(view.depositAddress) }
                .doOnNext { handleState(it.status) }
                .takeUntil { isInFinalState(it.status) }
                .compose(RxUtil.applySchedulersToObservable())
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(
                        {
                            // Doesn't particularly matter if completion is interrupted here
                            with(it) {
                                updateMetadata(address, transaction, status)
                            }
                        },
                        {
                            Timber.e(it)
                        },
                        {
                            Timber.d("On Complete")
                        }
                )
    }

    private fun updateUiAmounts(trade: Trade) {
        with(trade) {
            val (to, from) = getToFromPair(quote.pair)
            updateDeposit(from, quote.depositAmount)
            updateDeposit(from, quote.depositAmount)
            updateReceive(to, quote.withdrawalAmount)
            updateExchangeRate(quote.quotedRate, from, to)
            updateTransactionFee(from, quote.minerFee)
            updateOrderId(quote.orderId)
        }
    }

    //region View Updates
    private fun updateDeposit(fromCurrency: CryptoCurrencies, depositAmount: BigDecimal) {
        val label = stringUtils.getFormattedString(R.string.shapeshift_deposit_title, fromCurrency.unit)
        val amount = "${depositAmount.toPlainString()} ${fromCurrency.symbol.toUpperCase()}"

        view.updateDeposit(label, amount)
    }

    private fun updateReceive(toCurrency: CryptoCurrencies, receiveAmount: BigDecimal) {
        val label = stringUtils.getFormattedString(R.string.shapeshift_receive_title, toCurrency.unit)
        val amount = "${receiveAmount.toPlainString()} ${toCurrency.symbol.toUpperCase()}"

        view.updateReceive(label, amount)
    }

    private fun updateExchangeRate(
            exchangeRate: BigDecimal,
            fromCurrency: CryptoCurrencies,
            toCurrency: CryptoCurrencies
    ) {
        val formattedExchangeRate = exchangeRate.setScale(8, RoundingMode.HALF_DOWN)
        val formattedString = stringUtils.getFormattedString(
                R.string.shapeshift_exchange_rate_formatted,
                fromCurrency.symbol,
                formattedExchangeRate,
                toCurrency.symbol
        )

        view.updateExchangeRate(formattedString)
    }

    private fun updateTransactionFee(fromCurrency: CryptoCurrencies, transactionFee: BigDecimal) {
        val displayString = "${decimalFormat.format(transactionFee)} ${fromCurrency.symbol}"

        view.updateTransactionFee(displayString)
    }

    private fun updateOrderId(displayString: String) {
        view.updateOrderId(displayString)
    }
    //endregion


    private fun updateMetadata(
            address: String,
            hashOut: String?,
            status: Trade.STATUS
    ) {

        shapeShiftDataManager.findTrade(address)
                .map {
                    it.apply {
                        this.status = status
                        this.hashOut = hashOut
                    }
                }
                .flatMapCompletable { shapeShiftDataManager.updateTrade(it) }
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .subscribe(
                        { Timber.d("Update metadata entry complete") },
                        { Timber.e(it) }
                )
    }

    //region UI State
    private fun handleState(status: Trade.STATUS) {
        when (status) {
            Trade.STATUS.NO_DEPOSITS -> onNoDeposit()
            Trade.STATUS.RECEIVED -> onReceived()
            Trade.STATUS.COMPLETE -> onComplete()
            Trade.STATUS.FAILED, Trade.STATUS.RESOLVED -> onFailed()
        }
    }

    private fun onNoDeposit() {
        val state = TradeDetailUiState(
                R.string.shapeshift_sending_title,
                R.string.shapeshift_sending_title,
                stringUtils.getFormattedString(R.string.shapeshift_step_number, 1),
                R.drawable.shapeshift_progress_airplane
        )
        view.updateUi(state)
    }

    private fun onReceived() {
        val state = TradeDetailUiState(
                R.string.shapeshift_in_progress_title,
                R.string.shapeshift_in_progress_summary,
                stringUtils.getFormattedString(R.string.shapeshift_step_number, 2),
                R.drawable.shapeshift_progress_exchange
        )
        view.updateUi(state)
    }

    private fun onComplete() {
        val state = TradeDetailUiState(
                R.string.shapeshift_complete_title,
                R.string.shapeshift_complete_title,
                stringUtils.getFormattedString(R.string.shapeshift_step_number, 3),
                R.drawable.shapeshift_progress_complete
        )
        view.updateUi(state)
    }

    private fun onFailed() {
        val state = TradeDetailUiState(
                R.string.shapeshift_failed_title,
                R.string.shapeshift_failed_summary,
                stringUtils.getString(R.string.shapeshift_failed_explanation),
                R.drawable.shapeshift_progress_failed
        )
        view.updateUi(state)
    }
    //endregion

    private fun isInFinalState(status: Trade.STATUS) = when (status) {
        Trade.STATUS.NO_DEPOSITS, Trade.STATUS.RECEIVED -> false
        Trade.STATUS.COMPLETE, Trade.STATUS.FAILED, Trade.STATUS.RESOLVED -> true
    }

    private fun getToFromPair(pair: String) : ToFromPair = when (pair.toLowerCase()) {
        ShapeShiftPairs.ETH_BTC -> ToFromPair(CryptoCurrencies.ETHER, CryptoCurrencies.BTC)
        ShapeShiftPairs.BTC_ETH -> ToFromPair(CryptoCurrencies.BTC, CryptoCurrencies.ETHER)
        else -> throw IllegalStateException("Unknown currency pair $pair")
    }

    private data class ToFromPair(val to: CryptoCurrencies, val from: CryptoCurrencies)

}