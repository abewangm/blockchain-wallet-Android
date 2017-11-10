package piuk.blockchain.android.ui.shapeshift.newexchange

import info.blockchain.api.data.UnspentOutputs
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.shapeshift.data.MarketInfo
import io.reactivex.Observable
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
    }

    internal fun onSwitchCurrencyClicked() {
        currencyState.toggleCryptoCurrency()
                .run { view.clearEditTexts() }
                .run { onViewReady() }
    }

    internal fun onContinuePressed() {
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
            // This fixes focus issues but is a bit of a hack as this method is potentially called twice
            onFromCryptoValueChanged(this.toPlainString())
        }
    }

    internal fun onFromChooserClicked() {
        view.launchAccountChooserActivityFrom()
    }

    internal fun onToChooserClicked() {
        view.launchAccountChooserActivityTo()
    }

    internal fun onFromCryptoValueChanged(value: String) {
        val fromAmount = BigDecimal(value.sanitise())
        // Multiply by conversion rate
        val convertedAmount = fromAmount.multiply(getMarketRate())
        // Final amount is converted amount lesser miner's fees
        val toAmount = compareAmountsToZero(convertedAmount, BigDecimal::minus)

        view.updateToCryptoText(toAmount.toStrippedString())
        handleFiatUpdatesFromCrypto(toAmount, fromAmount)
    }

    internal fun onToCryptoValueChanged(value: String) {
        val toAmount = BigDecimal(value.sanitise())
        // Divide by conversion rate
        val convertedAmount = toAmount.divide(getMarketRate(), 18, RoundingMode.HALF_UP)
        // Final amount is converted amount plus miner's fees
        val fromAmount = compareAmountsToZero(convertedAmount, BigDecimal::plus)

        view.updateFromCryptoText(fromAmount.toStrippedString())
        handleFiatUpdatesFromCrypto(toAmount, fromAmount)
    }

    internal fun onFromFiatValueChanged(value: String) {
        val fromAmount = BigDecimal(value.sanitise())
        // Work out amount of crypto
        val (fromExchangeRate, toExchangeRate) = getExchangeRates()
        val fromCrypto = fromAmount.divide(toExchangeRate, 18, RoundingMode.HALF_UP)
        // Convert to toCrypto
        val toCrypto = fromCrypto.multiply(getMarketRate())
        // Subtract fee
        val toCryptoMinusFee = compareAmountsToZero(toCrypto, BigDecimal::minus)
        // Convert that to toFiat
        val toFiat = toCryptoMinusFee.multiply(fromExchangeRate)
        // Update UI
        view.updateFromCryptoText(fromCrypto.toStrippedString())
        view.updateToCryptoText(toCryptoMinusFee.toStrippedString())
        view.updateToFiatText(
                monetaryUtil.getFiatDisplayString(
                        toFiat.toDouble(),
                        currencyHelper.fiatUnit,
                        view.locale
                )
        )
    }

    internal fun onToFiatValueChanged(value: String) {
        val toAmount = BigDecimal(value.sanitise())
        // Work out amount of crypto
        val (fromExchangeRate, toExchangeRate) = getExchangeRates()
        val toCrypto = toAmount.divide(fromExchangeRate, 18, RoundingMode.HALF_UP)
        // Convert to fromCrypto
        val fromCrypto = toCrypto.divide(getMarketRate(), 18, RoundingMode.HALF_UP)
        // Add fee
        val fromCryptoPlusFee = compareAmountsToZero(fromCrypto, BigDecimal::plus)
        // Convert that to fromFiat
        val fromFiat = fromCryptoPlusFee.multiply(toExchangeRate)
        // Update UI
        view.updateToCryptoText(toCrypto.toStrippedString())
        view.updateFromCryptoText(fromCryptoPlusFee.toStrippedString())
        view.updateFromFiatText(
                monetaryUtil.getFiatDisplayString(
                        fromFiat.toDouble(),
                        currencyHelper.fiatUnit,
                        view.locale
                )
        )
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
                            // This fixes focus issues but is a bit of a hack as this method is potentially called twice
                            onFromCryptoValueChanged(maximum.toPlainString())
                        },
                        { throwable ->
                            Timber.e(throwable)
                            // No unspent outputs
                            // TODO:
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
        // This fixes focus issues but is a bit of a hack as this method is potentially called twice
        onFromCryptoValueChanged(maximum.toPlainString())
    }

    private fun getUnspentApiResponse(address: String): Observable<UnspentOutputs> {
        return if (payloadDataManager.getAddressBalance(address).toLong() > 0) {
            sendDataManager.getUnspentOutputs(address)
        } else {
            Observable.error(Throwable("No funds - skipping call to unspent API"))
        }
    }

    private fun handleFiatUpdatesFromCrypto(toAmount: BigDecimal, fromAmount: BigDecimal) {
        val (toRate, fromRate) = getExchangeRates()
        updateFiatViews(toAmount, fromAmount, toRate, fromRate)
    }

    private fun updateFiatViews(
            toAmount: BigDecimal,
            fromAmount: BigDecimal,
            toRate: BigDecimal,
            fromRate: BigDecimal
    ) {
        view.updateToFiatText(
                monetaryUtil.getFiatDisplayString(
                        toAmount.multiply(toRate).toDouble(),
                        currencyHelper.fiatUnit,
                        view.locale
                )
        )
        view.updateFromFiatText(
                monetaryUtil.getFiatDisplayString(
                        fromAmount.multiply(fromRate).toDouble(),
                        currencyHelper.fiatUnit,
                        view.locale
                )
        )
    }

    private fun compareAmountsToZero(
            amount: BigDecimal,
            operator: BigDecimal.(BigDecimal) -> BigDecimal
    ): BigDecimal {

        val fee = BigDecimal.valueOf(marketInfo?.minerFee ?: 0.0)
        val amountToReturn = amount.operator(fee).stripTrailingZeros()

        return when {
            amount <= BigDecimal.ZERO -> BigDecimal.ZERO
            amountToReturn <= BigDecimal.ZERO -> BigDecimal.ZERO
            else -> amountToReturn
        }
    }

    private fun getDefaultBtcLabel(): String {
        return if (btcAccounts.size > 1) {
            payloadDataManager.defaultAccount.label
        } else {
            stringUtils.getString(R.string.shapeshift_btc)
        }
    }

    private fun getMarketRate() = BigDecimal.valueOf(marketInfo?.rate ?: 1.0)

    private fun getMaximum() = BigDecimal.valueOf(marketInfo?.maxLimit ?: 0.0)

    private fun getMinimum() = BigDecimal.valueOf(marketInfo?.minimum ?: 0.0)

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
    //endregion

    private fun String.sanitise() = if (isNotEmpty()) this else "0"

    private fun BigDecimal.toStrippedString() = stripTrailingZeros().toPlainString()

    private data class ExchangeRates(val toRate: BigDecimal, val fromRate: BigDecimal)

}