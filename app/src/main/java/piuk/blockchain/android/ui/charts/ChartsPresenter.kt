package piuk.blockchain.android.ui.charts

import piuk.blockchain.android.data.charts.ChartsDataManager
import piuk.blockchain.android.data.charts.TimeSpan
import piuk.blockchain.android.data.charts.models.ChartDatumDto
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import javax.inject.Inject

class ChartsPresenter @Inject constructor(
        private val chartsDataManager: ChartsDataManager,
        private val prefsUtil: PrefsUtil
) : BasePresenter<ChartsView>() {

    private val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(getBtcUnitType()) }
    private var timeSpan = TimeSpan.MONTH
    private var cryptoCurrency = CryptoCurrencies.BTC

    override fun onViewReady() {
        cryptoCurrency = view.cryptoCurrency
        updateChartsData(timeSpan)
    }

    private fun updateChartsData(timeSpan: TimeSpan) {
        this.timeSpan = timeSpan
        compositeDisposable.clear()

        view.updateChartState(ChartsState.TimeSpanUpdated(timeSpan))

        when (timeSpan) {
            TimeSpan.ALL_TIME -> chartsDataManager.getAllTimePrice(cryptoCurrency, getFiatCurrency())
            TimeSpan.YEAR -> chartsDataManager.getYearPrice(cryptoCurrency, getFiatCurrency())
            TimeSpan.MONTH -> chartsDataManager.getMonthPrice(cryptoCurrency, getFiatCurrency())
            TimeSpan.WEEK -> chartsDataManager.getWeekPrice(cryptoCurrency, getFiatCurrency())
            TimeSpan.DAY -> chartsDataManager.getDayPrice(cryptoCurrency, getFiatCurrency())
        }.compose(RxUtil.addObservableToCompositeDisposable(this))
                .toList()
                .doOnSubscribe { view.updateChartState(ChartsState.Loading) }
                .doOnSubscribe { view.updateSelectedCurrency(cryptoCurrency) }
                .doOnSuccess { view.updateChartState(getChartsData(it)) }
                .doOnError { view.updateChartState(ChartsState.Error) }
                .subscribe(
                        { /* No-op */ },
                        { Timber.e(it) }
                )
    }

    private fun getChartsData(list: List<ChartDatumDto>) = ChartsState.Data(
            data = list,
            fiatSymbol = getCurrencySymbol(),
            getChartAllTime = { updateChartsData(TimeSpan.ALL_TIME) },
            getChartYear = { updateChartsData(TimeSpan.YEAR) },
            getChartMonth = { updateChartsData(TimeSpan.MONTH) },
            getChartWeek = { updateChartsData(TimeSpan.WEEK) },
            getChartDay = { updateChartsData(TimeSpan.DAY) }
    )

    private fun getFiatCurrency() =
            prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

    private fun getCurrencySymbol() = monetaryUtil.getCurrencySymbol(getFiatCurrency(), view.locale)

    private fun getBtcUnitType() =
            prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)

}

sealed class ChartsState {

    data class Data(
            val data: List<ChartDatumDto>,
            val fiatSymbol: String,
            val getChartAllTime: () -> Unit,
            val getChartYear: () -> Unit,
            val getChartMonth: () -> Unit,
            val getChartWeek: () -> Unit,
            val getChartDay: () -> Unit
    ) : ChartsState()

    class TimeSpanUpdated(val timeSpan: TimeSpan) : ChartsState()
    object Loading : ChartsState()
    object Error : ChartsState()

}