package piuk.blockchain.android.ui.charts

import piuk.blockchain.android.data.charts.ChartsDataManager
import piuk.blockchain.android.data.charts.TimeSpan
import piuk.blockchain.android.data.charts.models.ChartDatumDto
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.annotations.Mockable
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

class ChartsPresenter @Inject constructor(
        private val chartsDataManager: ChartsDataManager,
        private val exchangeRateFactory: ExchangeRateFactory,
        private val prefsUtil: PrefsUtil
) : BasePresenter<ChartsView>() {

    private val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(getBtcUnitType()) }
    internal var selectedTimeSpan by Delegates.observable(TimeSpan.MONTH) { _, _, new ->
        updateChartsData(new)
    }

    override fun onViewReady() {
        selectedTimeSpan = TimeSpan.MONTH
    }

    private fun updateChartsData(timeSpan: TimeSpan) {
        compositeDisposable.clear()
        getCurrentPrice()

        view.updateChartState(ChartsState.TimeSpanUpdated(timeSpan))

        when (timeSpan) {
            TimeSpan.ALL_TIME -> chartsDataManager.getAllTimePrice(view.cryptoCurrency, getFiatCurrency())
            TimeSpan.YEAR -> chartsDataManager.getYearPrice(view.cryptoCurrency, getFiatCurrency())
            TimeSpan.MONTH -> chartsDataManager.getMonthPrice(view.cryptoCurrency, getFiatCurrency())
            TimeSpan.WEEK -> chartsDataManager.getWeekPrice(view.cryptoCurrency, getFiatCurrency())
            TimeSpan.DAY -> chartsDataManager.getDayPrice(view.cryptoCurrency, getFiatCurrency())
        }.compose(RxUtil.addObservableToCompositeDisposable(this))
                .toList()
                .doOnSubscribe { view.updateChartState(ChartsState.Loading) }
                .doOnSubscribe { view.updateSelectedCurrency(view.cryptoCurrency) }
                .doOnSuccess { view.updateChartState(getChartsData(it)) }
                .doOnError { view.updateChartState(ChartsState.Error) }
                .subscribe(
                        { /* No-op */ },
                        { Timber.e(it) }
                )
    }

    private fun getChartsData(list: List<ChartDatumDto>) = ChartsState.Data(list, getCurrencySymbol())

    private fun getCurrentPrice() {
        val price = when (view.cryptoCurrency) {
            CryptoCurrencies.BTC -> exchangeRateFactory.getLastBtcPrice(getFiatCurrency())
            CryptoCurrencies.ETHER -> exchangeRateFactory.getLastEthPrice(getFiatCurrency())
            CryptoCurrencies.BCH -> exchangeRateFactory.getLastBchPrice(getFiatCurrency())
        }

        view.updateCurrentPrice(getCurrencySymbol(), price)
    }

    private fun getFiatCurrency() =
            prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

    private fun getCurrencySymbol() =
            monetaryUtil.getCurrencySymbol(getFiatCurrency(), view.locale)

    private fun getBtcUnitType() =
            prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)

}

sealed class ChartsState {

    @Mockable data class Data(
            val data: List<ChartDatumDto>,
            val fiatSymbol: String
    ) : ChartsState()

    @Mockable class TimeSpanUpdated(val timeSpan: TimeSpan) : ChartsState()
    object Loading : ChartsState()
    object Error : ChartsState()

}