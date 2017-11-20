package piuk.blockchain.android.ui.shapeshift.confirmation

import info.blockchain.api.data.UnspentOutputs
import info.blockchain.wallet.ethereum.EthereumAccount
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import info.blockchain.wallet.util.FormatsUtil
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
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject

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
                            receiveAddress,
                            changeAddress,
                            withdrawalAmount,
                            networkFee,
                            feePerKb
                    )
                    CryptoCurrencies.ETHER -> sendEthTransaction(
                            networkFee,
                            receiveAddress,
                            withdrawalAmount,
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

    private fun sendBtcTransaction(
            xPub: String,
            receiveAddress: String,
            changeAddress: String,
            withdrawalAmount: BigDecimal,
            networkFee: BigDecimal,
            feePerKb: BigInteger
    ) {
        require(FormatsUtil.isValidBitcoinAddress(receiveAddress)) { "Attempting to send BTC to a non-BTC address" }

        val account = payloadDataManager.getAccountForXPub(xPub)
        val satoshis = withdrawalAmount.multiply(BigDecimal.valueOf(BTC_DEC)).toBigInteger()

        if (payloadDataManager.isDoubleEncrypted) {
            payloadDataManager.decryptHDWallet(verifiedSecondPassword)
        }

        getUnspentApiResponse(xPub)
                .compose(RxUtil.applySchedulersToObservable<UnspentOutputs>())
                .map { sendDataManager.getSpendableCoins(it, satoshis, feePerKb) }
                .flatMap { unspent ->
                    getBitcoinKeys(account, unspent)
                            .flatMap {
                                sendDataManager.submitPayment(
                                        unspent,
                                        it,
                                        receiveAddress,
                                        changeAddress,
                                        networkFee.toBigInteger(),
                                        satoshis
                                )
                            }
                }
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(
                        {
                            // TODO: Handle successful payment, launch next page?
                            view.showToast(R.string.success, ToastCustom.TYPE_OK)
                        },
                        {
                            Timber.e(it)
                            view.showToast(R.string.transaction_failed, ToastCustom.TYPE_ERROR)
                        }
                )
    }

    private fun getUnspentApiResponse(address: String): Observable<UnspentOutputs> {
        return if (payloadDataManager.getAddressBalance(address).toLong() > 0) {
            sendDataManager.getUnspentOutputs(address)
        } else {
            Observable.error(Throwable("No funds - skipping call to unspent API"))
        }
    }

    private fun createEthTransaction(
            networkFee: BigDecimal,
            receiveAddress: String,
            withdrawalAmount: BigDecimal,
            gasLimit: BigInteger
    ): Observable<RawTransaction> {
        require(FormatsUtil.isValidEthereumAddress(receiveAddress)) { "Attempting to send ETH to a non-ETH address" }

        return Observable.just(ethDataManager.getEthResponseModel()!!.getNonce())
                .map {
                    ethDataManager.createEthTransaction(
                            nonce = it,
                            to = receiveAddress,
                            gasPrice = networkFee.toBigInteger(),
                            gasLimit = gasLimit,
                            weiValue = Convert.toWei(withdrawalAmount, Convert.Unit.ETHER).toBigInteger()
                    )
                }
    }

    private fun sendEthTransaction(
            networkFee: BigDecimal,
            receiveAddress: String,
            withdrawalAmount: BigDecimal,
            gasLimit: BigInteger
    ) {
        createEthTransaction(networkFee, receiveAddress, withdrawalAmount, gasLimit)
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .doOnError { view.showToast(R.string.transaction_failed, ToastCustom.TYPE_ERROR) }
                .doOnTerminate { view.dismissProgressDialog() }
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
                .subscribe(
                        {
                            // TODO: Handle successful payment, launch next page?
                            view.showToast(R.string.success, ToastCustom.TYPE_OK)
                        },
                        {
                            Timber.e(it)
                            view.showToast(R.string.transaction_failed, ToastCustom.TYPE_ERROR)
                        })
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
            CryptoCurrencies.ETHER -> Convert.fromWei(BigDecimal(transactionFee), Convert.Unit.WEI)
            CryptoCurrencies.BCH -> throw IllegalArgumentException("BCH not yet supported")
        }

        val displayString = "${decimalFormat.format(amount)} ${fromCurrency.symbol}"

        view.updateTransactionFee(displayString)
    }

    private fun updateNetworkFee(toCurrency: CryptoCurrencies, networkFee: BigDecimal) {
        val amount = when (toCurrency) {
            CryptoCurrencies.BTC -> networkFee.divide(BigDecimal.valueOf(BTC_DEC), 8, RoundingMode.HALF_DOWN)
            CryptoCurrencies.ETHER -> Convert.fromWei(networkFee, Convert.Unit.WEI)
            CryptoCurrencies.BCH -> throw IllegalArgumentException("BCH not yet supported")
        }

        val displayString = "${decimalFormat.format(amount)} ${toCurrency.symbol}"

        view.updateNetworkFee(displayString)
    }
    //endregion

    private fun startCountdown(endTime: Long) {
        var remaining = (endTime - System.currentTimeMillis()) / 1000
        if (remaining <= 0) {
            // Finish page with error
            view.finishPage()
            view.showToast(R.string.shapeshift_quote_expired_error, ToastCustom.TYPE_ERROR)
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
                    .takeUntil { it <= 0 }
                    .doOnComplete { /** Show countdown finished */ }
                    .subscribe()
        }
    }

    companion object {

        private const val BTC_DEC = 1e8

    }

}