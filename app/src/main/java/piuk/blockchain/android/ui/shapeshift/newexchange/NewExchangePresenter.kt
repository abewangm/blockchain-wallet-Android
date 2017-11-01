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
import java.util.*
import javax.inject.Inject

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
                if (selectedCurrency == CryptoCurrencies.BTC) getDefaultBtcLabel() else getEthLabel(),
                if (selectedCurrency == CryptoCurrencies.ETHER) getDefaultBtcLabel() else getEthLabel(),
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
        // TODO: Set up parcelable object if all fields are valid
    }

    internal fun onMaxPressed() {
        with(getMaximum()) {
            // TODO: Calculate max available crypto, calculate if greater or lesser than max from API
            view.updateFromCryptoText(this.toPlainString())
            // This fixes focus issues but is a bit of a hack as this method is potentially called twice
            onFromCryptoValueChanged(this.toPlainString())
        }
    }

    internal fun onMinPressed() {
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
        val convertedAmount = toAmount.divide(
                getMarketRate(),
                18,
                RoundingMode.HALF_UP
        )
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

    private fun getExchangeRates(): ExchangeRates {
        val currencyCode = currencyHelper.fiatUnit
        return when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> {
                val to = exchangeRateFactory.getLastEthPrice(currencyCode)
                val from = exchangeRateFactory.getLastBtcPrice(currencyCode)
                ExchangeRates(BigDecimal.valueOf(to), BigDecimal.valueOf(from))
            }
            CryptoCurrencies.ETHER -> {
                val to = exchangeRateFactory.getLastBtcPrice(currencyCode)
                val from = exchangeRateFactory.getLastEthPrice(currencyCode)
                ExchangeRates(BigDecimal.valueOf(to), BigDecimal.valueOf(from))
            }
            else -> throw IllegalArgumentException("BCC is not currently supported")
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

    private fun getEthLabel() = stringUtils.getString(R.string.shapeshift_eth)

    // Changes depending on chosen account type - this is horrible and needs to be passed
    // to the method instead of having an outside dependency on some stored state
    private fun getAccountList() = walletAccountHelper.getAccountItems()

    ///////////////////////////////////////////////////////////////////////////
    // Formatting stuff, to be checked and deleted if possible
    ///////////////////////////////////////////////////////////////////////////

    private fun String.sanitise() = if (isNotEmpty()) this else "0"

    private fun BigDecimal.toStrippedString() = stripTrailingZeros().toPlainString()

    data class ExchangeRates(val toRate: BigDecimal, val fromRate: BigDecimal)

}