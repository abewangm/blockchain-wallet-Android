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
                if (selectedCurrency == CryptoCurrencies.ETHER) getBtcLabel() else getEthLabel()
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
        currencyState.toggleCryptoCurrency().run { onViewReady() }
    }

    internal fun onContinuePressed() {
        // TODO:
    }

    internal fun onMaxPressed() {
        // TODO: Calculate max available crypto, calculate if greater or lesser than [MarketInfo#max]
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
        val amount = BigDecimal(sanitisedValue)
        val toAmount = amount.divide(BigDecimal.valueOf(marketInfo!!.rate), 8, RoundingMode.HALF_UP)
                .stripTrailingZeros()

        view.updateToCryptoText(toAmount.toPlainString())

        val currencyCode = currencyHelper.fiatUnit

        val lastBtcRate = exchangeRateFactory.getLastBtcPrice(currencyCode)
        val lastEthRate = exchangeRateFactory.getLastEthPrice(currencyCode)

        Timber.d("onFromCryptoValueChanged")

        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> {
//                view.updateFromCryptoText(amount.multiply(BigDecimal.valueOf(lastBtcRate)).toPlainString())
//                view.updateToCryptoText(toAmount.multiply(BigDecimal.valueOf(lastEthRate)).toPlainString())
            }
            CryptoCurrencies.ETHER -> {
//                view.updateFromCryptoText(amount.multiply(BigDecimal.valueOf(lastEthRate)).toPlainString())
//                view.updateToCryptoText(toAmount.multiply(BigDecimal.valueOf(lastBtcRate)).toPlainString())
            }
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }
    }

    internal fun onToCryptoValueChanged(value: String) {
        val sanitisedValue = if (value.isNotEmpty()) value else "0"
        val amount = BigDecimal(sanitisedValue)
        val toAmount = amount.multiply(BigDecimal.valueOf(marketInfo!!.rate))
                .stripTrailingZeros()

        view.updateFromCryptoText(toAmount.toPlainString())

        val currencyCode = currencyHelper.fiatUnit

        val lastBtcRate = exchangeRateFactory.getLastBtcPrice(currencyCode)
        val lastEthRate = exchangeRateFactory.getLastEthPrice(currencyCode)

        Timber.d("onToCryptoValueChanged")

        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> {
//                view.updateToCryptoText(toAmount.multiply(BigDecimal.valueOf(lastBtcRate)).toPlainString())
//                view.updateFromCryptoText(amount.multiply(BigDecimal.valueOf(lastEthRate)).toPlainString())
            }
            CryptoCurrencies.ETHER -> {
//                view.updateToCryptoText(toAmount.multiply(BigDecimal.valueOf(lastEthRate)).toPlainString())
//                view.updateFromCryptoText(amount.multiply(BigDecimal.valueOf(lastBtcRate)).toPlainString())
            }
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }
    }

    internal fun onFromFiatValueChanged(value: String) {
        // TODO:
    }

    internal fun onToFiatValueChanged(value: String) {
        // TODO:
    }

    private fun getBtcLabel(): String {
        return if (btcAccounts.size > 1) {
            payloadDataManager.defaultAccount.label
        } else {
            stringUtils.getString(R.string.shapeshift_btc)
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
     * Returns BTC amount from satoshis.
     *
     * @return BTC, mBTC or bits relative to what is set in [MonetaryUtil]
     */
    private fun getTextFromSatoshis(satoshis: Long): String {
        var displayAmount = monetaryUtil.getDisplayAmount(satoshis)
        displayAmount = displayAmount.replace(".", getDefaultDecimalSeparator())
        return displayAmount
    }

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