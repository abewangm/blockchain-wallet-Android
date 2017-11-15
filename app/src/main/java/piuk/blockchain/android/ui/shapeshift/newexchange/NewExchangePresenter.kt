package piuk.blockchain.android.ui.shapeshift.newexchange

import info.blockchain.api.data.UnspentOutputs
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.shapeshift.ShapeShiftPairs
import info.blockchain.wallet.shapeshift.data.MarketInfo
import info.blockchain.wallet.shapeshift.data.Quote
import info.blockchain.wallet.shapeshift.data.QuoteRequest
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.datamanagers.FeeDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.ethereum.models.CombinedEthModel
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.payments.SendDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.shapeshift.CoinPairings
import piuk.blockchain.android.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.data.stores.Either
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.receive.ReceiveCurrencyHelper
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.ui.shapeshift.models.ShapeShiftData
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class NewExchangePresenter @Inject constructor(
        private val payloadDataManager: PayloadDataManager,
        private val ethDataManager: EthDataManager,
        private val prefsUtil: PrefsUtil,
        private val sendDataManager: SendDataManager,
        private val dynamicFeeCache: DynamicFeeCache,
        private val feeDataManager: FeeDataManager,
        private val exchangeRateFactory: ExchangeRateFactory,
        private val currencyState: CurrencyState,
        private val shapeShiftDataManager: ShapeShiftDataManager,
        private val stringUtils: StringUtils,
        walletAccountHelper: WalletAccountHelper
) : BasePresenter<NewExchangeView>() {

    val toCryptoSubject: PublishSubject<String> = PublishSubject.create<String>()
    val fromCryptoSubject: PublishSubject<String> = PublishSubject.create<String>()
    val toFiatSubject: PublishSubject<String> = PublishSubject.create<String>()
    val fromFiatSubject: PublishSubject<String> = PublishSubject.create<String>()

    private val ethLabel = stringUtils.getString(R.string.shapeshift_eth)
    private val btcAccounts = walletAccountHelper.getHdAccounts()
    private val monetaryUtil by unsafeLazy {
        MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
    }
    private val currencyHelper by unsafeLazy {
        ReceiveCurrencyHelper(monetaryUtil, Locale.getDefault(), prefsUtil, exchangeRateFactory, currencyState)
    }
    private var marketInfo: MarketInfo? = null
    private var shapeShiftData: ShapeShiftData? = null
    private var latestQuote: Quote? = null
    private var account: Account? = null
    private var feeOptions: FeeOptions? = null

    override fun onViewReady() {
        val selectedCurrency = currencyState.cryptoCurrency
        view.updateUi(
                selectedCurrency,
                btcAccounts.size > 1,
                if (selectedCurrency == CryptoCurrencies.BTC) getDefaultBtcLabel() else ethLabel,
                if (selectedCurrency == CryptoCurrencies.ETHER) getDefaultBtcLabel() else ethLabel,
                monetaryUtil.getFiatDisplayString(0.0, currencyHelper.fiatUnit, Locale.getDefault())
        )

        val shapeShiftObservable = getMarketInfoObservable(selectedCurrency)
        val feesObservable = getFeesObservable(selectedCurrency)

        shapeShiftObservable
                .doOnNext {
                    marketInfo = it
                    shapeShiftData = ShapeShiftData(
                            selectedCurrency,
                            if (selectedCurrency == CryptoCurrencies.BTC) CryptoCurrencies.ETHER else CryptoCurrencies.BTC,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            it.rate,
                            BigDecimal.ZERO,
                            BigDecimal.valueOf(it.minerFee),
                            "",
                            ""
                    )
                }
                .flatMap { feesObservable }
                .doOnSubscribe { view.showProgressDialog(R.string.shapeshift_getting_information) }
                .doOnTerminate { view.dismissProgressDialog() }
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(
                        {
                            account = payloadDataManager.defaultAccount
                        },
                        {
                            Timber.e(it)
                            view.showToast(R.string.shapeshift_getting_information_failed, ToastCustom.TYPE_ERROR)
                            view.finishPage()
                        }
                )

        fromCryptoSubject.applyDefaults()
                // Update from Fiat as it's not dependent on web results
                .doOnNext { updateFromFiat(it) }
                // Update results dependent on Shapeshift
                .flatMap { amount ->
                    getQuoteFromRequest(amount, currencyState.cryptoCurrency)
                            .doOnNext { updateToFields(it.withdrawalAmount) }
                }
                .subscribe()

        fromFiatSubject.applyDefaults()
                // Convert to from crypto amount
                .map {
                    val (_, toExchangeRate) = getExchangeRates()
                    return@map it.divide(toExchangeRate, 18, RoundingMode.HALF_UP)
                }
                // Update from amount view
                .doOnNext { view.updateFromCryptoText(it.toPlainString()) }
                // Update results dependent on Shapeshift
                .flatMap { amount ->
                    getQuoteFromRequest(amount, currencyState.cryptoCurrency)
                            .doOnNext { updateToFields(it.withdrawalAmount) }
                }
                .subscribe()

        toCryptoSubject.applyDefaults()
                // Update to Fiat as it's not dependent on web results
                .doOnNext { updateToFiat(it) }
                // Update results dependent on Shapeshift
                .flatMap { amount ->
                    getQuoteToRequest(amount, currencyState.cryptoCurrency)
                            .doOnNext { updateFromFields(it.depositAmount) }
                }
                .subscribe()

        toFiatSubject.applyDefaults()
                // Convert to from crypto amount
                .map {
                    val (fromExchangeRate, _) = getExchangeRates()
                    return@map it.divide(fromExchangeRate, 18, RoundingMode.HALF_UP)
                }
                // Update from amount view
                .doOnNext { view.updateToCryptoText(it.toPlainString()) }
                // Update results dependent on Shapeshift
                .flatMap { amount ->
                    getQuoteToRequest(amount, currencyState.cryptoCurrency)
                            .doOnNext { updateFromFields(it.depositAmount) }
                }
                .subscribe()
    }

    private fun getQuoteFromRequest(fromAmount: BigDecimal, selectedCurrency: CryptoCurrencies): Observable<Quote> {
        val quoteRequest = QuoteRequest().apply {
            depositAmount = fromAmount.setScale(8, RoundingMode.HALF_UP).toDouble()
            pair = if (selectedCurrency == CryptoCurrencies.BTC) ShapeShiftPairs.BTC_ETH else ShapeShiftPairs.ETH_BTC
            apiKey = view.shapeShiftApiKey
        }

        return getQuoteObservable(quoteRequest, selectedCurrency)
    }

    private fun getQuoteToRequest(toAmount: BigDecimal, selectedCurrency: CryptoCurrencies): Observable<Quote> {
        val quoteRequest = QuoteRequest().apply {
            withdrawalAmount = toAmount.setScale(8, RoundingMode.HALF_UP).toDouble()
            pair = if (selectedCurrency == CryptoCurrencies.BTC) ShapeShiftPairs.BTC_ETH else ShapeShiftPairs.ETH_BTC
            apiKey = view.shapeShiftApiKey
        }

        return getQuoteObservable(quoteRequest, selectedCurrency)
    }

    internal fun onSwitchCurrencyClicked() {
        currencyState.toggleCryptoCurrency()
                .run { view.clearEditTexts() }
                .run { onViewReady() }
    }

    internal fun onContinuePressed() {
        check(marketInfo != null && latestQuote != null) { "Market Info or Quote is null" }
        // TODO: Set up parcelable object if all fields are valid
    }

    internal fun onMaxPressed() {
        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> updateMaxBtcAmount()
            CryptoCurrencies.ETHER -> updateMaxEthAmount()
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }
    }

    internal fun onMinPressed() {
        // TODO: Calculate if min is more than user has available to spend
        with(getMinimum()) {
            view.updateFromCryptoText(this.toPlainString())
        }
    }

    internal fun onFromChooserClicked() {
        if (currencyState.cryptoCurrency == CryptoCurrencies.BTC) view.launchAccountChooserActivityFrom()
    }

    internal fun onToChooserClicked() {
        if (currencyState.cryptoCurrency == CryptoCurrencies.ETHER) view.launchAccountChooserActivityTo()
    }

    // Here we can safely assume BTC is the "from" type
    internal fun onFromAccountChanged(account: Account) {
        this.account = account
        view.updateUi(
                currencyState.cryptoCurrency,
                btcAccounts.size > 1,
                account.label,
                ethLabel,
                monetaryUtil.getFiatDisplayString(0.0, currencyHelper.fiatUnit, Locale.getDefault())
        )
    }

    // Here we can safely assume ETH is the "from" type
    internal fun onToAccountChanged(account: Account) {
        this.account = account
        view.updateUi(
                currencyState.cryptoCurrency,
                btcAccounts.size > 1,
                ethLabel,
                account.label,
                monetaryUtil.getFiatDisplayString(0.0, currencyHelper.fiatUnit, Locale.getDefault())
        )
    }

    private fun getExchangeRates(): ExchangeRates {
        val currencyCode = currencyHelper.fiatUnit
        return when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> ExchangeRates(
                    BigDecimal.valueOf(exchangeRateFactory.getLastEthPrice(currencyCode)),
                    BigDecimal.valueOf(exchangeRateFactory.getLastBtcPrice(currencyCode))
            )
            CryptoCurrencies.ETHER -> ExchangeRates(
                    BigDecimal.valueOf(exchangeRateFactory.getLastBtcPrice(currencyCode)),
                    BigDecimal.valueOf(exchangeRateFactory.getLastEthPrice(currencyCode))
            )
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }
    }

    private fun updateMaxBtcAmount() {
        getUnspentApiResponse(account!!.xpub)
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(
                        { coins ->
                            val sweepBundle = sendDataManager.getMaximumAvailable(
                                    coins,
                                    BigInteger.valueOf(feeOptions!!.regularFee * 1000)
                            )
                            val sweepableAmount = BigDecimal(sweepBundle.left)
                                    .divide(BigDecimal.valueOf(1e8))
                            val maximum = if (sweepableAmount > getMaximum()) getMaximum() else sweepableAmount

                            view.updateFromCryptoText(maximum.toPlainString())
                        },
                        { throwable ->
                            Timber.e(throwable)
                            // No unspent outputs
                            // TODO: Handle error state
                        })
    }

    private fun updateMaxEthAmount() {
        ethDataManager.fetchEthAddress()
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .doOnError { view.showToast(R.string.shapeshift_getting_fees_failed, ToastCustom.TYPE_ERROR) }
                .subscribe { calculateUnspentEth(it) }
    }

    private fun calculateUnspentEth(combinedEthModel: CombinedEthModel) {
        val gwei = BigDecimal.valueOf(feeOptions!!.gasLimit * feeOptions!!.regularFee)
        val wei = Convert.toWei(gwei, Convert.Unit.GWEI)

        val addressResponse = combinedEthModel.getAddressResponse()
        val maxAvailable = addressResponse!!.balance!!.minus(wei.toBigInteger()).max(BigInteger.ZERO)

        val availableEth = Convert.fromWei(maxAvailable.toString(), Convert.Unit.ETHER)
        val maximum = if (availableEth > getMaximum()) getMaximum() else availableEth

        view.updateFromCryptoText(maximum.toPlainString())
    }

    private fun getUnspentApiResponse(address: String): Observable<UnspentOutputs> {
        return if (payloadDataManager.getAddressBalance(address).toLong() > 0) {
            sendDataManager.getUnspentOutputs(address)
        } else {
            Observable.error(Throwable("No funds - skipping call to unspent API"))
        }
    }

    private fun getDefaultBtcLabel() = if (btcAccounts.size > 1) {
        payloadDataManager.defaultAccount.label
    } else {
        stringUtils.getString(R.string.shapeshift_btc)
    }

    private fun getMaximum() = BigDecimal.valueOf(marketInfo?.maxLimit ?: 0.0)

    private fun getMinimum() = BigDecimal.valueOf(marketInfo?.minimum ?: 0.0)

    //region Field Updates
    private fun updateFromFiat(amount: BigDecimal) {
        view.updateFromFiatText(
                monetaryUtil.getFiatDisplayString(
                        amount.multiply(getExchangeRates().fromRate).toDouble(),
                        currencyHelper.fiatUnit,
                        view.locale
                )
        )
    }

    private fun updateToFiat(amount: BigDecimal) {
        view.updateToFiatText(
                monetaryUtil.getFiatDisplayString(
                        amount.multiply(getExchangeRates().toRate).toDouble(),
                        currencyHelper.fiatUnit,
                        view.locale
                )
        )
    }

    private fun updateToFields(toAmount: Double) {
        val amount = toAmount.toBigDecimal().max(BigDecimal.ZERO)
        view.updateToCryptoText(amount.toStrippedString())
        view.updateToFiatText(
                monetaryUtil.getFiatDisplayString(
                        amount.multiply(getExchangeRates().toRate).toDouble(),
                        currencyHelper.fiatUnit,
                        view.locale
                )
        )
    }

    private fun updateFromFields(fromAmount: Double) {
        val amount = fromAmount.toBigDecimal().max(BigDecimal.ZERO)
        view.updateFromCryptoText(amount.toStrippedString())
        view.updateFromFiatText(
                monetaryUtil.getFiatDisplayString(
                        amount.multiply(getExchangeRates().fromRate).toDouble(),
                        currencyHelper.fiatUnit,
                        view.locale
                )
        )
    }
    //endregion

    //region Observables
    private fun getFeesObservable(selectedCurrency: CryptoCurrencies) = when (selectedCurrency) {
        CryptoCurrencies.BTC -> feeDataManager.btcFeeOptions
                .doOnSubscribe { feeOptions = dynamicFeeCache.btcFeeOptions!! }
                .doOnNext { dynamicFeeCache.btcFeeOptions = it }

        CryptoCurrencies.ETHER -> feeDataManager.ethFeeOptions
                .doOnSubscribe { feeOptions = dynamicFeeCache.ethFeeOptions!! }
                .doOnNext { dynamicFeeCache.ethFeeOptions = it }

        else -> throw IllegalArgumentException("BCC is not currently supported")
    }

    private fun getMarketInfoObservable(selectedCurrency: CryptoCurrencies) = when (selectedCurrency) {
        CryptoCurrencies.BTC -> shapeShiftDataManager.getRate(CoinPairings.BTC_TO_ETH)
        CryptoCurrencies.ETHER -> shapeShiftDataManager.getRate(CoinPairings.ETH_TO_BTC)
        else -> throw IllegalArgumentException("BCC is not currently supported")
    }

    private fun getQuoteObservable(quoteRequest: QuoteRequest, selectedCurrency: CryptoCurrencies) =
            shapeShiftDataManager.getQuote(quoteRequest)
                    .compose(RxUtil.addObservableToCompositeDisposable(this))
                    .map {
                        when (it) {
                            is Either.Left<String> -> {
                                // Show error in UI
                                view.showAmountError(it.value)
                                return@map Quote().apply {
                                    quotedRate = 1.0
                                    minerFee = 0.0
                                }
                            }
                            is Either.Right<Quote> -> return@map it.value
                        }
                    }
                    .doOnNext {
                        latestQuote = it
                        shapeShiftData = ShapeShiftData(
                                selectedCurrency,
                                if (selectedCurrency == CryptoCurrencies.BTC) CryptoCurrencies.ETHER else CryptoCurrencies.BTC,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                it.quotedRate,
                                BigDecimal.ZERO,
                                BigDecimal.valueOf(it.minerFee),
                                "",
                                ""
                        )
                    }.doOnTerminate { view.showQuoteInProgress(false) }

    private fun PublishSubject<String>.applyDefaults(): Observable<BigDecimal> {
        return this.doOnNext {
            view.clearError()
            view.setButtonEnabled(false)
            view.showQuoteInProgress(true)
        }.debounce(500, TimeUnit.MILLISECONDS)
                .onErrorReturn { "" }
                .map { BigDecimal(it.sanitise()) }
                .onErrorReturn { BigDecimal.ZERO }
                .compose(RxUtil.applySchedulersToObservable())
    }
    //endregion

    private fun String.sanitise() = if (isNotEmpty()) this else "0"

    private fun Double.toBigDecimal() = BigDecimal.valueOf(this)

    private fun BigDecimal.toStrippedString() = stripTrailingZeros().toPlainString()

    private data class ExchangeRates(val toRate: BigDecimal, val fromRate: BigDecimal)

}