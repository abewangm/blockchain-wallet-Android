package piuk.blockchain.android.ui.receive

import android.support.annotation.VisibleForTesting
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.util.FormatsUtil
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.uri.BitcoinURI
import piuk.blockchain.android.R
import piuk.blockchain.android.data.api.EnvironmentSettings
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager
import piuk.blockchain.android.data.ethereum.EthDataStore
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat
import java.util.*
import javax.inject.Inject

@Suppress("MemberVisibilityCanPrivate")
class ReceivePresenter @Inject internal constructor(
        private val prefsUtil: PrefsUtil,
        private val stringUtils: StringUtils,
        private val qrCodeDataManager: QrCodeDataManager,
        private val walletAccountHelper: WalletAccountHelper,
        private val payloadDataManager: PayloadDataManager,
        private val exchangeRateFactory: ExchangeRateFactory,
        private val ethDataStore: EthDataStore,
        private val environmentSettings: EnvironmentSettings
) : BasePresenter<ReceiveView>() {

    private val monetaryUtil by unsafeLazy {
        MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
    }
    @VisibleForTesting internal var selectedAddress: String? = null
    @VisibleForTesting internal var selectedContactId: String? = null
    @VisibleForTesting internal var selectedAccount: Account? = null
    internal val currencyHelper by unsafeLazy {
        ReceiveCurrencyHelper(monetaryUtil, Locale.getDefault(), prefsUtil, exchangeRateFactory)
    }

    override fun onViewReady() {
        if (prefsUtil.getValue(PrefsUtil.KEY_CONTACTS_INTRODUCTION_COMPLETE, false)) {
            view.hideContactsIntroduction()
        } else {
            view.showContactsIntroduction()
        }
    }

    internal fun onSendToContactClicked() {
        view.startContactSelectionActivity()
    }

    internal fun isValidAmount(btcAmount: String): Boolean =
            currencyHelper.getLongAmount(btcAmount) > 0

    internal fun getReceiveToList() = ArrayList<ItemAccount>().apply {
        addAll(walletAccountHelper.getAccountItems())
        addAll(walletAccountHelper.getAddressBookEntries())
    }

    internal fun onLegacyAddressSelected(legacyAddress: LegacyAddress) {
        if (legacyAddress.isWatchOnly && shouldWarnWatchOnly()) {
            view.showWatchOnlyWarning()
        }

        selectedAccount = null
        view.updateToAddress(
                if (!legacyAddress.label.isNullOrEmpty())
                    legacyAddress.label
                else
                    legacyAddress.address
        )
        legacyAddress.address.let {
            selectedAddress = it
            view.updateReceiveAddress(it)
            generateQrCode(getBitcoinUri(it, view.getBtcAmount()))
        }
    }

    internal fun onAccountSelected(account: Account) {
        selectedAccount = account
        view.updateToAddress(account.label)
        payloadDataManager.getNextReceiveAddress(account)
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .doOnNext { selectedAddress = it }
                .doOnNext { view.updateReceiveAddress(it) }
                .doOnNext { generateQrCode(getBitcoinUri(it, view.getBtcAmount())) }
                .subscribe(
                        { /* No-op */ },
                        { view.showToast(stringUtils.getString(R.string.unexpected_error), ToastCustom.TYPE_ERROR) })
    }

    internal fun onEthSelected() {
        view.hideBitcoinLayout()
        selectedAccount = null
        ethDataStore.ethAddressResponse!!.account.let {
            selectedAddress = it
            view.updateReceiveAddress(it)
            generateQrCode(it)
        }
    }

    internal fun onSelectDefault(defaultAccountPosition: Int) {
        view.displayBitcoinLayout()
        onAccountSelected(
                if (defaultAccountPosition > -1)
                    payloadDataManager.getAccount(defaultAccountPosition)
                else
                    payloadDataManager.defaultAccount
        )
    }

    internal fun onBitcoinAmountChanged(amount: String) {
        val amountBigInt = getBtcFromString(amount)

        if (currencyHelper.getIfAmountInvalid(amountBigInt)) {
            view.showToast(stringUtils.getString(R.string.invalid_amount), ToastCustom.TYPE_ERROR)
        }

        generateQrCode(getBitcoinUri(selectedAddress!!, amount))
    }

    internal fun getSelectedAccountPosition(): Int =
            payloadDataManager.wallet!!.hdWallets[0].accounts.indexOf(selectedAccount)

    internal fun setWarnWatchOnlySpend(warn: Boolean) {
        prefsUtil.setValue(KEY_WARN_WATCH_ONLY_SPEND, warn)
    }

    internal fun clearSelectedContactId() {
        this.selectedContactId = null
    }

    internal fun getConfirmationDetails(): PaymentConfirmationDetails {
        return PaymentConfirmationDetails().apply {
            val position = getSelectedAccountPosition()
            fromLabel = payloadDataManager.getAccount(position).label
            toLabel = view.getContactName()

            val btcUnit = prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
            val fiatUnit = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
            val exchangeRate = exchangeRateFactory.getLastBtcPrice(fiatUnit)

            val satoshis = getSatoshisFromText(view.getBtcAmount())

            btcAmount = getTextFromSatoshis(satoshis.toLong())
            this.btcUnit = monetaryUtil.getBtcUnit(btcUnit)
            this.fiatUnit = fiatUnit

            fiatAmount = monetaryUtil.getFiatFormat(fiatUnit)
                    .format(exchangeRate * (satoshis.toDouble() / 1e8))

            fiatSymbol = exchangeRateFactory.getSymbol(fiatUnit)
        }
    }

    fun onShowBottomSheetSelected() {
        selectedAddress?.let {
            if (FormatsUtil.isValidBitcoinAddress(it)) {
                view.showBottomSheet(getBitcoinUri(it, view.getBtcAmount()))
            } else if (FormatsUtil.isValidEthereumAddress(it)) {
                view.showBottomSheet(it)
            } else throw IllegalStateException("Unknown address format $selectedAddress")
        }
    }

    // TODO: Test me against valid Segwit address, although we don't currently generate these
    private fun getBitcoinUri(address: String, amount: String): String {
        require(FormatsUtil.isValidBitcoinAddress(address)) {
            "$address is not a valid Bitcoin address"
        }

        val amountBigInt = getBtcFromString(amount)

        return if (amountBigInt != BigInteger.ZERO) {
            BitcoinURI.convertToBitcoinURI(
                    Address.fromBase58(environmentSettings.networkParameters, address),
                    Coin.valueOf(amountBigInt.toLong()),
                    "",
                    ""
            )
        } else {
            "bitcoin:$address"
        }
    }

    private fun getBtcFromString(amount: String): BigInteger {
        val amountLong = currencyHelper.getLongAmount(amount)
        return currencyHelper.getUndenominatedAmount(amountLong)
    }

    private fun generateQrCode(uri: String) {
        view.showQrLoading()
        compositeDisposable.clear()
        qrCodeDataManager.generateQrCode(uri, DIMENSION_QR_CODE)
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(
                        { view.showQrCode(it) },
                        { view.showQrCode(null) })
    }

    internal fun updateFiatTextField(bitcoin: String) {
        var amount = bitcoin
        if (amount.isEmpty()) amount = "0"
        val btcAmount = currencyHelper.getUndenominatedAmount(currencyHelper.getDoubleAmount(amount))
        val fiatAmount = currencyHelper.lastPrice * btcAmount
        view.updateFiatTextField(currencyHelper.getFormattedFiatString(fiatAmount))
    }

    internal fun updateBtcTextField(fiat: String) {
        var amount = fiat
        if (amount.isEmpty()) amount = "0"
        val fiatAmount = currencyHelper.getDoubleAmount(amount)
        val btcAmount = fiatAmount / currencyHelper.lastPrice
        view.updateBtcTextField(currencyHelper.getFormattedBtcString(btcAmount))
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
        val symbols = format.decimalFormatSymbols
        return Character.toString(symbols.decimalSeparator)
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

    private fun shouldWarnWatchOnly() = prefsUtil.getValue(KEY_WARN_WATCH_ONLY_SPEND, true)

    companion object {

        @VisibleForTesting const val KEY_WARN_WATCH_ONLY_SPEND = "warn_watch_only_spend"
        private val DIMENSION_QR_CODE = 600

    }

}
