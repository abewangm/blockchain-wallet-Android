package piuk.blockchain.android.util

import info.blockchain.api.data.TickerItem
import info.blockchain.api.exchangerates.ExchangeRates
import info.blockchain.wallet.BlockchainFramework
import info.blockchain.wallet.exceptions.ApiException
import info.blockchain.wallet.prices.PriceApi
import io.reactivex.Completable
import io.reactivex.Observable
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxPinning
import piuk.blockchain.android.data.rxjava.RxUtil
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

    private final val SATOSHIS_PER_BITCOIN = BigDecimal.valueOf(100_000_000L)
    private final val WEI_PER_ETHER = BigDecimal.valueOf(1e18)

    private object Holder {
        val INSTANCE = ExchangeRateFactory()
    }

    private final val priceApi by unsafeLazy { PriceApi() }
    private final val api: ExchangeRates
    private final val rxPinning: RxPinning
    // Ticker data
    private var btcTickerData: TreeMap<String, TickerItem>? = null
    private var ethTickerData: TreeMap<String, TickerItem>? = null

    @Inject lateinit final var prefsUtil: PrefsUtil
    @Inject lateinit final var rxBus: RxBus

    init {
        @Suppress("LeakingThis") // This will be resolved in the future
        Injector.getInstance().appComponent.inject(this)

        api = ExchangeRates(
                BlockchainFramework.getRetrofitExplorerInstance(),
                BlockchainFramework.getRetrofitApiInstance(),
                BlockchainFramework.getApiCode()
        )
        rxPinning = RxPinning(rxBus)
    }

    fun updateTickers(): Completable = rxPinning.call {
        getBtcTicker().mergeWith(getEthTicker())
    }.compose(RxUtil.applySchedulersToCompletable())

    fun getLastBtcPrice(currencyName: String) = getLastPrice(currencyName, CryptoCurrencies.BTC)

    fun getLastEthPrice(currencyName: String) = getLastPrice(currencyName, CryptoCurrencies.ETHER)

    fun getSymbol(currencyName: String): String {
        var currency = currencyName
        if (currency.isEmpty()) {
            currency = "USD"
        }

        return getBtcTickerItem(currency)?.symbol ?: "$"
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
        priceApi.getHistoricPrice("btc", currency, timeInSeconds)
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
        priceApi.getHistoricPrice("eth", currency, timeInSeconds)
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
        val tickerData: TreeMap<String, TickerItem>?

        when (cryptoCurrency) {
            CryptoCurrencies.BTC -> {
                prefsKey = PREF_LAST_KNOWN_BTC_PRICE
                tickerData = btcTickerData
            }
            CryptoCurrencies.ETHER -> {
                prefsKey = PREF_LAST_KNOWN_ETH_PRICE
                tickerData = ethTickerData
            }
            else -> throw IllegalArgumentException("BCC is not currently supported")
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
                CryptoCurrencies.BTC -> getBtcTickerItem(currency)
                CryptoCurrencies.ETHER -> getEthTickerItem(currency)
                else -> throw IllegalArgumentException("BCC is not currently supported")
            }

            lastPrice = tickerItem?.last ?: 0.0

            if (lastPrice > 0.0) {
                prefsUtil.setValue("$prefsKey$currency", lastPrice.toString())
            } else {
                lastPrice = lastKnown
            }
        }

        return lastPrice
    }

    @Suppress("DEPRECATION")
    private fun getBtcTicker(): Completable = Completable.fromCallable {
        val call = api.btcTickerMap.execute()
        if (call.isSuccessful) {
            btcTickerData = call.body()
            Void.TYPE
        } else {
            throw ApiException(call.errorBody()!!.string())
        }
    }

    @Suppress("DEPRECATION")
    private fun getEthTicker(): Completable = Completable.fromCallable {
        val call = api.ethTickerMap.execute()
        if (call.isSuccessful) {
            ethTickerData = call.body()
            Void.TYPE
        } else {
            throw ApiException(call.errorBody()!!.string())
        }
    }

    private fun getBtcTickerItem(currencyName: String) = btcTickerData!![currencyName]

    private fun getEthTickerItem(currencyName: String) = ethTickerData!![currencyName]

    companion object {

        private val PREF_LAST_KNOWN_BTC_PRICE = "LAST_KNOWN_BTC_VALUE_FOR_CURRENCY_"
        private val PREF_LAST_KNOWN_ETH_PRICE = "LAST_KNOWN_ETH_VALUE_FOR_CURRENCY_"

        @JvmStatic
        val instance: ExchangeRateFactory by unsafeLazy { Holder.INSTANCE }

    }
}
