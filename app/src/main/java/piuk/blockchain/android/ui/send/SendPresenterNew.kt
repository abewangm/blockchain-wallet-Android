package piuk.blockchain.android.ui.send

import android.content.Intent
import android.text.Editable
import android.widget.EditText
import com.fasterxml.jackson.databind.ObjectMapper
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.util.PrivateKeyFactory
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.bitcoinj.core.ECKey
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.api.EnvironmentSettings
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.datamanagers.FeeDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.payments.SendDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.services.EventService
import piuk.blockchain.android.ui.account.ItemAccount
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
        private val sslVerifyUtil: SSLVerifyUtil
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

    internal fun onContinueClicked() {

        view?.showProgressDialog(R.string.app_name)

        //todo continue here
        val address = view.getReceivingAddress()
        if (FormatsUtil.isValidBitcoinAddress(address)) {
            //Receiving address manual or scanned input
            pendingTransaction.receivingAddress = address
        }

        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> if(isValidateBitcoinTransaction())
                view.showToast(R.string.ok_cap, ToastCustom.TYPE_OK)
            else -> if(iValidEtherTransaction())
                view.showToast(R.string.ok_cap, ToastCustom.TYPE_OK)
        }
        Timber.d("vos pendingTransaction: "+pendingTransaction.toString())

        view?.dismissProgressDialog()
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

    fun onBitcoinChosen() {
        currencyState.cryptoCurrency = CryptoCurrencies.BTC
        updateTicker()
        view.setTabSelection(0)
        absoluteSuggestedFee = BigInteger.ZERO
        view.updateFeeAmount("")
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
        currencyState.cryptoCurrency = CryptoCurrencies.ETHER
        updateTicker()
        view.setTabSelection(1)
        absoluteSuggestedFee = BigInteger.ZERO
        view.updateFeeAmount("")
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

    internal fun clearReceivingAddress() {
        view.updateReceivingAddress("")
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
                else -> hint = R.string.eth_to_field_helper
            }
        } else {
            when (currencyState.cryptoCurrency) {
                CryptoCurrencies.BTC -> hint = R.string.to_field_helper_no_dropdown
                else -> hint = R.string.eth_to_field_helper_no_dropdown
            }
        }

        view.updateReceivingHint(hint)
    }

    internal fun setCryptoCurrency() {
        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> view.updateCryptoCurrency(currencyHelper.btcUnit)
            else -> view.updateCryptoCurrency(currencyHelper.ethUnit)
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
            else -> {
                calculateUnspentEth(spendAll)
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
                            val amountToSend = getSatoshisFromText(amountToSendText)

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

    internal fun calculateUnspentEth(spendAll: Boolean) {

        view.showMaxAvailable()

        val gwei = BigDecimal.valueOf(feeOptions.gasLimit * feeOptions.regularFee)
        val wei = Convert.toWei(gwei, Convert.Unit.GWEI)
        updateFee(wei.toBigInteger())

        val ethR = ethDataManager.getEthAddress()
        maxAvailable = ethR!!.balance.minus(wei.toBigInteger())

        val availableEth = Convert.fromWei(maxAvailable.toString(), Convert.Unit.ETHER)
        if (spendAll) {
            view?.updateCryptoAmount(availableEth.toString())
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
            else -> view?.onShowBIP38PassphrasePrompt(scanData)//BIP38 needs passphrase
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

            //TODO
//            confirmPayment()
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

        payloadDataManager.getNextReceiveAddress(account)
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .doOnNext { pendingTransaction.receivingAddress = it }
                .doOnNext { view.updateReceivingAddress(it) }
                .subscribe({ /* No-op */ },{ view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR) })

        var label = account.label
        if (label == null || label.isEmpty()) {
            label = account.xpub
        }

        view.updateReceivingAddress(label)
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
        //TODO this is not working
        exchangeRateFactory.updateTickers()
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
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

    private fun isValidateBitcoinTransaction(): Boolean {

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

    companion object {
        private val PREF_WARN_WATCH_ONLY_SPEND = "pref_warn_watch_only_spend"
    }
}