package piuk.blockchain.android.ui.shapeshift.newexchange

import info.blockchain.wallet.shapeshift.data.MarketInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.ethereum.EthDataStore
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.shapeshift.CoinPairings
import piuk.blockchain.android.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.receive.ReceiveCurrencyHelper
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import javax.inject.Inject

// For type safety in methods
typealias ToAmount = String
typealias FromAmount = String

class NewExchangePresenter @Inject constructor(
        private val payloadDataManager: PayloadDataManager,
        private val ethDataStore: EthDataStore,
        private val prefsUtil: PrefsUtil,
        private val exchangeRateFactory: ExchangeRateFactory,
        private val currencyState: CurrencyState,
        private val shapeShiftDataManager: ShapeShiftDataManager,
        private val walletAccountHelper: WalletAccountHelper,
        private val stringUtils: StringUtils
) : BasePresenter<NewExchangeView>() {

    private val btcAccounts = walletAccountHelper.getHdAccounts()
    private val monetaryUtil by unsafeLazy {
        MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
    }
    private val currencyHelper by unsafeLazy {
        ReceiveCurrencyHelper(monetaryUtil, Locale.getDefault(), prefsUtil, exchangeRateFactory, currencyState)
    }
    private var marketInfo: MarketInfo? = null

    override fun onViewReady() {
        val selectedCurrency = currencyState.cryptoCurrency
        view.updateUi(
                selectedCurrency,
                btcAccounts.size > 1,
                if (selectedCurrency == CryptoCurrencies.BTC) getBtcLabel() else getEthLabel(),
                if (selectedCurrency == CryptoCurrencies.ETHER) getBtcLabel() else getEthLabel(),
                monetaryUtil.getFiatDisplayString(0.0, currencyHelper.fiatUnit, Locale.getDefault())
        )

        val observable = when (selectedCurrency) {
            CryptoCurrencies.BTC -> shapeShiftDataManager.getRate(CoinPairings.BTC_TO_ETH)
            CryptoCurrencies.ETHER -> shapeShiftDataManager.getRate(CoinPairings.ETH_TO_BTC)
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }

        observable
                .doOnSubscribe { view.showProgressDialog(R.string.shapeshift_getting_information) }
                .doOnTerminate { view.dismissProgressDialog() }
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(
                        {
                            marketInfo = it
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
        // TODO:
    }

    internal fun onMaxPressed() {
        // TODO: Calculate max available crypto, calculate if greater or lesser than [MarketInfo#max]
        val maximum = BigDecimal.valueOf(marketInfo?.maxLimit ?: 0.0)
    }

    internal fun onMinPressed() {
        val minimum = BigDecimal.valueOf(marketInfo?.minimum ?: 0.0)
        // TODO: Some other stuff
    }

    internal fun onFromChooserClicked() {
        view.launchAccountChooserActivityFrom()
    }

    internal fun onToChooserClicked() {
        view.launchAccountChooserActivityTo()
    }

    internal fun onFromCryptoValueChanged(value: String) {
        val sanitisedValue = if (value.isNotEmpty()) value else "0"
        val fromAmount = BigDecimal(sanitisedValue)
        // Multiply by conversion rate
        val convertedAmount = fromAmount.multiply(BigDecimal.valueOf(marketInfo?.rate ?: 1.0))
        // Final amount is converted amount lesser miner's fees
        val toAmount = compareAmountsToZero(convertedAmount, BigDecimal::minus)

        view.updateToCryptoText(toAmount.toPlainString())
        handleFiatUpdatesFromCrypto(toAmount, fromAmount)
    }

    internal fun onToCryptoValueChanged(value: String) {
        val sanitisedValue = if (value.isNotEmpty()) value else "0"
        val toAmount = BigDecimal(sanitisedValue)
        // Multiply by conversion rate
        val convertedAmount = toAmount.divide(
                BigDecimal.valueOf(marketInfo?.rate ?: 1.0),
                8,
                RoundingMode.HALF_UP
        ).stripTrailingZeros()
        // Final amount is converted amount plus miner's fees
        val fromAmount = compareAmountsToZero(convertedAmount, BigDecimal::plus)

        view.updateFromCryptoText(fromAmount.toPlainString())
        handleFiatUpdatesFromCrypto(toAmount, fromAmount)
    }

    internal fun onFromFiatValueChanged(value: String) {
        val sanitisedValue = if (value.isNotEmpty()) value else "0"
        val fromAmount = BigDecimal(sanitisedValue)
        // Work out amount of crypto
        val currencyCode = currencyHelper.fiatUnit
        val fromExchangeRate = when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> exchangeRateFactory.getLastBtcPrice(currencyCode)
            CryptoCurrencies.ETHER -> exchangeRateFactory.getLastEthPrice(currencyCode)
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }
        val fromCrypto = fromAmount.divide(
                BigDecimal.valueOf(fromExchangeRate),
                18,
                RoundingMode.HALF_UP
        )
        // Convert to to_crypto
        val toCrypto = fromCrypto.multiply(BigDecimal.valueOf(marketInfo?.rate ?: 1.0))
        // Subtract fee
        val toCryptoMinusFee = compareAmountsToZero(toCrypto, BigDecimal::minus)
        // Convert that to to_fiat
        val toExchangeRate = when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> exchangeRateFactory.getLastEthPrice(currencyCode)
            CryptoCurrencies.ETHER -> exchangeRateFactory.getLastBtcPrice(currencyCode)
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }
        val toFiat = toCryptoMinusFee.multiply(BigDecimal.valueOf(toExchangeRate))
        // Update UI
        view.updateFromCryptoText(fromCrypto.stripTrailingZeros().toPlainString())
        view.updateToCryptoText(toCryptoMinusFee.stripTrailingZeros().toPlainString())
        view.updateToFiatText(
                monetaryUtil.getFiatDisplayString(
                        toFiat.toDouble(),
                        currencyHelper.fiatUnit,
                        view.locale
                )
        )
    }

    internal fun onToFiatValueChanged(value: String) {
        val sanitisedValue = if (value.isNotEmpty()) value else "0"
        val toAmount = BigDecimal(sanitisedValue)
        // Work out amount of crypto
        val currencyCode = currencyHelper.fiatUnit
        val toExchangeRate = when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> exchangeRateFactory.getLastEthPrice(currencyCode)
            CryptoCurrencies.ETHER -> exchangeRateFactory.getLastBtcPrice(currencyCode)
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }
        val toCrypto = toAmount.divide(
                BigDecimal.valueOf(toExchangeRate),
                18,
                RoundingMode.HALF_UP
        )
        // Convert to from_crypto
        val fromCrypto = toCrypto.divide(
                BigDecimal.valueOf(marketInfo?.rate ?: 1.0),
                18,
                RoundingMode.HALF_UP
        ).stripTrailingZeros()
        // Add fee
        val fromCryptoPlusFee = compareAmountsToZero(fromCrypto, BigDecimal::plus)
        // Convert that to from_fiat
        val fromExchangeRate = when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> exchangeRateFactory.getLastBtcPrice(currencyCode)
            CryptoCurrencies.ETHER -> exchangeRateFactory.getLastEthPrice(currencyCode)
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }
        val fromFiat = fromCryptoPlusFee.multiply(BigDecimal.valueOf(fromExchangeRate))
        // Update UI
        view.updateToCryptoText(toCrypto.stripTrailingZeros().toPlainString())
        view.updateFromCryptoText(fromCryptoPlusFee.stripTrailingZeros().toPlainString())
        view.updateFromFiatText(
                monetaryUtil.getFiatDisplayString(
                        fromFiat.toDouble(),
                        currencyHelper.fiatUnit,
                        view.locale
                )
        )
    }

    private fun handleFiatUpdatesFromCrypto(toAmount: BigDecimal, fromAmount: BigDecimal) {
        val currencyCode = currencyHelper.fiatUnit
        val lastBtcRate = exchangeRateFactory.getLastBtcPrice(currencyCode)
        val lastEthRate = exchangeRateFactory.getLastEthPrice(currencyCode)

        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> updateFiatViews(toAmount, fromAmount, lastEthRate, lastBtcRate)
            CryptoCurrencies.ETHER -> updateFiatViews(toAmount, fromAmount, lastBtcRate, lastEthRate)
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }
    }

    private fun updateFiatViews(
            toAmount: BigDecimal,
            fromAmount: BigDecimal,
            toRate: Double,
            fromRate: Double
    ) {
        view.updateToFiatText(
                monetaryUtil.getFiatDisplayString(
                        toAmount.multiply(BigDecimal.valueOf(toRate)).toDouble(),
                        currencyHelper.fiatUnit,
                        view.locale
                )
        )
        view.updateFromFiatText(
                monetaryUtil.getFiatDisplayString(
                        fromAmount.multiply(BigDecimal.valueOf(fromRate)).toDouble(),
                        currencyHelper.fiatUnit,
                        view.locale
                )
        )
    }

    private fun getBtcLabel(): String {
        return if (btcAccounts.size > 1) {
            payloadDataManager.defaultAccount.label
        } else {
            stringUtils.getString(R.string.shapeshift_btc)
        }
    }

    private fun compareAmountsToZero(
            amount: BigDecimal,
            operator: BigDecimal.(BigDecimal) -> BigDecimal
    ): BigDecimal {

        val fee = BigDecimal.valueOf(marketInfo?.minerFee ?: 0.0)
        val amountToReturn = amount.operator(fee).stripTrailingZeros()

        return when {
            amountToReturn <= BigDecimal.ZERO -> BigDecimal.ZERO
            amount <= BigDecimal.ZERO -> BigDecimal.ZERO
            else -> amountToReturn
        }
    }

    private fun getEthLabel() = stringUtils.getString(R.string.shapeshift_eth)

    // Changes depending on chosen account type - this is horrible and needs to be passed
    // to the method instead of having an outside dependency on some stored state
    private fun getAccountList() = walletAccountHelper.getAccountItems()

    ///////////////////////////////////////////////////////////////////////////
    // Formatting stuff, to be checked and deleted if possible
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Gets device's specified locale decimal separator
     *
     * @return decimal separator
     */
    private fun getDefaultDecimalSeparator(): String {
        val format = DecimalFormat.getInstance(Locale.getDefault()) as DecimalFormat
        return Character.toString(format.decimalFormatSymbols.decimalSeparator)
    }

    private fun stripSeparator(text: String): String {
        return text.trim { it <= ' ' }
                .replace(" ", "")
                .replace(getDefaultDecimalSeparator(), ".")
    }

    private fun String.toBigDecimal(value: String) = BigDecimal(stripSeparator(value))

}