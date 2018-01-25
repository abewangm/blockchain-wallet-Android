package piuk.blockchain.android.ui.balance

import android.annotation.SuppressLint
import android.support.annotation.VisibleForTesting
import info.blockchain.wallet.ethereum.data.EthAddressResponse
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Completable
import io.reactivex.Observable
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.exchange.BuyDataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.ui.account.ItemAccount
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

        ethDataManager.fetchEthAddress()
                .doOnError { Timber.e(it) }
                .onExceptionResumeNext { Observable.empty<EthAddressResponse>() }
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .doOnSubscribe {
                    onAccountsAdapterSetup()
                    onTxFeedAdapterSetup()
                }
                .doOnComplete { refreshAllObservables() }
                .subscribe(
                        {
                            //no-op
                        },
                        { Timber.e(it) }
                )
    }

    /**
     * Do all API calls to reload page
     */
    internal fun refreshAllObservables() {

        val account = getCurrenctAccount()

        getUpdateTickerCompletable()
                .andThen(updateBalancesCompletable())
                .andThen(updateTransactionsListCompletable(account))
                .doOnSubscribe { view.setUiState(UiState.LOADING) }
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doOnError { Timber.e(it) }
                .doOnComplete {
                    refreshBalanceHeader(account)
                    refreshAccountDataSet()
                    setViewType(currencyState.isDisplayingCryptoCurrency)

                }
                .subscribe(
                        { /* No-op */ },
                        { view.setUiState(UiState.FAILURE) })
    }

    private fun getUpdateTickerCompletable(): Completable {
        return Completable.fromObservable(exchangeRateFactory.updateTickers())
    }

    //region api refresh calls
    /**
     * API call - Fetches latest balance for selected currency and updates UI balance
     */
    private fun updateBalancesCompletable() =
            when (currencyState.cryptoCurrency) {
                CryptoCurrencies.BTC -> payloadDataManager.updateAllBalances()
                CryptoCurrencies.ETHER -> Completable.fromObservable(ethDataManager.fetchEthAddress())
                CryptoCurrencies.BCH -> bchDataManager.updateAllBalances()
            }

    /**
     * API call - Fetches latest transactions for selected currency and account, and updates UI tx list
     */
    private fun updateTransactionsListCompletable(account: ItemAccount): Completable {

        return Completable.fromObservable(
//            getShapeShiftTxNotesObservable()
                transactionListDataManager.fetchTransactions(account, 50, 0)
//                    .doAfterTerminate(this::storeSwipeReceiveAddresses)
                        .map {
                            for (tx in it) {

                                when (currencyState.cryptoCurrency) {
                                    CryptoCurrencies.BTC -> {
                                        tx.totalDisplayableCrypto = getBtcBalanceString(
                                                true,
                                                tx.total.toLong())
                                        tx.totalDisplayableFiat = getBtcBalanceString(
                                                false,
                                                tx.total.toLong())
                                    }
                                    CryptoCurrencies.ETHER -> {
                                        tx.totalDisplayableCrypto = getEthBalanceString(
                                                true,
                                                BigDecimal(tx.total))
                                        tx.totalDisplayableFiat = getEthBalanceString(
                                                false,
                                                BigDecimal(tx.total))
                                    }
                                    CryptoCurrencies.BCH -> {
                                        tx.totalDisplayableCrypto = getBchBalanceString(
                                                true,
                                                tx.total.toLong())
                                        tx.totalDisplayableCrypto = getBchBalanceString(
                                                false,
                                                tx.total.toLong())
                                    }

                                }
                            }

                            when {
                                it.isEmpty() -> { view.setUiState(UiState.EMPTY) }
                                else -> { view.setUiState(UiState.CONTENT) }
                            }

                            view.updateTransactionDataSet(currencyState.isDisplayingCryptoCurrency, it)
                        })
    }
    //endregion

    //region Currency header

    /*
    Currency selected from dropdown
     */
    internal fun onCurrencySelected(cryptoCurrency: CryptoCurrencies) {
        // Set new currency state
        currencyState.cryptoCurrency = cryptoCurrency

        //Select default account for this currency
        val account = getAccounts(cryptoCurrency).get(0)

        updateTransactionsListCompletable(account)
                .doOnSubscribe { view.setUiState(UiState.LOADING) }
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doOnError { Timber.e(it) }
                .doOnComplete {
                    refreshBalanceHeader(account)
                    refreshAccountDataSet()
                    view.selectDefaultAccount()
                }
                .subscribe(
                        { /* No-op */ },
                        { view.setUiState(UiState.FAILURE) })
    }

    //endregion

    //region Accounts drop down
    /*
    Fetch all active accounts for initial selected currency and set up account adapter
     */
    fun onAccountsAdapterSetup() {
        view.setupAccountsAdapter(getAccounts(currencyState.cryptoCurrency))
    }

    internal fun getAccounts(currency: CryptoCurrencies): MutableList<ItemAccount> {
        return when (currency) {
            CryptoCurrencies.BTC -> getBtcAccounts()
            CryptoCurrencies.ETHER -> getEthAccounts()
            CryptoCurrencies.BCH -> getBchAccounts()
        }
    }

    internal fun refreshAccountDataSet() {
        val accountList = getAccounts(currencyState.cryptoCurrency)
        view.updateAccountsDataSet(accountList)
    }

//    internal fun updateTransactions(cryptoCurrency: CryptoCurrencies) {
//        val accountList = transactionListDataManager(cryptoCurrency)
//        view.updateAccountsDataSet(accountList)
//    }

    internal fun onAccountSelected(position: Int) {

        val account = getAccounts(currencyState.cryptoCurrency).get(position)

        updateTransactionsListCompletable(account)
                .doOnSubscribe { view.setUiState(UiState.LOADING) }
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doOnError { Timber.e(it) }
                .doOnComplete {
                    refreshBalanceHeader(account)
                    refreshAccountDataSet()
                }
                .subscribe(
                        { /* No-op */ },
                        { view.setUiState(UiState.FAILURE) })
    }

    /*
    Don't over use this method. It's a bit hacky, but fast enough to work.
     */
    internal fun getCurrenctAccount(): ItemAccount {
        return getAccounts(currencyState.cryptoCurrency).get(view.getCurrentAccountPosition())
    }

    //region Account Lists
    @VisibleForTesting
    internal fun getBtcAccounts(): MutableList<ItemAccount> {
        val result = mutableListOf<ItemAccount>()

        val legacyAddresses = payloadDataManager.legacyAddresses
                .filter { it.tag != LegacyAddress.ARCHIVED_ADDRESS }

        val accounts = payloadDataManager.accounts
                .filter { !it.isArchived }
                .map {
                    val balance = getBtcBalanceString(
                            currencyState.isDisplayingCryptoCurrency,
                            payloadDataManager.getAddressBalance(it.xpub).toLong())
                    ItemAccount().apply {
                        label = it.label
                        displayBalance = balance
                        address = it.xpub
                        type = ItemAccount.TYPE.SINGLE_ACCOUNT
                    }
                }

        // Show "All Accounts" if necessary
        if (accounts.size > 1 || legacyAddresses.isNotEmpty()) {
            val bigIntBalance = payloadDataManager.walletBalance

            val balance = getBtcBalanceString(
                    currencyState.isDisplayingCryptoCurrency,
                    bigIntBalance.toLong())
            result.add(
                    ItemAccount().apply {
                        label = stringUtils.getString(R.string.all_accounts)
                        displayBalance = balance
                        type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
                    })
        }

        result.addAll(accounts)

        // Show "Imported Addresses" if wallet contains legacy addresses
        if (!legacyAddresses.isEmpty()) {
            val bigIntBalance = payloadDataManager.importedAddressesBalance

            val balance = getBtcBalanceString(
                    currencyState.isDisplayingCryptoCurrency,
                    bigIntBalance.toLong())

            result.add(
                    ItemAccount().apply {
                        label = stringUtils.getString(R.string.imported_addresses)
                        displayBalance = balance
                        type = ItemAccount.TYPE.ALL_LEGACY
                    })
        }

        return result
    }

    @VisibleForTesting
    internal fun getEthAccounts(): MutableList<ItemAccount> {
        val result = mutableListOf<ItemAccount>()

        val balance = getEthBalanceString(
                currencyState.isDisplayingCryptoCurrency,
                BigDecimal(ethDataManager.getEthResponseModel()?.getTotalBalance() ?: BigInteger.ZERO)
        )

        result.add(
                ItemAccount().apply {
                    label = stringUtils.getString(R.string.eth_default_account_label)
                    displayBalance = balance
                    type = ItemAccount.TYPE.SINGLE_ACCOUNT
                })

        return result
    }

    @VisibleForTesting
    internal fun getBchAccounts(): MutableList<ItemAccount> {
        val result = mutableListOf<ItemAccount>()

        val legacyAddresses = payloadDataManager.legacyAddresses
                .filter { it.tag != LegacyAddress.ARCHIVED_ADDRESS }

        val accounts = bchDataManager.getActiveAccounts()
                .map {

                    val balance = getBchBalanceString(
                            currencyState.isDisplayingCryptoCurrency,
                            bchDataManager.getAddressBalance(it.xpub).toLong())
                    ItemAccount().apply {
                        label = it.label
                        displayBalance = balance
                        type = ItemAccount.TYPE.SINGLE_ACCOUNT
                        address = it.xpub
                    }
                }

        // Show "All Accounts" if necessary
        if (accounts.size > 1 || legacyAddresses.isNotEmpty()) {
            val bigIntBalance = bchDataManager.getWalletBalance()

            val balance = getBchBalanceString(
                    currencyState.isDisplayingCryptoCurrency,
                    bigIntBalance.toLong())
            result.add(ItemAccount().apply {
                label = stringUtils.getString(R.string.bch_all_accounts)
                displayBalance = balance
                type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
            })
        }

        result.addAll(accounts)

        // Show "Imported Addresses" if wallet contains legacy addresses
        if (!legacyAddresses.isEmpty()) {
            val bigIntBalance = bchDataManager.getImportedAddressBalance()

            val balance = getBchBalanceString(
                    currencyState.isDisplayingCryptoCurrency,
                    bigIntBalance.toLong())

            result.add(
                    ItemAccount().apply {
                        label = stringUtils.getString(R.string.bch_imported_addresses)
                        displayBalance = balance
                        type = ItemAccount.TYPE.ALL_LEGACY
                    })
        }

        return result
    }
    //endregion
    //endregion

    //region Transaction List
    fun onTxFeedAdapterSetup() {
        view.setupTxFeedAdapter(currencyState.isDisplayingCryptoCurrency)
    }
    //endregion

    /*
    Swipe down force refresh
     */
    internal fun onRefreshRequested() {
    }

    /*
    Toggle between fiat - crypto currency
     */
    internal fun onBalanceClick() = setViewType(!currencyState.isDisplayingCryptoCurrency)

    /*
    Set fiat or crypto currency state
     */
    internal fun setViewType(showCrypto: Boolean) {
        //Set new currency state
        currencyState.isDisplayingCryptoCurrency = showCrypto

        //Update balance header
        refreshBalanceHeader(getCurrenctAccount())

        //Update tx list balances
        view.updateTransactionValueType(showCrypto)

        //Update accounts data set
        refreshAccountDataSet()
    }

    internal fun refreshBalanceHeader(account: ItemAccount) {
        view.updateSelectedCurrency(currencyState.cryptoCurrency)
        view.updateBalanceHeader(account.displayBalance!!)
    }

    //region Helper methods
    private fun getBtcBalanceString(showCrypto: Boolean, btcBalance: Long): String {
        val strFiat = getFiatCurrency()
        val fiatBalance = exchangeRateFactory.getLastBtcPrice(strFiat) * (btcBalance / 1e8)
        var balance = monetaryUtil.getDisplayAmountWithFormatting(btcBalance)
        // Replace 0.0 with 0 to match web
        if (balance == "0.0") balance = "0"

        return if (showCrypto) {
            "$balance ${getBtcDisplayUnits()}"
        } else {
            "${monetaryUtil.getFiatFormat(strFiat).format(fiatBalance)} $strFiat"
        }
    }

    private fun getEthBalanceString(showCrypto: Boolean, ethBalance: BigDecimal): String {
        val strFiat = getFiatCurrency()
        val fiatBalance = BigDecimal.valueOf(exchangeRateFactory.getLastEthPrice(strFiat))
                .multiply(Convert.fromWei(ethBalance, Convert.Unit.ETHER))
        val number = DecimalFormat.getInstance().apply { maximumFractionDigits = 8 }
                .run { format(Convert.fromWei(ethBalance, Convert.Unit.ETHER)) }

        return if (showCrypto) {
            "$number ETH"
        } else {
            "${monetaryUtil.getFiatFormat(strFiat).format(fiatBalance.toDouble())} $strFiat"
        }
    }

    private fun getBchBalanceString(showCrypto: Boolean, bchBalance: Long): String {
        val strFiat = getFiatCurrency()
        val fiatBalance = exchangeRateFactory.getLastBchPrice(strFiat) * (bchBalance / 1e8)
        var balance = monetaryUtil.getDisplayAmountWithFormatting(bchBalance)
        // Replace 0.0 with 0 to match web
        if (balance == "0.0") balance = "0"

        return if (showCrypto) {
            "$balance ${getBchDisplayUnits()}"
        } else {
            "${monetaryUtil.getFiatFormat(strFiat).format(fiatBalance)} $strFiat"
        }
    }

    private fun getFiatCurrency() =
        prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

    private fun getBtcDisplayUnits() = monetaryUtil.getBtcUnits()[getBtcUnitType()]

    private fun getBchDisplayUnits() = monetaryUtil.getBchUnits()[getBtcUnitType()]

    private fun getBtcUnitType() =
        prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)

    //endregion

    private fun getShapeShiftTxNotesObservable() =
            shapeShiftDataManager.getTradesList()
                    .compose(RxUtil.addObservableToCompositeDisposable(this))
                    .map {
                        val map: MutableMap<String, String> = mutableMapOf()

                        for (trade in it) {
                            trade.hashIn?.let {
                                map.put(trade.hashIn, stringUtils.getString(R.string.shapeshift_deposit_to))
                            }
                            trade.hashOut?.let {
                                map.put(trade.hashOut, stringUtils.getString(R.string.shapeshift_deposit_from))
                            }
                        }
                        return@map map
                    }
                    .doOnError { Timber.e(it) }
                    .onErrorReturn { mutableMapOf() }

}