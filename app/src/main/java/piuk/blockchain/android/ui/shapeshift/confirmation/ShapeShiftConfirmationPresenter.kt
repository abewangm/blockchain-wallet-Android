package piuk.blockchain.android.ui.shapeshift.confirmation

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.util.StringUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ShapeShiftConfirmationPresenter @Inject constructor(
        private val payloadDataManager: PayloadDataManager,
        private val ethDataManager: EthDataManager,
        private val stringUtils: StringUtils
) : BasePresenter<ShapeShiftConfirmationView>() {

    private var termsAccepted = false

    override fun onViewReady() {
        with(view.shapeShiftData) {
            // Render data
            updateDeposit(fromCurrency, depositAmount)
            updateReceive(toCurrency, withdrawalAmount)
            updateExchangeRate(exchangeRate, fromCurrency, toCurrency)

            // Include countdown
            startCountdown(expiration)
        }
    }

    internal fun onAcceptTermsClicked() {
        termsAccepted = !termsAccepted
        view.setButtonState(termsAccepted)
    }

    internal fun onConfirmClicked() {
        if (!termsAccepted) {
            // Show warning
        } else {
            // Do some things
        }

    }

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

    private fun startCountdown(endTime: Long) {
        var remaining = (endTime - System.currentTimeMillis()) / 1000
        if (remaining <= 0) {
            // Finish page with error
        } else {
            Observable.interval(1, TimeUnit.SECONDS)
                    .compose(RxUtil.addObservableToCompositeDisposable(this))
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnEach { remaining-- }
                    .map { return@map remaining }
                    .doOnNext {
                        val readableTime = String.format("%2d:%02d",
                                TimeUnit.SECONDS.toMinutes(it),
                                TimeUnit.SECONDS.toSeconds(it) -
                                        TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(it))
                        )
                        view.updateCounter(readableTime)
                    }
                    .takeUntil { it <= 0 }
                    .doOnComplete { /** Show countdown finished */ }
                    .subscribe()
        }
    }

}