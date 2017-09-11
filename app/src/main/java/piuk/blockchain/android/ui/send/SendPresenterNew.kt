package piuk.blockchain.android.ui.send

import android.text.Editable
import android.widget.EditText
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.datamanagers.FeeDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.payments.SendDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.receive.ReceiveCurrencyHelper
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.util.*
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.io.UnsupportedEncodingException
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import javax.inject.Inject

class SendPresenterNew @Inject constructor(
        private val walletAccountHelper: WalletAccountHelper,
        private val payloadDataManager: PayloadDataManager,
        private val currencyState: CurrencyState,
        private val ethDataManager: EthDataManager,
        private val prefsUtil: PrefsUtil,
        private val exchangeRateFactory: ExchangeRateFactory,
        private val stringUtils: StringUtils,
        private val sendDataManager: SendDataManager,
        private val dynamicFeeCache: DynamicFeeCache,
        private val feeDataManager: FeeDataManager
) : BasePresenter<SendViewNew>() {

    val locale by unsafeLazy { Locale.getDefault() }
    val currencyHelper by unsafeLazy { ReceiveCurrencyHelper(monetaryUtil, locale, prefsUtil, exchangeRateFactory, currencyState) }
    val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(getBtcUnitType()) }

    lateinit var feeOptions: FeeOptions
    val pendingTransaction: PendingTransaction by unsafeLazy { PendingTransaction() }
    val unspentApiResponses: HashMap<String, UnspentOutputs> by unsafeLazy { HashMap<String, UnspentOutputs>() }
    var unspentApiDisposable: Disposable = compositeDisposable
    var absoluteSuggestedFee = BigInteger.ZERO
    var maxAvailable = BigInteger.ZERO

    private var metricInputFlag: String? = null

    private fun getBtcUnitType() = prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)

    override fun onViewReady() {
    }

    fun onContinue() {

    }

    internal fun resetAccountList() {
        val list = getAddressList()
        if (list.size == 1) {
            view.hideReceivingDropdown()
            view.hideSendingFieldDropdown()
            setReceiveHint(list.size)
        } else {
            view.showSendingFieldDropdown()
            view.showReceivingDropdown()
            setReceiveHint(list.size)
        }
        selectDefaultSendingAccount()
    }

    fun onBitcoinChosen() {
        Timber.d("onBitcoinChosen")
        currencyState.cryptoCurrency = CryptoCurrencies.BTC
        view.selectTab(0)
        absoluteSuggestedFee = BigInteger.ZERO
        view.updateFeeField("")
        resetAccountList()
        view.hideMaxAvailable()
        view.resetAmounts()
        setCryptoCurrency()
        calculateTransactionAmounts(spendAll = false, amountToSendText = "0", feePriority = FeeType.FEE_OPTION_REGULAR)
        view.showFeePriority()
    }

    fun onEtherChosen() {
        Timber.d("onEtherChosen")
        currencyState.cryptoCurrency = CryptoCurrencies.ETHER
        view.selectTab(1)
        absoluteSuggestedFee = BigInteger.ZERO
        view.updateFeeField("")
        resetAccountList()
        view.hideMaxAvailable()
        view.resetAmounts()
        setCryptoCurrency()
        calculateTransactionAmounts(spendAll = false, amountToSendText = "0", feePriority = FeeType.FEE_OPTION_REGULAR)
        view.hideFeePriority()
    }

    fun clearReceivingAddress() {
    }

    fun clearContact() {
    }

    internal fun getAddressList(): List<ItemAccount> = walletAccountHelper.getAccountItems()

    fun setReceiveHint(accountsCount: Int) {

        var hint: Int

        if (accountsCount > 1) {
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
        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> view.setCryptoCurrency(currencyHelper.btcUnit)
            else -> view.setCryptoCurrency(currencyHelper.ethUnit)
        }
    }

    fun selectDefaultSendingAccount() {
        val accountItem = walletAccountHelper.getDefaultAccount()
        view.setSendingAddress(accountItem)
        pendingTransaction.sendingObject = accountItem
    }

    fun selectSendingBtcAccount(accountPosition: Int) {

        if (accountPosition >= 0) {
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

        val maxLength = 2
        var fiat = EditTextFormatUtil.formatEditable(editable,
                maxLength,
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

        var crypto = EditTextFormatUtil.formatEditable(editable,
                currencyHelper.maxCryptoDecimalLength,
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

    fun onSpendAllClicked(feePriority: Int) {
        calculateTransactionAmounts(spendAll = true,
                amountToSendText = null,
                feePriority = feePriority)
    }

    /**
     * Get cached dynamic fee from new Fee options endpoint
     */
    private fun getSuggestedFee() {

        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> {
                feeOptions = dynamicFeeCache.btcFeeOptions!!
                // Refresh fee cache
                compositeDisposable.add(
                        feeDataManager.btcFeeOptions
                                .doOnError({ ignored ->
                                    view.showToast(R.string.confirm_payment_fee_sync_error, ToastCustom.TYPE_ERROR)
                                    view.finishPage(false)
                                })
                                .doOnTerminate({ feeOptions = dynamicFeeCache.getBtcFeeOptions()!! })
                                .subscribe({ feeOptions -> dynamicFeeCache.btcFeeOptions = feeOptions },{ it.printStackTrace() }))
            }
            else -> {
                feeOptions = dynamicFeeCache.ethFeeOptions!!
                // Refresh fee cache
                compositeDisposable.add(
                        feeDataManager.ethFeeOptions
                                .doOnError({ ignored ->
                                    view.showToast(R.string.confirm_payment_fee_sync_error, ToastCustom.TYPE_ERROR)
                                    view.finishPage(false)
                                })
                                .doOnTerminate({ feeOptions = dynamicFeeCache.ethFeeOptions!! })
                                .subscribe({ feeOptions -> dynamicFeeCache.ethFeeOptions = feeOptions },{ it.printStackTrace() }))

            }
        }
    }

    internal fun getFeeOptionsForDropDown(): List<DisplayFeeOptions> {
        val regular = DisplayFeeOptions(
                stringUtils.getString(R.string.fee_options_regular),
                stringUtils.getString(R.string.fee_options_regular_time))
        val priority = DisplayFeeOptions(
                stringUtils.getString(R.string.fee_options_priority),
                stringUtils.getString(R.string.fee_options_priority_time))
        val custom = DisplayFeeOptions(
                stringUtils.getString(R.string.fee_options_custom),
                stringUtils.getString(R.string.fee_options_custom_warning))
        return Arrays.asList(regular, priority, custom)
    }

    internal fun getFeePerKbFromPriority(@FeeType.FeePriorityDef feePriorityTemp: Int): BigInteger {

        getSuggestedFee()

        if (feeOptions == null) {
            // This is a stopgap in case of failure to prevent crashes.
            return BigInteger.ZERO
        }

        when (feePriorityTemp) {
            FeeType.FEE_OPTION_CUSTOM -> return BigInteger.valueOf(view.getCustomFeeValue() * 1000)
            FeeType.FEE_OPTION_PRIORITY -> return BigInteger.valueOf(feeOptions.getPriorityFee() * 1000)
            else -> return BigInteger.valueOf(feeOptions.getRegularFee() * 1000)
        }
    }

    /**
     * Retrieves unspent api data in memory. If not in memory yet, it will be retrieved and added.
     */
    private fun getUnspentApiResponse(address: String): Observable<UnspentOutputs> {
        if (payloadDataManager.getAddressBalance(address).toLong() > 0) {
            if (unspentApiResponses.containsKey(address)) {
                return Observable.just(unspentApiResponses.get(address))
            } else {
                return sendDataManager.getUnspentOutputs(address)
            }
        } else {
            return Observable.error(Throwable("No funds - skipping call to unspent API"))
        }
    }

    private fun stripSeparator(text: String): String {
        return text.trim { it <= ' ' }
                .replace(" ", "")
                .replace(getDefaultDecimalSeparator(), ".")
    }

    /**
     * Returns amount of satoshis from btc amount. This could be btc, mbtc or bits.
     *
     * @return satoshis
     */
    private fun getSatoshisFromText(text: String?): BigInteger {
        if (text == null || text.isEmpty()) return BigInteger.ZERO

        val amountToSend = stripSeparator(text)

        var amount: Double
        try {
            amount = java.lang.Double.parseDouble(amountToSend)
        } catch (nfe: NumberFormatException) {
            amount = 0.0
        }

        val amountL = BigDecimal.valueOf(monetaryUtil.getUndenominatedAmount(amount))
                .multiply(BigDecimal.valueOf(100000000))
                .toLong()
        return BigInteger.valueOf(amountL)
    }

    @Throws(UnsupportedEncodingException::class)
    private fun getSuggestedAbsoluteFee(coins: UnspentOutputs, amountToSend: BigInteger, feePerKb: BigInteger): BigInteger {
        val spendableCoins = sendDataManager.getSpendableCoins(coins, amountToSend, feePerKb)
        return spendableCoins.absoluteFee
    }

    /**
     * Update absolute fee with smallest dinomination of crypto currency (satoshi, wei, etc)
     */
    private fun updateFee(fee: BigInteger) {

        absoluteSuggestedFee = fee

        var cryptoPrice = "0"
        var fiatPrice = "0"

        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> {
                cryptoPrice = monetaryUtil.getDisplayAmount(absoluteSuggestedFee.toLong())
                fiatPrice = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit)
                        .format(currencyHelper.lastPrice * (absoluteSuggestedFee.toDouble() / 1e8))
            }
            else -> {
                val eth = Convert.fromWei(absoluteSuggestedFee.toString(), Convert.Unit.ETHER)
                cryptoPrice = eth.toString()
                fiatPrice = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit)
                        .format(currencyHelper.lastPrice * (eth.toDouble()))
            }
        }

        view.updateFeeField(
                cryptoPrice
                + " "
                + currencyHelper.cryptoUnit
                + " ("
                + fiatPrice
                + currencyHelper.fiatUnit
                + ")")
    }

    private fun updateMaxAvailable(balanceAfterFee: BigInteger) {
        maxAvailable = balanceAfterFee
        view.showMaxAvailable()

        //Format for display
        if (!currencyState.isDisplayingCryptoCurrency) {
            val fiatBalance = currencyHelper.lastPrice * (Math.max(balanceAfterFee.toDouble(), 0.0) / 1e8)
            val fiatBalanceFormatted = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit).format(fiatBalance)
            view.setMaxAvailable(stringUtils.getString(R.string.max_available) + " " + fiatBalanceFormatted + " " + currencyHelper.fiatUnit)
        } else {
            val btcAmountFormatted = monetaryUtil.getBtcFormat().format(monetaryUtil.getDenominatedAmount(Math.max(balanceAfterFee.toDouble(), 0.0) / 1e8))
            view.setMaxAvailable(stringUtils.getString(R.string.max_available) + " " + btcAmountFormatted + " " + currencyHelper.cryptoUnit)
        }

        if (balanceAfterFee.compareTo(Payment.DUST) <= 0) {
            view.setMaxAvailable(stringUtils.getString(R.string.insufficient_funds))
            view.setMaxAvailableColor(R.color.product_red_medium)
        } else {
            view.setMaxAvailableColor(R.color.primary_blue_accent)
        }
    }

    /**
     *
     * Fetches unspent data Gets spendable coins Mixed checks and updates
     */
    fun calculateTransactionAmounts(spendAll: Boolean,
                                    amountToSendText: String?,
                                    @FeeType.FeePriorityDef feePriority: Int) {

        view.hideMaxAvailable()
        view.setUnconfirmedFunds("")

        val feePerKb = getFeePerKbFromPriority(feePriority)

        when(currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> {
                calculateUnspentBtc(spendAll, amountToSendText, feePerKb)
            }
            else -> {
                calculateUnspentEth()
            }
        }
    }

    fun calculateUnspentBtc(spendAll: Boolean, amountToSendText: String?, feePerKb: BigInteger) {

        val address = pendingTransaction.sendingObject.getAddressString()

        if (unspentApiDisposable != null) unspentApiDisposable.dispose()
        unspentApiDisposable = getUnspentApiResponse(address)
                .compose(RxUtil.applySchedulersToObservable<UnspentOutputs>())
                .subscribe(
                        { coins ->
                            val amountToSend = getSatoshisFromText(amountToSendText)

                            // Future use. There might be some unconfirmed funds. Not displaying a warning currently (to line up with iOS and Web wallet)
                            view.setUnconfirmedFunds(if (coins.getNotice() != null) coins.getNotice() else "")
                            updateFee(getSuggestedAbsoluteFee(coins, amountToSend, feePerKb))

                            suggestedFeePayment(coins, amountToSend, spendAll, feePerKb)

                        }, { throwable ->
                    Timber.e(throwable)
                    // No unspent outputs
                    updateMaxAvailable(BigInteger.ZERO)
                    updateFee(BigInteger.ZERO)
                    pendingTransaction.unspentOutputBundle = null
                })
    }

    /**
     * Payment will use suggested dynamic fee
     */
    @Throws(UnsupportedEncodingException::class)
    private fun suggestedFeePayment(coins: UnspentOutputs, amountToSend: BigInteger, spendAll: Boolean, feePerKb: BigInteger) {
        var amountToSend = amountToSend

        //Calculate sweepable amount to display max available
        val sweepBundle = sendDataManager.getSweepableCoins(coins, feePerKb)
        val sweepableAmount = sweepBundle.left

        updateMaxAvailable(sweepableAmount)

        if (spendAll) {
            amountToSend = sweepableAmount
            if (view != null) {
                view.setSpendAllAmount(currencyHelper.getTextFromSatoshis(sweepableAmount.toLong(), getDefaultDecimalSeparator()))
            }
        }

        val unspentOutputBundle = sendDataManager.getSpendableCoins(coins,
                amountToSend,
                feePerKb)

        pendingTransaction.bigIntAmount = amountToSend
        pendingTransaction.unspentOutputBundle = unspentOutputBundle
        pendingTransaction.bigIntFee = pendingTransaction.unspentOutputBundle.getAbsoluteFee()
    }

    fun calculateUnspentEth() {

        view.showMaxAvailable()

        val gwei = BigDecimal.valueOf(feeOptions.gasLimit * feeOptions.regularFee)
        val wei = Convert.toWei(gwei, Convert.Unit.GWEI)
        updateFee(wei.toBigInteger())

        val ethR = ethDataManager.getEthAddress()
        // STOPSHIP: Here we need to get the non-legacy address and then get the balance of the standalone object, not the combined balance
        // #getTotalBalance is wrong; it's just a placeholder for compiling's sake.
        maxAvailable = ethR!!.getTotalBalance().minus(wei.toBigInteger())

        val availableEth = Convert.fromWei(maxAvailable.toString(), Convert.Unit.ETHER)

        //Format for display
        if (!currencyState.isDisplayingCryptoCurrency) {

            val fiatBalance = currencyHelper.lastPrice * availableEth.toLong()

            val fiatBalanceFormatted = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit).format(fiatBalance)
            view.setMaxAvailable(stringUtils.getString(R.string.max_available) + " " + fiatBalanceFormatted + " " + currencyHelper.fiatUnit)
        } else {
            val number = DecimalFormat.getInstance().apply { maximumFractionDigits = 16 }
                    .run { format(availableEth.toDouble()) }
            view.setMaxAvailable(stringUtils.getString(R.string.max_available) + " " + number)
        }

        if (maxAvailable.compareTo(Payment.DUST) <= 0) {
            view.setMaxAvailable(stringUtils.getString(R.string.insufficient_funds))
            view.setMaxAvailableColor(R.color.product_red_medium)
        } else {
            view.setMaxAvailableColor(R.color.primary_blue_accent)
        }

    }

    fun handleURIScan(untrimmedscanData: String, scanRoute: String) {
        metricInputFlag = scanRoute

        var scanData = untrimmedscanData.trim { it <= ' ' }
        var address = ""
        var amount: String? = null

        scanData = FormatsUtil.getURIFromPoorlyFormedBIP21(scanData)

        if (FormatsUtil.isValidBitcoinAddress(scanData)) {
            onBitcoinChosen()
            address = scanData
        } else if (FormatsUtil.isBitcoinUri(scanData)) {
            onBitcoinChosen()
            address = FormatsUtil.getBitcoinAddress(scanData)
            amount = FormatsUtil.getBitcoinAmount(scanData)

            // QR scan comes in as BTC - set current btc unit
            prefsUtil.setValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)

            //Convert to correct units
            try {
                amount = monetaryUtil.getDisplayAmount(java.lang.Long.parseLong(amount))
                view?.updateCryptoTextField(amount)
            } catch (e: Exception) {
                //ignore
            }

        } else if(FormatsUtil.isValidEthereumAddress(scanData)){
            onEtherChosen()
            address = scanData
            view?.updateCryptoTextField("")
        } else {
            view.showToast(R.string.invalid_bitcoin_address, ToastCustom.TYPE_ERROR)
            return
        }

        if (address != "") {
            pendingTransaction.receivingObject = null
            pendingTransaction.receivingAddress = address
            view.setReceivingAddress(address)
        }
    }

    fun handlePrivxScan(scanData: String) {

    }
}