package piuk.blockchain.android.ui.balance

import android.annotation.SuppressLint
import android.support.annotation.VisibleForTesting
import info.blockchain.wallet.ethereum.data.EthAddressResponse
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
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

    @VisibleForTesting var notificationObservable: Observable<NotificationPayload>? = null
    @VisibleForTesting var authEventObservable: Observable<AuthEvent>? = null

    private val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(getBtcUnitType()) }

    //region Life cycle
    @SuppressLint("VisibleForTests")
    override fun onViewReady() {
        onAccountsAdapterSetup()
        onTxFeedAdapterSetup()
        subscribeToEvents()
    }

    internal fun onResume() {
        onRefreshRequested()
    }

    override fun onViewDestroyed() {
        notificationObservable?.let { rxBus.unregister(NotificationPayload::class.java, it) }
        authEventObservable?.let { rxBus.unregister(AuthEvent::class.java, it) }
        super.onViewDestroyed()
    }

    private fun subscribeToEvents() {

        authEventObservable = rxBus.register(AuthEvent::class.java).apply {
            subscribe({
                //Clear tx feed
                view.updateTransactionDataSet(currencyState.isDisplayingCryptoCurrency, mutableListOf())
                transactionListDataManager.clearTransactionList()
            })
        }

        notificationObservable = rxBus.register(NotificationPayload::class.java).apply {
            subscribe({ notificationPayload ->
                //no-op
            })
        }
    }
    //endregion

    //region API calls
    /**
     * Do all API calls to reload page
     */
    private fun refreshAllCompletable(account: ItemAccount): Completable {
        return getUpdateTickerCompletable()
                .andThen(updateBchWallet())
                .andThen(updateEthAddress())
                .andThen(updateBalancesCompletable())
                .andThen(updateTransactionsListCompletable(account))
                //If 1 fails everything is fucked, don't do this
                .doOnError { view.setUiState(UiState.FAILURE) }
                .doOnSubscribe { view.setUiState(UiState.LOADING) }
                .doOnComplete {
                    refreshBalanceHeader(account)
                    refreshAccountDataSet()
                    refreshLauncherShortcuts()
                    setViewType(currencyState.isDisplayingCryptoCurrency)
                }
    }

    /*
    onResume and Swipe down force refresh
     */
    internal fun onRefreshRequested() {
        refreshAllCompletable(getCurrenctAccount())
                .doOnError { Timber.e(it) }
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .subscribe(
                        { /* No-op */ },
                        { Timber.e(it) }
                )
    }

    private fun getUpdateTickerCompletable(): Completable {
        return Completable.fromObservable(exchangeRateFactory.updateTickers())
    }

    /**
     * API call - Update eth address
     */
    private fun updateEthAddress() =
        Completable.fromObservable(ethDataManager.fetchEthAddress()
                .onExceptionResumeNext { Observable.empty<EthAddressResponse>() })

    /**
     * API call - Update bitcoincash wallet
     */
    private fun updateBchWallet() = bchDataManager.refreshMetadataCompletable()
            .doOnError{ Timber.e(it) }
            .compose(RxUtil.applySchedulersToCompletable())

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
                transactionListDataManager.fetchTransactions(account, 50, 0)
                        .doAfterTerminate(this::storeSwipeReceiveAddresses)
                        .map { txs ->

                            getShapeShiftTxNotesObservable()
                                    .compose(RxUtil.addObservableToCompositeDisposable(this))
                                    .subscribe(
                                            { shapeShiftNotesMap ->
                                                for (tx in txs) {

                                                    //Add shapeShift notes
                                                    shapeShiftNotesMap[tx.hash]?.let {
                                                        tx.note = it
                                                    }

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
                                                            tx.totalDisplayableFiat = getBchBalanceString(
                                                                    false,
                                                                    tx.total.toLong())
                                                        }

                                                    }
                                                }

                                                when {
                                                    txs.isEmpty() -> {
                                                        view.setUiState(UiState.EMPTY)
                                                    }
                                                    else -> {
                                                        view.setUiState(UiState.CONTENT)
                                                    }
                                                }

                                                view.updateTransactionDataSet(currencyState.isDisplayingCryptoCurrency, txs)
                                            }
                                            ,
                                            { Timber.e(it) })
                        })
    }

    //TODO This should replace updateTransactionsListCompletable, but can't get it to work properly
    private fun updateTransactionsListCompletable2(account: ItemAccount): Completable {

        return Completable.fromObservable(
                Observable.zip(
                        getShapeShiftTxNotesObservable(),
                        transactionListDataManager.fetchBchTransactions(account, 50, 0),
                        BiFunction { shapeShiftNotesMap: MutableMap<String, String>, txs: List<Displayable> ->
                            {
                                for (tx in txs) {

                                    //Add shapeShift notes
                                    shapeShiftNotesMap[tx.hash]?.let {
                                        tx.note = it
                                    }

                                    //Display currencies
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
                                            tx.totalDisplayableFiat = getBchBalanceString(
                                                    false,
                                                    tx.total.toLong())
                                        }

                                    }
                                }

                                when {
                                    txs.isEmpty() -> {
                                        view.setUiState(UiState.EMPTY)
                                    }
                                    else -> {
                                        view.setUiState(UiState.CONTENT)
                                    }
                                }

                                view.updateTransactionDataSet(currencyState.isDisplayingCryptoCurrency, txs)
                            }
                        }).doAfterTerminate(this::storeSwipeReceiveAddresses))
    }
    //endregion

    //region Incoming UI events
    /*
    Currency selected from dropdown
     */
    internal fun onCurrencySelected(cryptoCurrency: CryptoCurrencies) {
        // Set new currency state
        currencyState.cryptoCurrency = cryptoCurrency

        //Select default account for this currency
        val account = getAccounts().get(0)

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

    internal fun onGetBitcoinClicked() {
        buyDataManager.canBuy
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe({
                    if (it) {
                        view.startBuyActivity()
                    } else {
                        view.startReceiveFragmentBtc()
                    }
                }, { Timber.e(it) })
    }

    /*
    Fetch all active accounts for initial selected currency and set up account adapter
     */
    internal fun onAccountsAdapterSetup() {
        view.setupAccountsAdapter(getAccounts())
    }

    internal fun onAccountSelected(position: Int) {

        val account = getAccounts().get(position)

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

    /*
    Toggle between fiat - crypto currency
     */
    internal fun onBalanceClick() = setViewType(!currencyState.isDisplayingCryptoCurrency)
    //endregion

    //region Update UI
    private fun refreshBalanceHeader(account: ItemAccount) {
        view.updateSelectedCurrency(currencyState.cryptoCurrency)
        view.updateBalanceHeader(account.displayBalance!!)
    }

    private fun refreshAccountDataSet() {
        val accountList = getAccounts()
        view.updateAccountsDataSet(accountList)
    }

    private fun refreshLauncherShortcuts() {
        view.generateLauncherShortcuts()
    }
    //endregion

    //region Adapter data
    //region Account Lists
    @VisibleForTesting
    private fun getBtcAccounts(): MutableList<ItemAccount> {
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
    private fun getEthAccounts(): MutableList<ItemAccount> {
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
    private fun getBchAccounts(): MutableList<ItemAccount> {
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

    //region Transaction List
    fun onTxFeedAdapterSetup() {
        view.setupTxFeedAdapter(currencyState.isDisplayingCryptoCurrency)
    }
    //endregion

    /**
     * Get accounts based on selected currency
     */
    private fun getAccounts(): MutableList<ItemAccount> {
        return when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> getBtcAccounts()
            CryptoCurrencies.ETHER -> getEthAccounts()
            CryptoCurrencies.BCH -> getBchAccounts()
        }
    }

    private fun getCurrenctAccount(): ItemAccount {
        return getAccountAt(view.getCurrentAccountPosition() ?: 0)
    }

    /*
    Don't over use this method. It's a bit hacky, but fast enough to work.
     */
    private fun getAccountAt(position: Int): ItemAccount {
        return getAccounts().get(position)
    }

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

    private fun storeSwipeReceiveAddresses() {
        // Defer to background thread as deriving addresses is quite processor intensive
        Completable.fromCallable {
            swipeToReceiveHelper.updateAndStoreBitcoinAddresses()
            swipeToReceiveHelper.updateAndStoreBitcoinCashAddresses()
            Void.TYPE
        }.subscribeOn(Schedulers.computation())
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .subscribe(
                        { /* No-op */ },
                        { Timber.e(it) })
    }
    //endregion

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

    internal fun areLauncherShortcutsEnabled() =
            prefsUtil.getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true)

    internal fun getCurrentCurrency() = currencyState.cryptoCurrency
    //endregion
}