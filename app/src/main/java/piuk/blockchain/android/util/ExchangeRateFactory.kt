package piuk.blockchain.android.util

import info.blockchain.wallet.prices.PriceApi
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Observable
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxPinning
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.stores.Optional
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.util.annotations.Mockable
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import javax.inject.Inject

/**
 * This class obtains info on the currencies communicated via https://blockchain.info/ticker. This
 * is set up like a classical Singleton for compatibility's sake, and would normally be a Kotlin
 * object instead. However, we need this class open for testing, and we (for now) need to access
 * it outside of dependency injection.
 */
@Mockable
class ExchangeRateFactory private constructor() {

    private object Holder {
        val INSTANCE = ExchangeRateFactory()
    }

    private final val priceApi by unsafeLazy { PriceApi() }
    private final val rxPinning: RxPinning
    // Ticker data
    private var btcTickerData: Map<String, PriceDatum>? = null
    private var ethTickerData: Map<String, PriceDatum>? = null

    @Inject final lateinit var prefsUtil: PrefsUtil
    @Inject final lateinit var rxBus: RxBus

    init {
        @Suppress("LeakingThis") // This will be resolved in the future
        Injector.getInstance().appComponent.inject(this)
        rxPinning = RxPinning(rxBus)
    }

    fun updateTickers(): Observable<Map<String, PriceDatum>> = rxPinning.call<Map<String, PriceDatum>> {
        getBtcTicker().mergeWith(getEthTicker())
    }

    fun getLastBtcPrice(currencyName: String) = getLastPrice(currencyName.toUpperCase(), CryptoCurrencies.BTC)

    fun getLastEthPrice(currencyName: String) = getLastPrice(currencyName.toUpperCase(), CryptoCurrencies.ETHER)

    @Deprecated(
            "use MonetaryUtil.getCurrencySymbol, as this method doesn't allow the passing of a locale",
            replaceWith = ReplaceWith(
                    "monetaryUtil.getCurrencySymbol(currencyName, Locale.getDefault())",
                    "piuk.blockchain.android.util.MonetaryUtil"
            )
    )
    fun getSymbol(currencyName: String): String {
        var currency = currencyName
        if (currency.isEmpty()) {
            currency = "USD"
        }
        return Currency.getInstance(currency).getSymbol(Locale.getDefault())
    }

    fun getCurrencyLabels(): Array<String> = btcTickerData!!.keys.toTypedArray()

    /**
     * Returns the historic value of a number of Satoshi at a given time in a given currency.
     *
     * @param satoshis     The amount of Satoshi to be converted
     * @param currency     The currency to be converted to as a 3 letter acronym, eg USD, GBP
     * @param timeInSeconds The time at which to get the price, in seconds since epoch
     * @return A double value, which <b>is not</b> rounded to any significant figures
     */
    fun getBtcHistoricPrice(
            satoshis: Long,
            currency: String,
            timeInSeconds: Long
    ): Observable<Double> = rxPinning.call<Double> {
        priceApi.getHistoricPrice(CryptoCurrencies.BTC.symbol, currency, timeInSeconds)
                .map {
                    val exchangeRate = BigDecimal.valueOf(it)
                    val satoshiDecimal = BigDecimal.valueOf(satoshis)
                    return@map exchangeRate.multiply(satoshiDecimal.divide(SATOSHIS_PER_BITCOIN))
                            .toDouble()
                }
                .compose(RxUtil.applySchedulersToObservable())
    }

    /**
     * Returns the historic value of a number of Wei at a given time in a given currency.
     *
     * @param wei          The amount of Ether to be converted in Wei, ie ETH * 1e18
     * @param currency     The currency to be converted to as a 3 letter acronym, eg USD, GBP
     * @param timeInSeconds The time at which to get the price, in seconds since epoch
     * @return A double value, which <b>is not</b> rounded to any significant figures
     */
    fun getEthHistoricPrice(
            wei: BigInteger,
            currency: String,
            timeInSeconds: Long
    ): Observable<Double> = rxPinning.call<Double> {
        priceApi.getHistoricPrice(CryptoCurrencies.ETHER.symbol, currency, timeInSeconds)
                .map {
                    val exchangeRate = BigDecimal.valueOf(it)
                    val ethDecimal = BigDecimal(wei)
                    return@map exchangeRate.multiply(ethDecimal.divide(WEI_PER_ETHER))
                            .toDouble()
                }
                .compose(RxUtil.applySchedulersToObservable())
    }

    private fun getLastPrice(currencyName: String, cryptoCurrency: CryptoCurrencies): Double {
        val prefsKey: String
        val tickerData: Map<String, PriceDatum>?

        when (cryptoCurrency) {
            CryptoCurrencies.BTC -> {
                prefsKey = PREF_LAST_KNOWN_BTC_PRICE
                tickerData = btcTickerData
            }
            CryptoCurrencies.ETHER -> {
                prefsKey = PREF_LAST_KNOWN_ETH_PRICE
                tickerData = ethTickerData
            }
            else -> throw IllegalArgumentException("BCH is not currently supported")
        }
        var currency = currencyName
        if (currency.isEmpty()) {
            currency = "USD"
        }

        var lastPrice: Double
        val lastKnown = prefsUtil.getValue("$prefsKey$currency", "0.0").toDouble()

        if (tickerData == null) {
            lastPrice = lastKnown
        } else {
            val tickerItem = when (cryptoCurrency) {
                CryptoCurrencies.BTC -> getTickerItem(currency, btcTickerData)
                CryptoCurrencies.ETHER -> getTickerItem(currency, ethTickerData)
                else -> throw IllegalArgumentException("BCH is not currently supported")
            }

            lastPrice = when (tickerItem) {
                is Optional.Some -> tickerItem.element.price ?: 0.0
                else -> 0.0
            }

            if (lastPrice > 0.0) {
                prefsUtil.setValue("$prefsKey$currency", lastPrice.toString())
            } else {
                lastPrice = lastKnown
            }
        }

        return lastPrice
    }

    private fun getBtcTicker() = rxPinning.call<Map<String, PriceDatum>> {
        priceApi.getPriceIndexes(CryptoCurrencies.BTC.symbol)
                .doOnNext { this.btcTickerData = it.toMap() }
                .compose(RxUtil.applySchedulersToObservable())
    }

    private fun getEthTicker() = rxPinning.call<Map<String, PriceDatum>> {
        priceApi.getPriceIndexes(CryptoCurrencies.ETHER.symbol)
                .doOnNext { this.ethTickerData = it.toMap() }
                .compose(RxUtil.applySchedulersToObservable())
    }

    private fun getTickerItem(
            currencyName: String,
            tickerData: Map<String, PriceDatum>?
    ): Optional<PriceDatum> {
        val priceDatum = tickerData?.get(currencyName)
        return when {
            priceDatum != null -> Optional.Some(priceDatum)
            else -> Optional.None
        }
    }

    companion object {

        private const val PREF_LAST_KNOWN_BTC_PRICE = "LAST_KNOWN_BTC_VALUE_FOR_CURRENCY_"
        private const val PREF_LAST_KNOWN_ETH_PRICE = "LAST_KNOWN_ETH_VALUE_FOR_CURRENCY_"

        private val SATOSHIS_PER_BITCOIN = BigDecimal.valueOf(100_000_000L)
        private val WEI_PER_ETHER = BigDecimal.valueOf(1e18)

        @JvmStatic
        val instance: ExchangeRateFactory by unsafeLazy { Holder.INSTANCE }

    }
}
