package piuk.blockchain.android.ui.send

import android.text.Editable
import android.widget.EditText
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_send.*
import kotlinx.android.synthetic.main.include_amount_row.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.receive.ReceiveCurrencyHelper
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.util.EditTextFormatUtil
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.text.DecimalFormatSymbols
import java.util.*
import javax.inject.Inject

class SendPresenterNew @Inject constructor(
        private val walletAccountHelper: WalletAccountHelper,
        private val payloadDataManager: PayloadDataManager,
        private val currencyState: CurrencyState,
        private val ethDataManager: EthDataManager,
        private val prefsUtil: PrefsUtil,
        private val exchangeRateFactory: ExchangeRateFactory
) : BasePresenter<SendViewNew>() {


    private val locale by unsafeLazy { Locale.getDefault() }
    val currencyHelper by unsafeLazy { ReceiveCurrencyHelper(monetaryUtil, locale, prefsUtil, exchangeRateFactory, currencyState) }
    private val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(getBtcUnitType()) }

    private fun getBtcUnitType() = prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)


    override fun onViewReady() {
    }

    fun onContinue() {
    }

    internal fun resetAccountList() {
        val list = getAddressList()
        if(list.size == 1) {
            view.hideReceivingDropdown()
            view.hideSendingFieldDropdown()
            setReceiveHint(list.size)
        } else {
            view.showSendingFieldDropdown()
            view.showReceivingDropdown()
            setReceiveHint(list.size)
        }
        setCryptoCurrency()

        selectDefaultSendingAccount()
    }

    fun onBitcoinChosen() {
        currencyState.cryptoCurrency = CryptoCurrencies.BTC
        resetAccountList()
        view.resetAmounts()
    }

    fun onEtherChosen() {
        currencyState.cryptoCurrency = CryptoCurrencies.ETHER
        resetAccountList()
        view.resetAmounts()
    }

    fun clearReceivingAddress() {
    }

    fun clearContact() {
    }

    internal fun getAddressList(): List<ItemAccount> = walletAccountHelper.getAccountItems()

    fun setReceiveHint(accountsCount: Int) {

        var hint: Int

        if(accountsCount > 1) {
            when (currencyState.cryptoCurrency) {
                CryptoCurrencies.BTC -> hint = R.string.to_field_helper
                else -> hint = R.string.eth_to_field_helper
            }
        } else {
            when (currencyState.cryptoCurrency) {
                CryptoCurrencies.BTC -> hint = R.string.to_field_helper_no_dropdown
                else -> hint = R.string.eth_to_field_helper_no_dropdown
            }
        }

        view.setReceivingHint(hint)
    }

    fun setCryptoCurrency() {
        view.setCryptoCurrency("ETH")
    }

    fun selectDefaultSendingAccount() {
        val accountItem = walletAccountHelper.getDefaultAccount()
        view.setSendingAddress(accountItem)
    }

    fun selectSendingBtcAccount(accountPosition: Int) {

        if(accountPosition >= 0) {
            view.setSendingAddress(getAddressList().get(accountPosition))
        } else {
            selectDefaultSendingAccount()
        }
    }

    internal fun getDefaultBtcAccount(): Int {
        return getListIndexFromAccountIndex(payloadDataManager.defaultAccountIndex)
    }

    internal fun getListIndexFromAccountIndex(accountIndex: Int): Int {
        return payloadDataManager.getPositionOfAccountFromActiveList(accountIndex)
    }

    internal fun getDefaultDecimalSeparator(): String {
        return DecimalFormatSymbols.getInstance().decimalSeparator.toString()
    }

    fun updateCryptoTextField(editable: Editable, editText: EditText) {

        var fiat = EditTextFormatUtil.formatEditable(editable,
                currencyHelper.maxCryptoDecimalLength,
                editText,
                getDefaultDecimalSeparator()).toString()
        var amountString = ""

        if (!fiat.isEmpty()) {
            val fiatAmount = currencyHelper.getDoubleAmount(fiat)
            amountString = currencyHelper.getFormattedCryptoStringFromFiat(fiatAmount)
        }

        view.disableCryptoTextChangeListener()
        view.updateCryptoTextField(amountString)
        view.enableCryptoTextChangeListener()
    }

    fun updateFiatTextField(editable: Editable, editText: EditText) {

        val maxLength = 2
        var crypto = EditTextFormatUtil.formatEditable(editable,
                maxLength,
                editText,
                getDefaultDecimalSeparator()).toString()

        var amountString = ""

        if (!crypto.isEmpty()) {
            val cd = currencyHelper.getDoubleAmount(crypto)
            amountString = currencyHelper.getFormattedFiatStringFromCrypto(cd)
        }

        view.disableFiatTextChangeListener()
        view.updateFiatTextField(amountString)
        view.enableFiatTextChangeListener()
    }
}