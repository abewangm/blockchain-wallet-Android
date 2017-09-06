package piuk.blockchain.android.ui.dashboard

import info.blockchain.api.data.Point
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.charts.ChartsDataManager
import piuk.blockchain.android.data.charts.TimeSpan
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat
import javax.inject.Inject

class DashboardPresenter @Inject constructor(
        private val chartsDataManager: ChartsDataManager,
        private val prefsUtil: PrefsUtil,
        private val exchangeRateFactory: ExchangeRateFactory,
        private val ethDataManager: EthDataManager,
        private val payloadDataManager: PayloadDataManager,
        private val transactionListDataManager: TransactionListDataManager,
        private val stringUtils: StringUtils
) : BasePresenter<DashboardView>() {

    private val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(getBtcUnitType()) }
    private var cryptoCurrency = CryptoCurrencies.BTC

    override fun onViewReady() {
        updateChartsData(TimeSpan.YEAR)
        updateAllBalances()
    }

    internal fun updateChartsData(timeSpan: TimeSpan) {
        compositeDisposable.clear()

        view.updateChartState(ChartsState.SelectedTime(timeSpan))

        when (timeSpan) {
            TimeSpan.YEAR -> chartsDataManager.getYearPrice()
            TimeSpan.MONTH -> chartsDataManager.getMonthPrice()
            TimeSpan.WEEK -> chartsDataManager.getWeekPrice()
            else -> throw IllegalArgumentException("Day isn't currently supported")
        }.compose(RxUtil.addObservableToCompositeDisposable(this))
                .toList()
                .doOnSubscribe { view.updateChartState(ChartsState.Loading()) }
                .doOnSuccess { view.updateChartState(ChartsState.Data(it)) }
                .doOnError { view.updateChartState(ChartsState.Error()) }
                .subscribe(
                        { /* No-op */ },
                        { Timber.e(it) }
                )
    }

    internal fun getCurrencySymbol() = exchangeRateFactory.getSymbol(getFiatCurrency())

    internal fun updateSelectedCurrency(cryptoCurrency: CryptoCurrencies) {
        this.cryptoCurrency = cryptoCurrency
        updatePrices()
    }

    // TODO: This can be slow and can fail  
    internal fun updatePrices() {
        exchangeRateFactory.updateTickers()
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .subscribe(
                        {
                            if (cryptoCurrency == CryptoCurrencies.BTC) {
                                view.updateCryptoCurrencyPrice(getBtcString())
                            } else {
                                view.updateCryptoCurrencyPrice(getEthString())
                            }
                        },
                        { Timber.e(it) })
    }

    private fun updateAllBalances() {
        ethDataManager.getEthereumWallet(stringUtils.getString(R.string.eth_default_account_label))
                .flatMap { ethDataManager.fetchEthAddress() }
                .flatMapCompletable { ethAddressResponse ->
                    payloadDataManager.updateAllBalances()
                            .doOnComplete {
                                val btcBalance = transactionListDataManager.getBtcBalance(ItemAccount().apply {
                                    type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
                                })
                                view.updateBtcBalance(getBtcBalanceString(btcBalance))
                                view.updateEthBalance(getEthBalanceString(ethAddressResponse.balance))

                                val btcFiat = exchangeRateFactory.getLastBtcPrice(getFiatCurrency()) * (btcBalance / 1e8)
                                val ethFiat = BigDecimal(exchangeRateFactory.getLastEthPrice(getFiatCurrency()))
                                        .multiply(Convert.fromWei(BigDecimal(ethAddressResponse.balance), Convert.Unit.ETHER))

                                val totalDouble = btcFiat.plus(ethFiat.toDouble())

                                val totalString = "${getCurrencySymbol()}${monetaryUtil.getFiatFormat(getFiatCurrency()).format(totalDouble)}"
                                view.updateTotalBalance(totalString)
                            }
                }.subscribe(
                { /* No-op*/ },
                { Timber.e(it) }
        )
    }

    private fun getBtcBalanceString(btcBalance: Long): String {
        return "${monetaryUtil.getDisplayAmountWithFormatting(btcBalance)} ${getBtcDisplayUnits()}"
    }

    private fun getEthBalanceString(ethBalance: BigInteger): String {
        val number = DecimalFormat.getInstance().apply { maximumFractionDigits = 8 }
                .run { format(Convert.fromWei(BigDecimal(ethBalance), Convert.Unit.ETHER)) }

        return "$number ETH"

    }

    private fun getBtcString(): String {
        val lastBtcPrice = exchangeRateFactory.getLastBtcPrice(getFiatCurrency())
        return "${getCurrencySymbol()}${monetaryUtil.getFiatFormat(getFiatCurrency()).format(lastBtcPrice)}"
    }

    private fun getEthString(): String {
        val lastEthPrice = exchangeRateFactory.getLastEthPrice(getFiatCurrency())
        return "${getCurrencySymbol()}${monetaryUtil.getFiatFormat(getFiatCurrency()).format(lastEthPrice)}"
    }

    private fun getBtcDisplayUnits() = monetaryUtil.getBtcUnits()[getBtcUnitType()]

    private fun getBtcUnitType() =
            prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)

    private fun getFiatCurrency() =
            prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

}

sealed class ChartsState {

    data class Data(val data: List<Point>) : ChartsState()
    class Error : ChartsState()
    class Loading : ChartsState()
    data class SelectedTime(val timeSpan: TimeSpan) : ChartsState()

}