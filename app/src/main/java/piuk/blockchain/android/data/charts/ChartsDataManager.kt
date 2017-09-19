package piuk.blockchain.android.data.charts

import info.blockchain.wallet.prices.PriceApi
import info.blockchain.wallet.prices.Scale
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Observable
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxPinning
import piuk.blockchain.android.data.rxjava.RxUtil
import java.util.*

class ChartsDataManager(private val historicPriceApi: PriceApi, rxBus: RxBus) {

    private val rxPinning = RxPinning(rxBus)

    fun getYearPrice(cryptoCurrency: CryptoCurrencies, fiatCurrency: String): Observable<PriceDatum> =
            rxPinning.call<PriceDatum> {
                getHistoricPriceObservable(cryptoCurrency, fiatCurrency, TimeSpan.YEAR)
            }

    fun getMonthPrice(cryptoCurrency: CryptoCurrencies, fiatCurrency: String): Observable<PriceDatum> =
            rxPinning.call<PriceDatum> {
                getHistoricPriceObservable(cryptoCurrency, fiatCurrency, TimeSpan.MONTH)
            }

    fun getWeekPrice(cryptoCurrency: CryptoCurrencies, fiatCurrency: String): Observable<PriceDatum> =
            rxPinning.call<PriceDatum> {
                getHistoricPriceObservable(cryptoCurrency, fiatCurrency, TimeSpan.WEEK)
            }

    fun getDayPrice(cryptoCurrency: CryptoCurrencies, fiatCurrency: String): Observable<PriceDatum> =
            rxPinning.call<PriceDatum> {
                getHistoricPriceObservable(cryptoCurrency, fiatCurrency, TimeSpan.DAY)
            }

    private fun getHistoricPriceObservable(
            cryptoCurrency: CryptoCurrencies,
            fiatCurrency: String,
            timeSpan: TimeSpan
    ): Observable<PriceDatum> {

        val scale = when (timeSpan) {
            TimeSpan.YEAR -> Scale.ONE_DAY
            TimeSpan.MONTH -> Scale.TWO_HOURS
            TimeSpan.WEEK -> Scale.ONE_HOUR
            TimeSpan.DAY -> Scale.FIFTEEN_MINUTES
        }

        return historicPriceApi.getHistoricPriceSeries(
                cryptoCurrency.symbol,
                fiatCurrency,
                getStartTimeForTimeSpan(timeSpan),
                scale
        ).flatMapIterable { it }
                .compose(RxUtil.applySchedulersToObservable())
    }

    private fun getStartTimeForTimeSpan(timeSpan: TimeSpan): Long {
        val start = when (timeSpan) {
            TimeSpan.YEAR -> 365
            TimeSpan.MONTH -> 30
            TimeSpan.WEEK -> 7
            TimeSpan.DAY -> 1
        }

        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -start) }
        return cal.timeInMillis / 1000
    }

}