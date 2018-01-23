package piuk.blockchain.android.ui.balance

import android.annotation.SuppressLint
import android.support.annotation.VisibleForTesting
import info.blockchain.wallet.ethereum.data.EthAddressResponse
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.access.AuthEvent
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.exchange.BuyDataManager
import piuk.blockchain.android.data.notifications.models.NotificationPayload
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.data.transactions.Displayable
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.balance.adapter.ItemAccount2
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.base.UiState
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat
import javax.inject.Inject

class BalancePresenter @Inject constructor(
        private val exchangeRateFactory: ExchangeRateFactory,
        private val transactionListDataManager: TransactionListDataManager,
        private val ethDataManager: EthDataManager,
        private val swipeToReceiveHelper: SwipeToReceiveHelper,
        internal val payloadDataManager: PayloadDataManager,
        private val buyDataManager: BuyDataManager,
        private val stringUtils: StringUtils,
        private val prefsUtil: PrefsUtil,
        private val rxBus: RxBus,
        private val currencyState: CurrencyState,
        private val shapeShiftDataManager: ShapeShiftDataManager,
        private val bchDataManager: BchDataManager
) : BasePresenter<BalanceView>() {

    private val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(getBtcUnitType()) }

    @SuppressLint("VisibleForTests")
    override fun onViewReady() {

//        getBalanceObservable()
//                .compose(RxUtil.addObservableToCompositeDisposable(this))
//                .doOnSubscribe { view.setUiState(UiState.LOADING) }



        view.updateSelectedCurrency(currencyState.cryptoCurrency)
        onAccountsAdapterSetup()

        //For now
        view.setUiState(UiState.EMPTY)
    }

    //region Currency header

    /*
    Currency selected from dropdown
     */
    internal fun updateSelectedCurrency(cryptoCurrency: CryptoCurrencies) {
        Timber.d("vos updateSelectedCurrency")
        currencyState.cryptoCurrency = cryptoCurrency
        view.selectDefaultAccount()
        onAccountChosen(0)
//        updateCurrencyUi(cryptoCurrency)

        updateAdapters(cryptoCurrency)
    }

//    private fun getBalanceObservable(itemAccount: ItemAccount2): Observable<Nothing> {
//
//        return when (currencyState.cryptoCurrency) {
//            CryptoCurrencies.BTC    -> {
//                payloadDataManager.updateAllBalances()
//                        .doOnComplete {
//                            val btcBalance = transactionListDataManager.getBtcBalance(itemAccount)
//                            val balanceTotal = getBtcBalanceString(currencyState.isDisplayingCryptoCurrency, btcBalance)
//                            view.onTotalBalanceUpdated(balanceTotal)
//                        }.toObservable<Nothing>()
//            }
//            CryptoCurrencies.ETHER    -> {
//                ethDataManager.fetchEthAddress()
//                        .doOnNext {
//                            val ethBalance = BigDecimal(it.getTotalBalance())
//                            val ethString = getEthBalanceString(currencyState.isDisplayingCryptoCurrency, ethBalance)
//                            view.onTotalBalanceUpdated(ethString)
//                        }.flatMap { Observable.empty<Nothing>() }
//            }
//            CryptoCurrencies.BCH -> {
//                payloadDataManager.updateAllBalances()
//                        .doOnComplete {
//                            val btcBalance = transactionListDataManager.getBtcBalance(itemAccount)
//                            val balanceTotal = getBtcBalanceString(currencyState.isDisplayingCryptoCurrency, btcBalance)
//                            view.onTotalBalanceUpdated(balanceTotal)
//                        }.toObservable<Nothing>()
//            }
//        }
//    }

    //endregion

    //region Accounts drop down
    /*
    Fetch all active accounts for initial selected currency and set up account adapter
     */
    fun onAccountsAdapterSetup() {
        Timber.d("vos onAccountsAdapterSetup: "+currencyState.cryptoCurrency)
        view.setupAccountsAdapter(getAccounts(currencyState.cryptoCurrency))
        onAccountChosen(0)
    }

    internal fun getAccounts(currency: CryptoCurrencies): MutableList<ItemAccount2> {
        return when (currency) {
            CryptoCurrencies.BTC -> getBtcAccounts()
            CryptoCurrencies.ETHER -> getEthAccounts()
            CryptoCurrencies.BCH -> getBchAccounts()
        }
    }

    internal fun updateAccounts(cryptoCurrency: CryptoCurrencies) {
        val accountList = getAccounts(cryptoCurrency)
        view.updateAccountsDataSet(accountList)
    }

    internal fun onAccountChosen(position: Int) {
        val account = getAccounts(currencyState.cryptoCurrency).get(position)
        view.onTotalBalanceUpdated(account.displayBalance)
    }

    //region Account Lists
    @VisibleForTesting
    internal fun getBtcAccounts(): MutableList<ItemAccount2> {
        val result = mutableListOf<ItemAccount2>()

        val legacyAddresses = payloadDataManager.legacyAddresses
                .filter { it.tag != LegacyAddress.ARCHIVED_ADDRESS }

        val accounts = payloadDataManager.accounts
                .filter { !it.isArchived }
                .map {
                    val displayBalance = getBtcBalanceString(
                            currencyState.isDisplayingCryptoCurrency,
                            payloadDataManager.getAddressBalance(it.xpub).toLong())
                    ItemAccount2(it.label,
                            displayBalance)
                }

        // Show "All Accounts" if necessary
        if (accounts.size > 1 || legacyAddresses.isNotEmpty()) {
            val bigIntBalance = payloadDataManager.walletBalance

            val displayBalance = getBtcBalanceString(
                    currencyState.isDisplayingCryptoCurrency,
                    bigIntBalance.toLong())
            result.add(ItemAccount2(stringUtils.getString(R.string.all_accounts),
                    displayBalance))
        }

        result.addAll(accounts)

        // Show "Imported Addresses" if wallet contains legacy addresses
        if (!legacyAddresses.isEmpty()) {
            val bigIntBalance = payloadDataManager.importedAddressesBalance

            val displayBalance = getBtcBalanceString(
                    currencyState.isDisplayingCryptoCurrency,
                    bigIntBalance.toLong())

            result.add(ItemAccount2(stringUtils.getString(R.string.imported_addresses),
                    displayBalance))
        }

        return result
    }

    @VisibleForTesting
    internal fun getEthAccounts(): MutableList<ItemAccount2> {
        val result = mutableListOf<ItemAccount2>()

        val label = stringUtils.getString(R.string.eth_default_account_label)
//        absoluteBalance = ethDataManager.getEthResponseModel()?.getTotalBalance()?.toLong() ?: 0L
        val displayBalance = getEthBalanceString(
                currencyState.isDisplayingCryptoCurrency,
                BigDecimal(ethDataManager.getEthResponseModel()?.getTotalBalance() ?: BigInteger.ZERO)
        )

        result.add(ItemAccount2(label, displayBalance))

        return result
    }

    @VisibleForTesting
    internal fun getBchAccounts(): MutableList<ItemAccount2> {
        val result = mutableListOf<ItemAccount2>()

        val legacyAddresses = payloadDataManager.legacyAddresses
                .filter { it.tag != LegacyAddress.ARCHIVED_ADDRESS }

        val accounts = bchDataManager.getActiveAccounts()
                .map {
                    val displayBalance = getBtcBalanceString(
                            currencyState.isDisplayingCryptoCurrency,
                            bchDataManager.getAddressBalance(it.xpub).toLong())
                    ItemAccount2(it.label, displayBalance)
                }

        // Show "All Accounts" if necessary
        if (accounts.size > 1 || legacyAddresses.isNotEmpty()) {
            val bigIntBalance = bchDataManager.getWalletBalance()

            val displayBalance = getBtcBalanceString(
                    currencyState.isDisplayingCryptoCurrency,
                    bigIntBalance.toLong())
            result.add(ItemAccount2(stringUtils.getString(R.string.bch_all_accounts), displayBalance))
        }

        result.addAll(accounts)

        // Show "Imported Addresses" if wallet contains legacy addresses
        if (!legacyAddresses.isEmpty()) {
            val bigIntBalance = bchDataManager.getImportedAddressBalance()

            val displayBalance = getBtcBalanceString(
                    currencyState.isDisplayingCryptoCurrency,
                    bigIntBalance.toLong())

            result.add(ItemAccount2(stringUtils.getString(R.string.bch_imported_addresses), displayBalance))
        }

        return result
    }
    //endregion
    //endregion

    /*
    Swipe down force refresh
     */
    internal fun onRefreshRequested() {
    }

    /*
    Toggle between fiat - crypto currency
     */
    internal fun invertViewType() = setViewType(!currencyState.isDisplayingCryptoCurrency)

    /*
    Set fiat or crypto currency state
     */
    internal fun setViewType(showCrypto: Boolean) {

        currencyState.isDisplayingCryptoCurrency = showCrypto

        view.updateCurrencyType(showCrypto, getBtcUnitType())

        updateAdapters(currencyState.cryptoCurrency)
    }

    internal fun updateAdapters(cryptoCurrency: CryptoCurrencies) {
        Timber.d("vos updateAdapters")

        updateAccounts(cryptoCurrency)

        //Update balances in account list
        //        if (chosenAccount?.type == ItemAccount.TYPE.ETHEREUM) {
//            view.onTotalBalanceUpdated(
//                    getEthBalanceString(showCrypto, BigDecimal(chosenAccount?.absoluteBalance ?: 0L)))
//        } else {
//            view.onTotalBalanceUpdated(
//                    getBtcBalanceString(showCrypto, chosenAccount?.absoluteBalance ?: 0L))
//        }

        //Update txs in tx list

        //Update curency?

//        view.updateAdapters()
    }

    //region Helper methods
    private fun getBtcBalanceString(isBtc: Boolean, btcBalance: Long): String {
        val strFiat = getFiatCurrency()
        val fiatBalance = exchangeRateFactory.getLastBtcPrice(strFiat) * (btcBalance / 1e8)
        var balance = monetaryUtil.getDisplayAmountWithFormatting(btcBalance)
        // Replace 0.0 with 0 to match web
        if (balance == "0.0") balance = "0"

        return if (isBtc) {
            "$balance ${getBtcDisplayUnits()}"
        } else {
            "${monetaryUtil.getFiatFormat(strFiat).format(fiatBalance)} $strFiat"
        }
    }

    private fun getEthBalanceString(isEth: Boolean, ethBalance: BigDecimal): String {
        val strFiat = getFiatCurrency()
        val fiatBalance = BigDecimal.valueOf(exchangeRateFactory.getLastEthPrice(strFiat))
                .multiply(Convert.fromWei(ethBalance, Convert.Unit.ETHER))
        val number = DecimalFormat.getInstance().apply { maximumFractionDigits = 8 }
                .run { format(Convert.fromWei(ethBalance, Convert.Unit.ETHER)) }

        return if (isEth) {
            "$number ETH"
        } else {
            "${monetaryUtil.getFiatFormat(strFiat).format(fiatBalance.toDouble())} $strFiat"
        }
    }

    private fun getFiatCurrency() =
        prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

    private fun getBtcDisplayUnits() = monetaryUtil.getBtcUnits()[getBtcUnitType()]

    private fun getBtcUnitType() =
        prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
    //endregion
}