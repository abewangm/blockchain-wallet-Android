package piuk.blockchain.android.ui.exchange.newexchange

import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.ethereum.EthDataStore
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.receive.ReceiveCurrencyHelper
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat
import java.util.*
import javax.inject.Inject

class NewExchangePresenter @Inject constructor(
        private val payloadDataManager: PayloadDataManager,
        private val ethDataStore: EthDataStore,
        private val prefsUtil: PrefsUtil,
        private val exchangeRateFactory: ExchangeRateFactory,
        private val currencyState: CurrencyState,
        walletAccountHelper: WalletAccountHelper
) : BasePresenter<NewExchangeView>() {

    private val monetaryUtil by unsafeLazy {
        MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
    }
    private val currencyHelper by unsafeLazy {
        ReceiveCurrencyHelper(monetaryUtil, Locale.getDefault(), prefsUtil, exchangeRateFactory, currencyState)
    }
    private val accountList = walletAccountHelper.getAccountItems()

    override fun onViewReady() {
        val selectedCurrency = currencyState.cryptoCurrency
        view.showFrom(selectedCurrency)
        when (selectedCurrency) {
            CryptoCurrencies.BTC -> onSelectDefaultBtc()
            CryptoCurrencies.ETHER -> onEthSelected()
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }
    }

    internal fun onContinuePressed() {
        // TODO:
    }

    internal fun onMaxPressed() {
        // TODO:
    }

    internal fun onMinPressed() {
        // TODO:
    }

    internal fun onFromChooserClicked() {
        // TODO:
    }

    internal fun onToChooserClicked() {
        // TODO:
    }

    private fun onSelectDefaultBtc() {
        compositeDisposable.clear()
    }

    private fun onEthSelected() {
        compositeDisposable.clear()
    }

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

    /**
     * Returns amount of satoshis from btc amount. This could be btc, mbtc or bits.
     *
     * @return satoshis
     */
    private fun getSatoshisFromText(text: String?): BigInteger {
        if (text.isNullOrEmpty()) return BigInteger.ZERO

        val amountToSend = stripSeparator(text!!)

        val amount = try {
            amountToSend.toDouble()
        } catch (nfe: NumberFormatException) {
            0.0
        }

        return BigDecimal.valueOf(monetaryUtil.getUndenominatedAmount(amount))
                .multiply(BigDecimal.valueOf(100000000))
                .toBigInteger()
    }

    private fun stripSeparator(text: String): String {
        return text.trim { it <= ' ' }
                .replace(" ", "")
                .replace(getDefaultDecimalSeparator(), ".")
    }

}