package piuk.blockchain.android.ui.send

import android.content.Intent
import android.text.Editable
import android.widget.EditText
import com.fasterxml.jackson.databind.ObjectMapper
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.wallet.api.WalletApi
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.util.PrivateKeyFactory
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import org.bitcoinj.core.ECKey
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.answers.Logging
import piuk.blockchain.android.data.answers.PaymentSentEvent
import piuk.blockchain.android.data.api.EnvironmentSettings
import piuk.blockchain.android.data.auth.AuthService
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.datamanagers.FeeDataManager
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.ethereum.models.CombinedEthModel
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.payments.SendDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.services.EventService
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.chooser.AccountChooserActivity
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.receive.ReceiveCurrencyHelper
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.util.*
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.io.IOException
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
        private val feeDataManager: FeeDataManager,
        private val privateKeyFactory: PrivateKeyFactory,
        private val environmentSettings: EnvironmentSettings,
        private val sslVerifyUtil: SSLVerifyUtil,
        private val transactionListDataManager: TransactionListDataManager
) : BasePresenter<SendViewNew>() {

    val locale by unsafeLazy { Locale.getDefault() }
    val currencyHelper by unsafeLazy { ReceiveCurrencyHelper(monetaryUtil, locale, prefsUtil, exchangeRateFactory, currencyState) }
    val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(getBtcUnitType()) }

    lateinit var feeOptions: FeeOptions
    val pendingTransaction: PendingTransaction by unsafeLazy { PendingTransaction() }
    val unspentApiResponses: HashMap<String, UnspentOutputs> by unsafeLazy { HashMap<String, UnspentOutputs>() }
    var unspentApiDisposable: Disposable = CompositeDisposable()
    var absoluteSuggestedFee = BigInteger.ZERO
    var maxAvailable = BigInteger.ZERO
    var verifiedSecondPassword: String? = null

    private var metricInputFlag: String? = null

    private fun getBtcUnitType() = prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)

    /**
     * External changes.
     * Possible currency change, Account/address archive, Balance change
     */
    fun onBroadcastReceived() {
        updateTicker()
        resetAccountList()
    }

    override fun onViewReady() {
        sslVerifyUtil.validateSSL()

        updateTicker()
        setCryptoCurrency()
    }

    fun onBitcoinChosen() {
        Timber.d("vos onBitcoinChosen compositeDisposable.clear")
        compositeDisposable.clear()
        currencyState.cryptoCurrency = CryptoCurrencies.BTC
        updateTicker()
        view.setTabSelection(0)
        absoluteSuggestedFee = BigInteger.ZERO
        view.updateFeeAmount("")
        view.enableFeeDropdown()
        resetAccountList()
        selectDefaultSendingAccount()
        view.hideMaxAvailable()
        clearCryptoAmount()
        clearReceivingAddress()
        view.setCryptoMaxLength(17)
        setCryptoCurrency()
        calculateTransactionAmounts(spendAll = false, amountToSendText = "0", feePriority = FeeType.FEE_OPTION_REGULAR)
        view.showFeePriority()
    }

    fun onEtherChosen() {
        Timber.d("vos onEtherChosen compositeDisposable.clear")
        compositeDisposable.clear()
        currencyState.cryptoCurrency = CryptoCurrencies.ETHER
        updateTicker()
        view.setTabSelection(1)
        absoluteSuggestedFee = BigInteger.ZERO
        view.updateFeeAmount("")
        view.disableFeeDropdown()
        resetAccountList()
        selectDefaultSendingAccount()
        view.hideMaxAvailable()
        clearCryptoAmount()
        clearReceivingAddress()
        view.setCryptoMaxLength(30)
        setCryptoCurrency()
        calculateTransactionAmounts(spendAll = false, amountToSendText = "0", feePriority = FeeType.FEE_OPTION_REGULAR)
        view.hideFeePriority()
    }

    internal fun onContinueClicked() {

        view?.showProgressDialog(R.string.app_name)

        checkManualAddressInput()

        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> {

                if (isValidBitcoinTransaction()) {

                    if(pendingTransaction.isWatchOnly()) {
                        //returns to spendFromWatchOnly*BIP38 -> showPaymentReview()
                        view.showSpendFromWatchOnlyWarning((pendingTransaction.sendingObject.accountObject as LegacyAddress).address)
                    } else if(pendingTransaction.isWatchOnly() && verifiedSecondPassword != null) {
                        //Second password already verified
                        showPaymentReview()
                    } else {
                        //Checks if second pw needed then -> onNoSecondPassword()
                        view.showSecondPasswordDialog()
                    }
                }
            }
            CryptoCurrencies.ETHER -> {

                if (iValidEtherTransaction()) {
                    showPaymentReview()
                }
            }
        }

        view?.dismissProgressDialog()
    }

    /**
     * Executes transaction
     */
    internal fun submitPayment() {
        when(currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> submitBitcoinTransaction()
            CryptoCurrencies.ETHER -> submitEthTransaction()
        }
    }

    private fun submitBitcoinTransaction() {

        view.showProgressDialog(R.string.app_name)

        val changeAddress: String
        val keys = ArrayList<ECKey>()
        try {
            if (pendingTransaction.isHD()) {
                val account = pendingTransaction.sendingObject.accountObject as Account
                changeAddress = payloadDataManager.getNextChangeAddress(account).blockingFirst()

                if (payloadDataManager.isDoubleEncrypted) {
                    payloadDataManager.decryptHDWallet(verifiedSecondPassword)
                }
                keys.addAll(payloadDataManager.getHDKeysForSigning(account, pendingTransaction.unspentOutputBundle))

            } else {
                val legacyAddress = pendingTransaction.sendingObject.accountObject as LegacyAddress
                changeAddress = legacyAddress.address
                keys.add(payloadDataManager.getAddressECKey(legacyAddress, verifiedSecondPassword)!!)
            }

        } catch (e: Exception) {
            Timber.e(e)
            view.dismissProgressDialog()
            view.showToast(R.string.transaction_failed, ToastCustom.TYPE_ERROR)
            return
        }

        compositeDisposable.add(
                sendDataManager.submitPayment(
                        pendingTransaction.unspentOutputBundle,
                        keys,
                        pendingTransaction.receivingAddress,
                        changeAddress,
                        pendingTransaction.bigIntFee,
                        pendingTransaction.bigIntAmount)
                        .doAfterTerminate { view.dismissProgressDialog() }
                        .subscribe(
                                { hash ->
                                    Logging.logCustom(PaymentSentEvent()
                                            .putSuccess(true)
                                            .putAmountForRange(pendingTransaction.bigIntAmount))

                                    clearBtcUnspentResponseCache()
                                    view.dismissConfirmationDialog()
                                    handleSuccessfulBtcPayment(hash)
                                }) { throwable ->
                            view.showToast(R.string.transaction_failed, ToastCustom.TYPE_ERROR)

                            Logging.logCustom(PaymentSentEvent()
                                    .putSuccess(false)
                                    .putAmountForRange(pendingTransaction.bigIntAmount))
                        })
    }

    private fun submitEthTransaction() {

    }

    private fun clearBtcUnspentResponseCache() {
        if (pendingTransaction.isHD()) {
            val account = pendingTransaction.sendingObject.accountObject as Account
            unspentApiResponses.remove(account.xpub)
        } else {
            val legacyAddress = pendingTransaction.sendingObject.accountObject as LegacyAddress
            unspentApiResponses.remove(legacyAddress.address)
        }
    }

    private fun handleSuccessfulBtcPayment(hash: String) {
        insertPlaceHolderTransaction(hash, pendingTransaction)

        if (pendingTransaction.isHD()) {
            val account = pendingTransaction.sendingObject.accountObject as Account
            payloadDataManager.incrementChangeAddress(account)
            payloadDataManager.incrementReceiveAddress(account)
            updateInternalBalances()
        }
        if (view != null) {
            view.showTransactionSuccess(hash, pendingTransaction.bigIntAmount.toLong())
        }
        pendingTransaction.clear()
        unspentApiResponses.clear()

        logAddressInputMetric()
    }

    private fun logAddressInputMetric() {
        val handler = EventService(prefsUtil, AuthService(WalletApi()))
        if (metricInputFlag != null) handler.logAddressInputEvent(metricInputFlag)
    }

    /**
     * Update balance immediately after spend - until refresh from server
     */
    private fun updateInternalBalances() {
        try {
            val totalSent = pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee)
            if (pendingTransaction.isHD()) {
                val account = pendingTransaction.sendingObject.accountObject as Account
                payloadDataManager.subtractAmountFromAddressBalance(account.xpub, totalSent.toLong())
            } else {
                val address = pendingTransaction.sendingObject.accountObject as LegacyAddress
                payloadDataManager.subtractAmountFromAddressBalance(address.address, totalSent.toLong())
            }
        } catch (e: Exception) {
            Timber.e(e)
        }

    }

    /**
     * After sending btc we create a "placeholder" tx until websocket handler refreshes list
     */
    private fun insertPlaceHolderTransaction(hash: String, pendingTransaction: PendingTransaction) {
        val inputs = HashMap<String, BigInteger>()
        pendingTransaction.sendingObject.label?.let {
            inputs.put(pendingTransaction.sendingObject.label!!, pendingTransaction.bigIntAmount)
        }

        val outputs = HashMap<String, BigInteger>()
        outputs.put(pendingTransaction.displayableReceivingLabel, pendingTransaction.bigIntAmount)

        val tx = TransactionSummary()
        tx.direction = TransactionSummary.Direction.SENT
        tx.time = System.currentTimeMillis() / 1000
        tx.total = pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee)
        tx.hash = hash
        tx.fee = pendingTransaction.bigIntFee
        tx.inputsMap = inputs
        tx.outputsMap = outputs
        tx.isPending = true
        // STOPSHIP: 24/08/2017
//      transactionListDataManager.insertTransactionIntoListAndReturnSorted(tx);
    }

    internal fun onNoSecondPassword() {
        showPaymentReview()
    }

    internal fun onSecondPasswordValidated(secondPassword: String) {
        verifiedSecondPassword = secondPassword
        showPaymentReview()
    }

    internal fun showPaymentReview() {

        val paymentDetais = getConfirmationDetails()
        var allowFeeChange = true

        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> {
                if(paymentDetais.isLargeTransaction) {
                    view.showLargeTransactionWarning()
                }
            }
            CryptoCurrencies.ETHER -> {
                allowFeeChange = false
            }
        }

        view.showPaymentDetails(getConfirmationDetails(), null, allowFeeChange)
    }

    internal fun checkManualAddressInput() {
        val address = view.getReceivingAddress()
        address?.let {
            //Input analytics
            checkClipboardPaste(address)

            //Only if valid address so we don't override with a label
            when(currencyState.cryptoCurrency) {
                CryptoCurrencies.BTC -> if (FormatsUtil.isValidBitcoinAddress(address)) pendingTransaction.receivingAddress = address
                CryptoCurrencies.ETHER -> if (FormatsUtil.isValidEthereumAddress(address)) {
                    pendingTransaction.receivingAddress = address
                }
            }
        }
    }

    private fun getConfirmationDetails(): PaymentConfirmationDetails {
        val pendingTransaction = pendingTransaction

        val details = PaymentConfirmationDetails()

        details.fromLabel = pendingTransaction.sendingObject.label
        details.toLabel = pendingTransaction.getDisplayableReceivingLabel()

        details.cryptoUnit = currencyHelper.cryptoUnit
        details.fiatUnit = currencyHelper.fiatUnit
        details.fiatSymbol = exchangeRateFactory.getSymbol(currencyHelper.fiatUnit)

        when(currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> {
                details.isLargeTransaction = isLargeTransaction()
                details.btcSuggestedFee = currencyHelper.getTextFromSatoshis(absoluteSuggestedFee.toLong(), getDefaultDecimalSeparator())
//                details.hasConsumedAmounts = pendingTransaction.unspentOutputBundle.consumedAmount.compareTo(BigInteger.ZERO) == 1 //Unused

                details.cryptoTotal = currencyHelper.getTextFromSatoshis(pendingTransaction.total.toLong(), getDefaultDecimalSeparator())
                details.cryptoAmount = currencyHelper.getTextFromSatoshis(pendingTransaction.bigIntAmount.toLong(), getDefaultDecimalSeparator())
                details.cryptoFee = currencyHelper.getTextFromSatoshis(pendingTransaction.bigIntFee.toLong(), getDefaultDecimalSeparator())

                details.fiatFee = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit)
                        .format(currencyHelper.lastPrice * (pendingTransaction.bigIntFee.toDouble() / 1e8))
                details.fiatAmount = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit)
                        .format(currencyHelper.lastPrice * (pendingTransaction.bigIntAmount.toDouble() / 1e8))
                details.fiatTotal = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit)
                        .format(currencyHelper.lastPrice * (pendingTransaction.total.toDouble() / 1e8))
            }
            CryptoCurrencies.ETHER -> {

                val ethAmount = Convert.fromWei(pendingTransaction.bigIntAmount.toString(), Convert.Unit.ETHER)
                val ethFee = Convert.fromWei(pendingTransaction.bigIntFee.toString(), Convert.Unit.ETHER)
                val ethTotal = ethAmount.add(ethFee)

                details.cryptoAmount = ethAmount.toString()
                details.cryptoFee = ethFee.toString()
                details.cryptoTotal = ethTotal.toString()

                details.fiatFee = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit)
                        .format(currencyHelper.lastPrice * (ethFee.toDouble()))
                details.fiatAmount = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit)
                        .format(currencyHelper.lastPrice * (ethAmount.toDouble()))
                details.fiatTotal = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit)
                        .format(currencyHelper.lastPrice * (ethTotal.toDouble()))
            }
        }

        return details
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
    }

    internal fun clearReceivingAddress() {
        view.updateReceivingAddress("")
    }

    internal fun clearReceivingObject() {
        pendingTransaction.receivingObject = null
        metricInputFlag = null
    }

    internal fun clearCryptoAmount() {
        view.updateCryptoAmount("")
    }

    internal fun getAddressList(): List<ItemAccount> = walletAccountHelper.getAccountItems()

    internal fun setReceiveHint(accountsCount: Int) {

        var hint: Int

        if (accountsCount > 1) {
            when (currencyState.cryptoCurrency) {
                CryptoCurrencies.BTC -> hint = R.string.to_field_helper
                CryptoCurrencies.ETHER -> hint = R.string.eth_to_field_helper
            }
        } else {
            when (currencyState.cryptoCurrency) {
                CryptoCurrencies.BTC -> hint = R.string.to_field_helper_no_dropdown
                CryptoCurrencies.ETHER -> hint = R.string.eth_to_field_helper_no_dropdown
            }
        }

        view.updateReceivingHint(hint)
    }

    internal fun setCryptoCurrency() {
        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> view.updateCryptoCurrency(currencyHelper.btcUnit)
            CryptoCurrencies.ETHER -> view.updateCryptoCurrency(currencyHelper.ethUnit)
        }
    }

    internal fun selectDefaultSendingAccount() {
        val accountItem = walletAccountHelper.getDefaultAccount()
        view.updateSendingAddress(accountItem.label!!)
        pendingTransaction.sendingObject = accountItem
    }

    internal fun selectSendingBtcAccount(accountPosition: Int) {

        if (accountPosition >= 0) {
            var label = getAddressList().get(accountPosition).label
            if (label == null || label.isEmpty()) {
                label = getAddressList().get(accountPosition).address
            }
            view.updateSendingAddress(label!!)
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

    internal fun updateCryptoTextField(editable: Editable, editText: EditText) {

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
        view.updateCryptoAmount(amountString)
        view.enableCryptoTextChangeListener()
    }

    internal fun updateFiatTextField(editable: Editable, editText: EditText) {

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
        view.updateFiatAmount(amountString)
        view.enableFiatTextChangeListener()
    }

    internal fun onSpendAllClicked(feePriority: Int) {
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
            CryptoCurrencies.ETHER -> {
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
            CryptoCurrencies.ETHER -> {
                val eth = Convert.fromWei(absoluteSuggestedFee.toString(), Convert.Unit.ETHER)
                cryptoPrice = eth.toString()
                fiatPrice = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit)
                        .format(currencyHelper.lastPrice * (eth.toDouble()))
            }
        }

        view.updateFeeAmount(
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
            view.updateMaxAvailable(stringUtils.getString(R.string.max_available) + " " + fiatBalanceFormatted + " " + currencyHelper.fiatUnit)
        } else {
            val btcAmountFormatted = monetaryUtil.getBtcFormat().format(monetaryUtil.getDenominatedAmount(Math.max(balanceAfterFee.toDouble(), 0.0) / 1e8))
            view.updateMaxAvailable(stringUtils.getString(R.string.max_available) + " " + btcAmountFormatted + " " + currencyHelper.cryptoUnit)
        }

        if (balanceAfterFee.compareTo(Payment.DUST) <= 0) {
            view.updateMaxAvailable(stringUtils.getString(R.string.insufficient_funds))
            view.updateMaxAvailableColor(R.color.product_red_medium)
        } else {
            view.updateMaxAvailableColor(R.color.primary_blue_accent)
        }
    }

    /**
     *
     * Fetches unspent data Gets spendable coins Mixed checks and updates
     */
    internal fun calculateTransactionAmounts(spendAll: Boolean,
                                    amountToSendText: String?,
                                    @FeeType.FeePriorityDef feePriority: Int) {

        view.hideMaxAvailable()
        view.clearWarning()

        val feePerKb = getFeePerKbFromPriority(feePriority)

        when(currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> {
                calculateUnspentBtc(spendAll, amountToSendText, feePerKb)
            }
            CryptoCurrencies.ETHER -> {
                getEthAccountResponse(spendAll, amountToSendText)
            }
        }
    }

    internal fun calculateUnspentBtc(spendAll: Boolean, amountToSendText: String?, feePerKb: BigInteger) {

        val address = pendingTransaction.sendingObject.getAddressString()

        if (unspentApiDisposable != null) unspentApiDisposable.dispose()
        unspentApiDisposable = getUnspentApiResponse(address)
                .compose(RxUtil.applySchedulersToObservable<UnspentOutputs>())
                .subscribe(
                        { coins ->
                            val amountToSend = currencyHelper.getSatoshisFromText(amountToSendText, getDefaultDecimalSeparator())

                            // Future use. There might be some unconfirmed funds. Not displaying a warning currently (to line up with iOS and Web wallet)
                            if (coins.getNotice() != null)
                                view.updateWarning(coins.getNotice())
                            else
                                view.clearWarning()

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
            view?.updateCryptoAmount(currencyHelper.getTextFromSatoshis(sweepableAmount.toLong(), getDefaultDecimalSeparator()))
        }

        val unspentOutputBundle = sendDataManager.getSpendableCoins(coins,
                amountToSend,
                feePerKb)

        pendingTransaction.bigIntAmount = amountToSend
        pendingTransaction.unspentOutputBundle = unspentOutputBundle
        pendingTransaction.bigIntFee = pendingTransaction.unspentOutputBundle.getAbsoluteFee()
    }

    internal fun getEthAccountResponse(spendAll: Boolean, amountToSendText: String?) {

        view.showMaxAvailable()

        if(ethDataManager.getEthAddress() == null) {
            ethDataManager.fetchEthAddress()
                    .compose(RxUtil.addObservableToCompositeDisposable(this))
                    .doOnError { view.showToast(R.string.api_fail, ToastCustom.TYPE_ERROR) }
                    .subscribe(Consumer {
                        calculateUnspentEth(it, spendAll, amountToSendText)
                    })
        } else {
            val combinedEthModel = ethDataManager.getEthAddress()
            combinedEthModel?.let {
                calculateUnspentEth(combinedEthModel, spendAll, amountToSendText)
            }
        }
    }

    internal fun calculateUnspentEth(combinedEthModel: CombinedEthModel, spendAll: Boolean, amountToSendText: String?) {

        val gwei = BigDecimal.valueOf(feeOptions.gasLimit * feeOptions.regularFee)
        val wei = Convert.toWei(gwei, Convert.Unit.GWEI)

        updateFee(wei.toBigInteger())
        pendingTransaction.bigIntFee = wei.toBigInteger()

        val addressResponse = combinedEthModel.getAddressResponse()
        maxAvailable = addressResponse!!.balance.minus(wei.toBigInteger())

        val availableEth = Convert.fromWei(maxAvailable.toString(), Convert.Unit.ETHER)
        if (spendAll) {
            view?.updateCryptoAmount(availableEth.toString())
            pendingTransaction.bigIntAmount = availableEth.toBigInteger()
        } else {
            pendingTransaction.bigIntAmount = currencyHelper.getWeiFromText(amountToSendText, getDefaultDecimalSeparator())
        }

        //Format for display
        if (!currencyState.isDisplayingCryptoCurrency) {

            val fiatBalance = currencyHelper.lastPrice * availableEth.toLong()

            val fiatBalanceFormatted = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit).format(fiatBalance)
            view.updateMaxAvailable(stringUtils.getString(R.string.max_available) + " " + fiatBalanceFormatted + " " + currencyHelper.fiatUnit)
        } else {
            val number = DecimalFormat.getInstance().apply { maximumFractionDigits = 16 }
                    .run { format(availableEth.toDouble()) }
            view.updateMaxAvailable(stringUtils.getString(R.string.max_available) + " " + number)
        }

        if (maxAvailable.compareTo(Payment.DUST) <= 0) {
            view.updateMaxAvailable(stringUtils.getString(R.string.insufficient_funds))
            view.updateMaxAvailableColor(R.color.product_red_medium)
        } else {
            view.updateMaxAvailableColor(R.color.primary_blue_accent)
        }
    }

    internal fun handleURIScan(untrimmedscanData: String?, scanRoute: String) {

        if(untrimmedscanData == null) return

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
                view?.updateCryptoAmount(amount)
            } catch (e: Exception) {
                //ignore
            }

        } else if(FormatsUtil.isValidEthereumAddress(scanData)){
            onEtherChosen()
            address = scanData
            view?.updateCryptoAmount("")
        } else {
            view.showToast(R.string.invalid_bitcoin_address, ToastCustom.TYPE_ERROR)
            return
        }

        if (address != "") {
            pendingTransaction.receivingObject = null
            pendingTransaction.receivingAddress = address
            view.updateReceivingAddress(address)
        }
    }

    internal fun handlePrivxScan(scanData: String?) {

        if(scanData == null) return

        val format = privateKeyFactory.getFormat(scanData)

        if(format == null) {
            view?.showToast(R.string.privkey_error, ToastCustom.TYPE_ERROR)
        }

        when (format) {
            PrivateKeyFactory.BIP38 -> spendFromWatchOnlyNonBIP38(format, scanData)
            else -> view?.showBIP38PassphrasePrompt(scanData)//BIP38 needs passphrase
        }
    }

    private fun spendFromWatchOnlyNonBIP38(format: String, scanData: String) {
        try {
            val key = privateKeyFactory.getKey(format, scanData)
            val legacyAddress = pendingTransaction.sendingObject.accountObject as LegacyAddress
            setTempLegacyAddressPrivateKey(legacyAddress, key)

        } catch (e: Exception) {
            view?.showToast(R.string.no_private_key, ToastCustom.TYPE_ERROR)
            Timber.e(e)
        }

    }

    internal fun spendFromWatchOnlyBIP38(pw: String, scanData: String) {
        compositeDisposable.add(
                sendDataManager.getEcKeyFromBip38(pw, scanData, environmentSettings.getNetworkParameters())
                        .subscribe({ ecKey ->
                            val legacyAddress = pendingTransaction.sendingObject.accountObject as LegacyAddress
                            setTempLegacyAddressPrivateKey(legacyAddress, ecKey)
                        }) { throwable -> view?.showToast(R.string.bip38_error, ToastCustom.TYPE_ERROR) })
    }

    private fun setTempLegacyAddressPrivateKey(legacyAddress: LegacyAddress, key: ECKey?) {
        if (key != null && key.hasPrivKey() && legacyAddress.address == key.toAddress(
                environmentSettings.getNetworkParameters()).toString()) {

            //Create copy, otherwise pass by ref will override private key in wallet payload
            val tempLegacyAddress = LegacyAddress()
            tempLegacyAddress.setPrivateKeyFromBytes(key.privKeyBytes)
            tempLegacyAddress.address = key.toAddress(environmentSettings.getNetworkParameters()).toString()
            tempLegacyAddress.label = legacyAddress.label
            pendingTransaction.sendingObject.accountObject = tempLegacyAddress

            showPaymentReview()
        } else {
            view?.showToast(R.string.invalid_private_key, ToastCustom.TYPE_ERROR)
        }
    }

    internal fun onSendingLegacyAddressSelected(legacyAddress: LegacyAddress) {

        pendingTransaction.receivingObject = ItemAccount(legacyAddress.label, null, null, null, legacyAddress, legacyAddress.address)

        var label = legacyAddress.label
        if (label == null || label.isEmpty()) {
            label = legacyAddress.address
        }
        view.updateSendingAddress(label)
    }

    internal fun onSendingAccountSelected(account: Account) {

        pendingTransaction.receivingObject = ItemAccount(account.label, null, null, null, account, null)

        var label = account.label
        if (label == null || label.isEmpty()) {
            label = account.xpub
        }

        view.updateSendingAddress(label)
    }

    internal fun onReceivingLegacyAddressSelected(legacyAddress: LegacyAddress) {

        pendingTransaction.receivingObject = ItemAccount(legacyAddress.label, null, null, null, legacyAddress, legacyAddress.address)
        pendingTransaction.receivingAddress = legacyAddress.address

        var label = legacyAddress.label
        if (label == null || label.isEmpty()) {
            label = legacyAddress.address
        }
        view.updateReceivingAddress(label)

        if (legacyAddress.isWatchOnly && shouldWarnWatchOnly()) {
            view.showWatchOnlyWarning(legacyAddress.address)
        }
    }

    internal fun shouldWarnWatchOnly(): Boolean {
        return prefsUtil.getValue(PREF_WARN_WATCH_ONLY_SPEND, true)
    }

    internal fun setWarnWatchOnlySpend(warn: Boolean) {
        prefsUtil.setValue(PREF_WARN_WATCH_ONLY_SPEND, warn)
    }

    internal fun onReceivingAccountSelected(account: Account) {

        pendingTransaction.receivingObject = ItemAccount(account.label, null, null, null, account, null)

        var label = account.label
        if (label == null || label.isEmpty()) {
            label = account.xpub
        }
        view.updateReceivingAddress(label)

        payloadDataManager.getNextReceiveAddress(account)
                .doOnNext {
                    pendingTransaction.receivingAddress = it
                }
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe({
                    /* No-op */
                }, { view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR) })
    }

    internal fun selectSendingAccount(data: Intent?) {

        try {
            val type: Class<*> = Class.forName(data?.getStringExtra(AccountChooserActivity.EXTRA_SELECTED_OBJECT_TYPE))
            val any = ObjectMapper().readValue(data?.getStringExtra(AccountChooserActivity.EXTRA_SELECTED_ITEM), type)

            when (any) {
                is LegacyAddress -> onSendingLegacyAddressSelected(any)
                is Account -> onSendingAccountSelected(any)
                else -> throw IllegalArgumentException("No method for handling $type available")
            }

        } catch (e: ClassNotFoundException) {
            Timber.e(e)
            selectDefaultSendingAccount()
        } catch (e: IOException) {
            Timber.e(e)
            selectDefaultSendingAccount()
        }
    }

    internal fun selectReceivingAccount(data: Intent?) {

        try {
            val type: Class<*> = Class.forName(data?.getStringExtra(AccountChooserActivity.EXTRA_SELECTED_OBJECT_TYPE))
            val any = ObjectMapper().readValue(data?.getStringExtra(AccountChooserActivity.EXTRA_SELECTED_ITEM), type)

            when (any) {
                is LegacyAddress -> onReceivingLegacyAddressSelected(any)
                is Account -> onReceivingAccountSelected(any)
                else -> throw IllegalArgumentException("No method for handling $type available")
            }

        } catch (e: ClassNotFoundException) {
            Timber.e(e)
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    internal fun updateTicker() {
        exchangeRateFactory.updateTickers()
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .compose(RxUtil.applySchedulersToCompletable())
                .subscribe({
                    //no-op
                }, { it.printStackTrace() })
    }

    private fun checkClipboardPaste(address: String) {
        val contents = view.getClipboardContents()
        if (contents != null && contents == address) {
            metricInputFlag = EventService.EVENT_TX_INPUT_FROM_PASTE
        }
    }

    private fun isValidBitcoinAmount(bAmount: BigInteger?): Boolean {
        if (bAmount == null) {
            return false
        }

        // Test that amount is more than dust
        if (bAmount.compareTo(Payment.DUST) == -1) {
            return false
        }

        // Test that amount does not exceed btc limit
        if (bAmount.compareTo(BigInteger.valueOf(2_100_000_000_000_000L)) == 1) {
            clearCryptoAmount()
            return false
        }

        // Test that amount is not zero
        return bAmount.compareTo(BigInteger.ZERO) >= 0
    }

    private fun isValidBitcoinTransaction(): Boolean {

        var validated = true

        //Validate address
        if (pendingTransaction.receivingAddress == null || !FormatsUtil.isValidBitcoinAddress(pendingTransaction.receivingAddress)) {
            view?.showToast(R.string.invalid_bitcoin_address, ToastCustom.TYPE_ERROR)
            validated = false
        }

        //Validate amount
        if (!isValidBitcoinAmount(pendingTransaction.bigIntAmount)) {
            view?.showToast(R.string.invalid_amount, ToastCustom.TYPE_ERROR)
            return false
        }

        // Validate sufficient funds
        if (pendingTransaction.unspentOutputBundle == null || pendingTransaction.unspentOutputBundle.spendableOutputs == null) {
            view?.showToast(R.string.no_confirmed_funds, ToastCustom.TYPE_ERROR)
            return false
        }

        if (maxAvailable.compareTo(pendingTransaction.bigIntAmount) == -1) {
            view?.showToast(R.string.insufficient_funds, ToastCustom.TYPE_ERROR)
            return false
        }

        if (pendingTransaction.unspentOutputBundle.spendableOutputs.isEmpty()) {
            view?.showToast(R.string.insufficient_funds, ToastCustom.TYPE_ERROR)
            return false
        }

        return validated
    }

    private fun isValidEtherAmount(bAmount: BigInteger?): Boolean {
        return (bAmount != null && bAmount.compareTo(BigInteger.ZERO) >= 0)
    }

    private fun iValidEtherTransaction(): Boolean {

        var validated = true

        //Validate address
        if (pendingTransaction.receivingAddress == null || !FormatsUtil.isValidEthereumAddress(pendingTransaction.receivingAddress)) {
            view?.showToast(R.string.invalid_ether_address, ToastCustom.TYPE_ERROR)
            validated = false
        }

        //Validate amount
        if (!isValidEtherAmount(pendingTransaction.bigIntAmount)) {
            view?.showToast(R.string.invalid_amount, ToastCustom.TYPE_ERROR)
            return false
        }

        // Validate sufficient funds
        if (maxAvailable.compareTo(pendingTransaction.bigIntAmount) == -1) {
            view?.showToast(R.string.insufficient_funds, ToastCustom.TYPE_ERROR)
            return false
        }

        return validated

    }

    /**
     * Returns true if bitcoin transaction is large by checking against 3 criteria:
     *
     * If the fee > $0.50
     * If the Tx size is over 1kB
     * If the ratio of fee/amount is over 1%
     */
    private fun isLargeTransaction(): Boolean {
        val valueString = monetaryUtil.getFiatFormat("USD")
                .format(exchangeRateFactory.getLastBtcPrice("USD") * absoluteSuggestedFee.toDouble() / 1e8)
        val usdValue = java.lang.Double.parseDouble(currencyHelper.stripSeparator(valueString, getDefaultDecimalSeparator()))
        val txSize = sendDataManager.estimateSize(pendingTransaction.unspentOutputBundle.getSpendableOutputs().size, 2)//assume change
        val relativeFee = absoluteSuggestedFee.toDouble() / pendingTransaction.bigIntAmount.toDouble() * 100.0

        return usdValue > SendModel.LARGE_TX_FEE
                && txSize > SendModel.LARGE_TX_SIZE
                && relativeFee > SendModel.LARGE_TX_PERCENTAGE
    }

    companion object {
        private val PREF_WARN_WATCH_ONLY_SPEND = "pref_warn_watch_only_spend"
    }
}