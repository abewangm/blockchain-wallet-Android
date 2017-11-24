package piuk.blockchain.android.ui.shapeshift.confirmation

import info.blockchain.api.data.UnspentOutputs
import info.blockchain.wallet.ethereum.EthereumAccount
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import info.blockchain.wallet.shapeshift.data.Quote
import info.blockchain.wallet.shapeshift.data.Trade
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import org.bitcoinj.core.ECKey
import org.web3j.protocol.core.methods.request.RawTransaction
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.payments.SendDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.annotations.Mockable
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Mockable
class ShapeShiftConfirmationPresenter @Inject constructor(
        private val shapeShiftDataManager: ShapeShiftDataManager,
        private val payloadDataManager: PayloadDataManager,
        private val sendDataManager: SendDataManager,
        private val ethDataManager: EthDataManager,
        private val stringUtils: StringUtils
) : BasePresenter<ShapeShiftConfirmationView>() {

    private val decimalFormat by unsafeLazy {
        DecimalFormat().apply {
            minimumIntegerDigits = 1
            minimumFractionDigits = 1
            maximumFractionDigits = 8
        }
    }
    private var termsAccepted = false
    private var verifiedSecondPassword: String? = null

    override fun onViewReady() {
        // TODO: Remove me
        Timber.d(view.shapeShiftData.toString())

        with(view.shapeShiftData) {
            // Render data
            updateDeposit(fromCurrency, depositAmount)
            updateReceive(toCurrency, withdrawalAmount)
            updateExchangeRate(exchangeRate, fromCurrency, toCurrency)
            updateTransactionFee(fromCurrency, transactionFee)
            updateNetworkFee(toCurrency, networkFee)

            // Include countdown
            startCountdown(expiration)
        }
    }

    internal fun onAcceptTermsClicked() {
        termsAccepted = !termsAccepted
        view.setButtonState(termsAccepted)
    }

    internal fun onConfirmClicked() {
        if (!termsAccepted) {
            // Show warning
        } else {
            if (payloadDataManager.isDoubleEncrypted && verifiedSecondPassword == null) {
                // Show password dialog
                view.showSecondPasswordDialog()
                return
            }

            with(view.shapeShiftData) {
                when (fromCurrency) {
                    CryptoCurrencies.BTC -> sendBtcTransaction(
                            xPub,
                            depositAddress,
                            returnAddress,
                            depositAmount,
                            transactionFee,
                            feePerKb
                    )
                    CryptoCurrencies.ETHER -> sendEthTransaction(
                            gasPrice,
                            depositAddress,
                            depositAmount,
                            gasLimit
                    )
                    CryptoCurrencies.BCH -> throw IllegalArgumentException("BCH not yet supported")
                }
            }
        }
    }

    internal fun onSecondPasswordVerified(secondPassword: String) {
        verifiedSecondPassword = secondPassword
        onConfirmClicked()
    }

    private fun updateMetadata(completedTxHash: String): Completable {
        val trade = Trade().apply {
            hashIn = completedTxHash
            timestamp = System.currentTimeMillis()
            status = Trade.STATUS.NO_DEPOSITS
            quote = Quote().apply {
                // See https://github.com/blockchain/my-wallet-v3/blob/master/src/shift/trade.js#L132
                val shapeShiftData = view.shapeShiftData
                // toPartialJSON
                orderId = shapeShiftData.orderId
                quotedRate = shapeShiftData.exchangeRate
                deposit = shapeShiftData.depositAddress
                minerFee = shapeShiftData.networkFee
                // toJSON
                pair = """${shapeShiftData.fromCurrency.symbol.toLowerCase()}_${shapeShiftData.toCurrency.symbol.toLowerCase()}"""
                depositAmount = shapeShiftData.depositAmount
                withdrawal = shapeShiftData.withdrawalAddress
                withdrawalAmount = shapeShiftData.withdrawalAmount
            }
        }

        return shapeShiftDataManager.addTradeToList(trade)
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
    }

    private fun sendBtcTransaction(
            xPub: String,
            depositAddress: String,
            returnAddress: String,
            depositAmount: BigDecimal,
            transactionFee: BigInteger,
            feePerKb: BigInteger
    ) {
        require(FormatsUtil.isValidBitcoinAddress(depositAddress)) { "Attempting to send BTC to a non-BTC address" }

        val account = payloadDataManager.getAccountForXPub(xPub)
        val satoshis = depositAmount.multiply(BigDecimal.valueOf(BTC_DEC)).toBigInteger()

        if (payloadDataManager.isDoubleEncrypted) {
            payloadDataManager.decryptHDWallet(verifiedSecondPassword)
        }

        getUnspentApiResponse(xPub)
                .doOnSubscribe { view.showProgressDialog(R.string.please_wait) }
                .map { sendDataManager.getSpendableCoins(it, satoshis, feePerKb) }
                .flatMap { unspent ->
                    getBitcoinKeys(account, unspent)
                            .flatMap {
                                sendDataManager.submitPayment(
                                        unspent,
                                        it,
                                        depositAddress,
                                        returnAddress,
                                        transactionFee,
                                        satoshis
                                )
                            }
                }
                .flatMapCompletable { updateMetadata(it) }
                .doOnTerminate { view.dismissProgressDialog() }
                .doOnError(Timber::e)
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .subscribe(
                        { view.launchProgressPage(depositAddress) },
                        { view.showToast(R.string.transaction_failed, ToastCustom.TYPE_ERROR) }
                )
    }

    private fun sendEthTransaction(
            gasPrice: BigInteger,
            depositAddress: String,
            depositAmount: BigDecimal,
            gasLimit: BigInteger
    ) {
        createEthTransaction(gasPrice, depositAddress, depositAmount, gasLimit)
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .doOnSubscribe { view.showProgressDialog(R.string.please_wait) }
                .doOnTerminate { view.dismissProgressDialog() }
                .doOnError(Timber::e)
                .flatMap {
                    if (payloadDataManager.isDoubleEncrypted) {
                        payloadDataManager.decryptHDWallet(verifiedSecondPassword)
                    }

                    val ecKey = EthereumAccount.deriveECKey(
                            payloadDataManager.wallet.hdWallets[0].masterKey,
                            0
                    )
                    return@flatMap ethDataManager.signEthTransaction(it, ecKey)
                }
                .flatMap { ethDataManager.pushEthTx(it) }
                .flatMap { ethDataManager.setLastTxHashObservable(it) }
                .flatMapCompletable { updateMetadata(it) }
                .subscribe(
                        { view.launchProgressPage(depositAddress) },
                        { view.showToast(R.string.transaction_failed, ToastCustom.TYPE_ERROR) }
                )
    }

    private fun getUnspentApiResponse(address: String): Observable<UnspentOutputs> {
        return if (payloadDataManager.getAddressBalance(address).toLong() > 0) {
            sendDataManager.getUnspentOutputs(address)
                    .compose(RxUtil.applySchedulersToObservable())
        } else {
            Observable.error(Throwable("No funds - skipping call to unspent API"))
        }
    }

    private fun createEthTransaction(
            gasPrice: BigInteger,
            withdrawalAddress: String,
            withdrawalAmount: BigDecimal,
            gasLimit: BigInteger
    ): Observable<RawTransaction> {
        require(FormatsUtil.isValidEthereumAddress(withdrawalAddress)) { "Attempting to send ETH to a non-ETH address" }

        return Observable.just(ethDataManager.getEthResponseModel()!!.getNonce())
                .map {
                    ethDataManager.createEthTransaction(
                            nonce = it,
                            to = withdrawalAddress,
                            gasPrice = gasPrice,
                            gasLimit = gasLimit,
                            weiValue = Convert.toWei(withdrawalAmount, Convert.Unit.ETHER).toBigInteger()
                    )
                }
    }

    //region View Updates
    private fun getBitcoinKeys(account: Account, unspent: SpendableUnspentOutputs): Observable<MutableList<ECKey>> =
            Observable.just(payloadDataManager.getHDKeysForSigning(account, unspent))

    private fun updateDeposit(fromCurrency: CryptoCurrencies, depositAmount: BigDecimal) {
        val label = stringUtils.getFormattedString(R.string.shapeshift_deposit_title, fromCurrency.unit)
        val amount = "${depositAmount.toPlainString()} ${fromCurrency.symbol.toUpperCase()}"

        view.updateDeposit(label, amount)
    }

    private fun updateReceive(toCurrency: CryptoCurrencies, receiveAmount: BigDecimal) {
        val label = stringUtils.getFormattedString(R.string.shapeshift_receive_title, toCurrency.unit)
        val amount = "${receiveAmount.toPlainString()} ${toCurrency.symbol.toUpperCase()}"

        view.updateReceive(label, amount)
    }

    private fun updateExchangeRate(
            exchangeRate: BigDecimal,
            fromCurrency: CryptoCurrencies,
            toCurrency: CryptoCurrencies
    ) {
        val formattedExchangeRate = exchangeRate.setScale(8, RoundingMode.HALF_DOWN)
        val formattedString = stringUtils.getFormattedString(
                R.string.shapeshift_exchange_rate_formatted,
                fromCurrency.symbol,
                formattedExchangeRate,
                toCurrency.symbol
        )

        view.updateExchangeRate(formattedString)
    }

    private fun updateTransactionFee(fromCurrency: CryptoCurrencies, transactionFee: BigInteger) {
        val amount = when (fromCurrency) {
            CryptoCurrencies.BTC -> BigDecimal(transactionFee).divide(BigDecimal.valueOf(BTC_DEC), 8, RoundingMode.HALF_DOWN)
            CryptoCurrencies.ETHER -> Convert.fromWei(BigDecimal(transactionFee), Convert.Unit.ETHER)
            CryptoCurrencies.BCH -> throw IllegalArgumentException("BCH not yet supported")
        }

        val displayString = "${decimalFormat.format(amount)} ${fromCurrency.symbol}"

        view.updateTransactionFee(displayString)
    }

    private fun updateNetworkFee(toCurrency: CryptoCurrencies, networkFee: BigDecimal) {
        val displayString = "${decimalFormat.format(networkFee)} ${toCurrency.symbol}"

        view.updateNetworkFee(displayString)
    }
    //endregion

    private fun startCountdown(endTime: Long) {
        var remaining = (endTime - System.currentTimeMillis()) / 1000
        if (remaining <= 0) {
            // Finish page with error
            view.showQuoteExpiredDialog()
        } else {
            Observable.interval(1, TimeUnit.SECONDS)
                    .compose(RxUtil.addObservableToCompositeDisposable(this))
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnEach { remaining-- }
                    .map { return@map remaining }
                    .doOnNext {
                        val readableTime = String.format("%2d:%02d",
                                TimeUnit.SECONDS.toMinutes(it),
                                TimeUnit.SECONDS.toSeconds(it) -
                                        TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(it))
                        )
                        view.updateCounter(readableTime)
                    }
                    .doOnNext { if (it < 5 * 60) view.showTimeExpiring() }
                    .takeUntil { it <= 0 }
                    .doOnComplete { view.showQuoteExpiredDialog() }
                    .subscribe()
        }
    }

    companion object {

        private const val BTC_DEC = 1e8

    }

}