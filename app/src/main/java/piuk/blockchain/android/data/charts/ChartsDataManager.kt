package piuk.blockchain.android.data.charts

import info.blockchain.wallet.prices.PriceApi
import info.blockchain.wallet.prices.Scale
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Observable
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxPinning
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.util.annotations.Mockable
import java.util.*

@Mockable
class ChartsDataManager(private val historicPriceApi: PriceApi, rxBus: RxBus) {

    private val rxPinning = RxPinning(rxBus)

    //region Convenience methods
    /**
     * Returns a stream of [PriceDatum] objects representing prices with timestamps over the last
     * year, with each measurement being 24 hours apart.
     *
     * @param cryptoCurrency The chosen [CryptoCurrencies] object
     * @param fiatCurrency The fiat currency which you want results for, eg "USD"
     * @return A stream of [PriceDatum] objects via an [Observable]
     */
    fun getYearPrice(cryptoCurrency: CryptoCurrencies, fiatCurrency: String): Observable<PriceDatum> =
            rxPinning.call<PriceDatum> {
                getHistoricPriceObservable(cryptoCurrency, fiatCurrency, TimeSpan.YEAR)
            }

    /**
     * Returns a stream of [PriceDatum] objects representing prices with timestamps over the last
     * month, with each measurement being 2 hours apart.
     *
     * @param cryptoCurrency The chosen [CryptoCurrencies] object
     * @param fiatCurrency The fiat currency which you want results for, eg "USD"
     * @return A stream of [PriceDatum] objects via an [Observable]
     */
    fun getMonthPrice(cryptoCurrency: CryptoCurrencies, fiatCurrency: String): Observable<PriceDatum> =
            rxPinning.call<PriceDatum> {
                getHistoricPriceObservable(cryptoCurrency, fiatCurrency, TimeSpan.MONTH)
            }

    /**
     * Returns a stream of [PriceDatum] objects representing prices with timestamps over the last
     * week, with each measurement being 1 hour apart.
     *
     * @param cryptoCurrency The chosen [CryptoCurrencies] object
     * @param fiatCurrency The fiat currency which you want results for, eg "USD"
     * @return A stream of [PriceDatum] objects via an [Observable]
     */
    fun getWeekPrice(cryptoCurrency: CryptoCurrencies, fiatCurrency: String): Observable<PriceDatum> =
            rxPinning.call<PriceDatum> {
                getHistoricPriceObservable(cryptoCurrency, fiatCurrency, TimeSpan.WEEK)
            }

    /**
     * Returns a stream of [PriceDatum] objects representing prices with timestamps over the last
     * day, with each measurement being 15 minutes apart.
     *
     * @param cryptoCurrency The chosen [CryptoCurrencies] object
     * @param fiatCurrency The fiat currency which you want results for, eg "USD"
     * @return A stream of [PriceDatum] objects via an [Observable]
     */
    fun getDayPrice(cryptoCurrency: CryptoCurrencies, fiatCurrency: String): Observable<PriceDatum> =
            rxPinning.call<PriceDatum> {
                getHistoricPriceObservable(cryptoCurrency, fiatCurrency, TimeSpan.DAY)
            }
    //endregion

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