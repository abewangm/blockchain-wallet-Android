//package piuk.blockchain.android.ui.balance
//
//import android.annotation.SuppressLint
//import android.support.annotation.VisibleForTesting
//import info.blockchain.wallet.ethereum.data.EthAddressResponse
//import info.blockchain.wallet.payload.data.LegacyAddress
//import info.blockchain.wallet.prices.data.PriceDatum
//import io.reactivex.Completable
//import io.reactivex.Observable
//import io.reactivex.schedulers.Schedulers
//import org.web3j.utils.Convert
//import piuk.blockchain.android.R
//import piuk.blockchain.android.data.access.AuthEvent
//import piuk.blockchain.android.data.bitcoincash.BchDataManager
//import piuk.blockchain.android.data.currency.CryptoCurrencies
//import piuk.blockchain.android.data.currency.CurrencyState
//import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
//import piuk.blockchain.android.data.ethereum.EthDataManager
//import piuk.blockchain.android.data.exchange.BuyDataManager
//import piuk.blockchain.android.data.notifications.models.NotificationPayload
//import piuk.blockchain.android.data.payload.PayloadDataManager
//import piuk.blockchain.android.data.rxjava.RxBus
//import piuk.blockchain.android.data.rxjava.RxUtil
//import piuk.blockchain.android.data.shapeshift.ShapeShiftDataManager
//import piuk.blockchain.android.data.transactions.Displayable
//import piuk.blockchain.android.ui.account.ItemAccount
//import piuk.blockchain.android.ui.base.BasePresenter
//import piuk.blockchain.android.ui.base.UiState
//import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
//import piuk.blockchain.android.util.ExchangeRateFactory
//import piuk.blockchain.android.util.MonetaryUtil
//import piuk.blockchain.android.util.PrefsUtil
//import piuk.blockchain.android.util.StringUtils
//import piuk.blockchain.android.util.helperfunctions.unsafeLazy
//import timber.log.Timber
//import java.math.BigDecimal
//import java.math.BigInteger
//import java.text.DecimalFormat
//import javax.inject.Inject
//
//class BalancePresenter2 @Inject constructor(
//        private val exchangeRateFactory: ExchangeRateFactory,
//        private val transactionListDataManager: TransactionListDataManager,
//        private val ethDataManager: EthDataManager,
//        private val swipeToReceiveHelper: SwipeToReceiveHelper,
//        internal val payloadDataManager: PayloadDataManager,
//        private val buyDataManager: BuyDataManager,
//        private val stringUtils: StringUtils,
//        private val prefsUtil: PrefsUtil,
//        private val rxBus: RxBus,
//        private val currencyState: CurrencyState,
//        private val shapeShiftDataManager: ShapeShiftDataManager,
//        private val bchDataManager: BchDataManager
//) : BasePresenter<BalanceView>() {
//
//    @VisibleForTesting var notificationObservable: Observable<NotificationPayload>? = null
//    @VisibleForTesting var authEventObservable: Observable<AuthEvent>? = null
//    @VisibleForTesting var chosenAccount: ItemAccount? = null
//
//    @VisibleForTesting val currencyAccountMap: MutableMap<CryptoCurrencies, MutableList<ItemAccount>> = mutableMapOf()
//    private val displayList: MutableList<Any> = mutableListOf()
                //    private val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(getBtcUnitType()) }
//    private var txNoteMap: MutableMap<String, String> = mutableMapOf()
//
//    @SuppressLint("VisibleForTests")
//    override fun onViewReady() {
//
//        currencyState.cryptoCurrency = CryptoCurrencies.BTC
//
//        subscribeToEvents()
//        updateCurrencyUi(currencyState.cryptoCurrency)
//
//        ethDataManager.fetchEthAddress()
//                .doOnError { Timber.e(it) }
//                .onExceptionResumeNext { Observable.empty<EthAddressResponse>() }
//                .compose(RxUtil.addObservableToCompositeDisposable(this))
//                .doOnNext {
//                    currencyAccountMap.clear()
//                    currencyAccountMap.putAll(getAllDisplayableAccounts())
//
//                    Timber.d("vos Stuff we put in")
//                    for (item in currencyAccountMap) {
//                        Timber.d("vos key: "+item.key)
//                        for(v in item.value) {
//                            Timber.d("vos value: " + v)
//                        }
//                    }
//
//                }
//                .flatMap { getShapeShiftTxNotesObservable() }
//                .doOnNext { txNoteMap.putAll(it) }
//                .doOnComplete { setupTransactions() }
//                .subscribe(
//                        { updateSelectedCurrency(currencyState.cryptoCurrency) },
//                        { Timber.e(it) }
//                )
//    }
//
//    override fun onViewDestroyed() {
//        notificationObservable?.let { rxBus.unregister(NotificationPayload::class.java, it) }
//        authEventObservable?.let { rxBus.unregister(AuthEvent::class.java, it) }
//        super.onViewDestroyed()
//    }
//
//    internal fun onResume() {
//        // Here we check the Fiat and Btc formats and let the UI handle any potential updates
//        val btcUnitType = getBtcUnitType()
//        monetaryUtil.updateUnit(btcUnitType)
//        view.onExchangeRateUpdated(
//                getLastBtcPrice(getFiatCurrency()),
//                getLastEthPrice(getFiatCurrency()),
//                currencyState.isDisplayingCryptoCurrency,
//                txNoteMap
//        )
//        view.onViewTypeChanged(currencyState.isDisplayingCryptoCurrency, btcUnitType)
//    }
//
//    // TODO: This will need updating for BCH
//    internal fun chooseDefaultAccount() {
////        if (currencyState.cryptoCurrency == CryptoCurrencies.ETHER) {
////            onAccountChosen(activeAccountAndAddressList.lastIndex)
////        } else {
//            onAccountChosen(0)
////        }
//    }
                //
                //    internal fun onAccountChosen(position: Int) {
                //
                //        Timber.d("vos currencyState.cryptoCurrency: "+currencyState.cryptoCurrency)
                //        val accountList = currencyAccountMap.get(currencyState.cryptoCurrency) ?: mutableListOf()
                //
                //        Timber.d("vos accountList size: "+accountList.size)
                //        Timber.d("vos accountList: "+accountList)
                //
                //        if (position - 1 > accountList.size || accountList.isEmpty()) {
                //            Timber.d("activeAccountAndAddressList.size == 0")
                //            return
                //        }
                //
                //        view.setUiState(UiState.LOADING)
                //        chosenAccount = accountList[if (position >= 0) position else 0]
                //
                //        Timber.d("vos chosenAccount: "+chosenAccount?.label)
                //
                //        view.updateAccountList(accountList)
                //
                //        chosenAccount?.let {
                //            Observable.merge(
                //                    getBalanceObservable(it),
                //                    getTransactionsListObservable(it)
                //            ).compose(RxUtil.addObservableToCompositeDisposable(this))
                //                    .doOnError { Timber.e(it) }
                //                    .subscribe(
                //                            { /* No-op */ },
                //                            { view.setUiState(UiState.FAILURE) })
                //        }
                //    }
                //
                //    internal fun onRefreshRequested() {
                //        chosenAccount?.let {
                //            Observable.merge(
                //                    getBalanceObservable(it),
                //                    getTransactionsListObservable(it)
                //            ).compose(RxUtil.addObservableToCompositeDisposable(this))
                //                    .doOnError { Timber.e(it) }
                //                    .subscribe(
                //                            { /* No-op */ },
                //                            { view.setUiState(UiState.FAILURE) })
                //        }
                //    }
                //
                //    internal fun setViewType(isBtc: Boolean) {
                //        currencyState.isDisplayingCryptoCurrency = isBtc
                //        view.onViewTypeChanged(isBtc, getBtcUnitType())
                //        if (chosenAccount?.type == ItemAccount.TYPE.ETHEREUM) {
                //            view.onTotalBalanceUpdated(
                //                    getEthBalanceString(isBtc, BigDecimal(chosenAccount?.absoluteBalance ?: 0L)))
                //        } else {
                //            view.onTotalBalanceUpdated(
                //                    getBtcBalanceString(isBtc, chosenAccount?.absoluteBalance ?: 0L))
                //        }
                //    }
                //
                //    internal fun invertViewType() = setViewType(!currencyState.isDisplayingCryptoCurrency)
//
//    internal fun areLauncherShortcutsEnabled() =
//            prefsUtil.getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true)
//
//    internal fun getBitcoinClicked() {
//        if (view.shouldShowBuy) {
//            buyDataManager.canBuy
//                    .compose(RxUtil.addObservableToCompositeDisposable(this))
//                    .subscribe({
//                        if (it) {
//                            view.startBuyActivity()
//                        } else {
//                            view.startReceiveFragment()
//                        }
//                    }, { Timber.e(it) })
//        } else {
//            view.startReceiveFragment()
//        }
//    }
//
                //    internal fun updateSelectedCurrency(cryptoCurrency: CryptoCurrencies) {
                //        currencyState.cryptoCurrency = cryptoCurrency
                //
                ////        when (cryptoCurrency) {
                ////            CryptoCurrencies.BTC -> onAccountChosen(0)
                ////            CryptoCurrencies.ETHER -> onAccountChosen(activeAccountAndAddressList.lastIndex)
                ////            else -> throw IllegalArgumentException("BCH is not currently supported")
                ////        }
                //        onAccountChosen(0)
                //
                //        updateCurrencyUi(cryptoCurrency)
                //    }
//
//    @VisibleForTesting
//    internal fun getAllDisplayableAccounts(): MutableMap<CryptoCurrencies, MutableList<ItemAccount>> {
//
//        val btcList = mutableListOf<ItemAccount>()
//
//        val legacyAddresses = payloadDataManager.legacyAddresses
//                .filter { it.tag != LegacyAddress.ARCHIVED_ADDRESS }
//
//        val accounts = payloadDataManager.accounts
//                .filter { !it.isArchived }
//                .map {
//                    val bigIntBalance = payloadDataManager.getAddressBalance(it.xpub)
//                    ItemAccount().apply {
//                        label = it.label
//                        Timber.d("vos label: "+label)
//
//                        displayBalance = getBtcBalanceString(
//                                currencyState.isDisplayingCryptoCurrency,
//                                bigIntBalance.toLong()
//                        )
//                        absoluteBalance = bigIntBalance.toLong()
//                        address = it.xpub
//                        type = ItemAccount.TYPE.SINGLE_ACCOUNT
//                    }
//                }
//
//        // Show "All Accounts" if necessary
//        if (accounts.size > 1 || legacyAddresses.isNotEmpty()) {
//            val bigIntBalance = payloadDataManager.walletBalance
//
//            btcList.add(ItemAccount().apply {
//                label = stringUtils.getString(R.string.all_accounts)
//                Timber.d("vos label: "+label)
//                displayBalance = getBtcBalanceString(
//                        currencyState.isDisplayingCryptoCurrency,
//                        bigIntBalance.toLong()
//                )
//                absoluteBalance = bigIntBalance.toLong()
//                type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
//            })
//        }
//
//        btcList.addAll(accounts)
//
//        // Show "Imported Addresses" if wallet contains legacy addresses
//        if (!legacyAddresses.isEmpty()) {
//            val bigIntBalance = payloadDataManager.importedAddressesBalance
//
//            btcList.add(ItemAccount().apply {
//                displayBalance = getBtcBalanceString(
//                        currencyState.isDisplayingCryptoCurrency,
//                        bigIntBalance.toLong()
//                )
//                label = stringUtils.getString(R.string.imported_addresses)
//                Timber.d("vos label: "+label)
//                absoluteBalance = bigIntBalance.toLong()
//                type = ItemAccount.TYPE.ALL_LEGACY
//            })
//        }
//
//        // Add Ethereum
//        val ethMutableList = mutableListOf<ItemAccount>()
//        ethMutableList.add(ItemAccount().apply {
//            type = ItemAccount.TYPE.ETHEREUM
//            label = stringUtils.getString(R.string.eth_default_account_label)
//            Timber.d("vos label: "+label)
//            absoluteBalance = ethDataManager.getEthResponseModel()?.getTotalBalance()?.toLong() ?: 0L
//            displayBalance = getEthBalanceString(
//                    currencyState.isDisplayingCryptoCurrency,
//                    BigDecimal(ethDataManager.getEthResponseModel()?.getTotalBalance() ?: BigInteger.ZERO)
//            )
//        })
//
//        val bchList = mutableListOf<ItemAccount>()
//
//        val bchAccounts = bchDataManager.getActiveAccounts()
//                .map {
//                    val bigIntBalance = bchDataManager.getAddressBalance(it.xpub)
//                    ItemAccount().apply {
//                        label = it.label
//                        Timber.d("vos label: "+label)
//                        displayBalance = getBtcBalanceString(
//                                currencyState.isDisplayingCryptoCurrency,
//                                bigIntBalance.toLong()
//                        )
//                        absoluteBalance = bigIntBalance.toLong()
//                        address = it.xpub
//                        type = ItemAccount.TYPE.SINGLE_ACCOUNT
//                    }
//                }
//
//        // Show "All Accounts" if necessary
//        if (bchAccounts.size > 1 || legacyAddresses.isNotEmpty()) {
//            val bigIntBalance = bchDataManager.getWalletBalance()
//
//            bchList.add(ItemAccount().apply {
//                label = stringUtils.getString(R.string.bch_all_accounts)
//                Timber.d("vos label: "+label)
//                displayBalance = getBtcBalanceString(
//                        currencyState.isDisplayingCryptoCurrency,
//                        bigIntBalance.toLong()
//                )
//                absoluteBalance = bigIntBalance.toLong()
//                type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
//            })
//        }
//
//        bchList.addAll(bchAccounts)
//
//        // Show "Imported Addresses" if wallet contains legacy addresses
//        if (!legacyAddresses.isEmpty()) {
//            val bigIntBalance = payloadDataManager.importedAddressesBalance
//
//            bchList.add(ItemAccount().apply {
//                displayBalance = getBtcBalanceString(
//                        currencyState.isDisplayingCryptoCurrency,
//                        bigIntBalance.toLong()
//                )
//                label = stringUtils.getString(R.string.bch_imported_addresses)
//                absoluteBalance = bigIntBalance.toLong()
//                type = ItemAccount.TYPE.ALL_LEGACY
//            })
//        }
//
//        val result = mutableMapOf<CryptoCurrencies, MutableList<ItemAccount>>()
//        result.put(CryptoCurrencies.BTC, btcList)
//        result.put(CryptoCurrencies.ETHER, ethMutableList)
//        result.put(CryptoCurrencies.BCH, bchList)
//
//        return result
//    }
//
//    private fun updateCurrencyUi(cryptoCurrency: CryptoCurrencies) {
//        when (cryptoCurrency) {
//            CryptoCurrencies.BTC -> view.showAccountSpinner()
//            CryptoCurrencies.ETHER -> view.hideAccountSpinner()
//            CryptoCurrencies.BCH -> view.showAccountSpinner()
//        }
//
//        view.updateSelectedCurrency(cryptoCurrency)
//    }
//
//    private fun setupTransactions() {
//        chosenAccount?.let {
//            Observable.merge(
//                    getBalanceObservable(it),
//                    getTransactionsListObservable(it),
//                    getUpdateTickerObservable()
//            ).compose(RxUtil.addObservableToCompositeDisposable(this))
//                    .doOnError { Timber.e(it) }
//                    .subscribe(
//                            { /* No-op */ },
//                            { view.setUiState(UiState.FAILURE) })
//        }
//    }
//
//    private fun getTransactionsListObservable(itemAccount: ItemAccount) =
//            transactionListDataManager.fetchTransactions(itemAccount, 50, 0)
//                    .doAfterTerminate(this::storeSwipeReceiveAddresses)
//                    .doOnNext {
//                        displayList.removeAll { it is Displayable }
//                        displayList.addAll(it)
//
//                        when {
//                            displayList.isEmpty() -> view.setUiState(UiState.EMPTY)
//                            else -> view.setUiState(UiState.CONTENT)
//                        }
//                        view.onTransactionsUpdated(displayList)
//                    }
//
                //    private fun getBalanceObservable(itemAccount: ItemAccount): Observable<Nothing> {
                //        return if (chosenAccount?.type == ItemAccount.TYPE.ETHEREUM) {
                //            ethDataManager.fetchEthAddress()
                //                    .doOnNext {
                //                        val ethBalance = BigDecimal(it.getTotalBalance())
                //                        val ethString = getEthBalanceString(currencyState.isDisplayingCryptoCurrency, ethBalance)
                //                        view.onTotalBalanceUpdated(ethString)
                //                    }.flatMap { Observable.empty<Nothing>() }
                //        } else {
                //            payloadDataManager.updateAllBalances()
                //                    .doOnComplete {
                //                        val btcBalance = transactionListDataManager.getBtcBalance(itemAccount)
                //                        val balanceTotal = getBtcBalanceString(currencyState.isDisplayingCryptoCurrency, btcBalance)
                //                        view.onTotalBalanceUpdated(balanceTotal)
                //                    }.toObservable<Nothing>()
                //        }
                //    }
//
//    private fun getUpdateTickerObservable(): Observable<Map<String, PriceDatum>> {
//        // Remove ETH from list of accounts
////        val displayableAccounts = mutableListOf<ItemAccount>().apply {
////            addAll(activeAccountAndAddressList)
////            removeAt(lastIndex)
////        }
//        val displayableAccounts = currencyAccountMap.get(currencyState.cryptoCurrency) ?: mutableListOf()
//
//        Timber.d("vos getUpdateTickerObservable")
//
//        return exchangeRateFactory.updateTickers()
//                .doOnComplete {
//                    view.onAccountsUpdated(
//                            displayableAccounts,
//                            getLastBtcPrice(getFiatCurrency()),
//                            getFiatCurrency(),
//                            monetaryUtil,
//                            currencyState.isDisplayingCryptoCurrency
//                    )
//                    view.onExchangeRateUpdated(
//                            exchangeRateFactory.getLastBtcPrice(getFiatCurrency()),
//                            exchangeRateFactory.getLastEthPrice(getFiatCurrency()),
//                            currencyState.isDisplayingCryptoCurrency,
//                            txNoteMap
//                    )
//                }
//    }
//
//    private fun storeSwipeReceiveAddresses() {
//        // Defer to background thread as deriving addresses is quite processor intensive
//        Completable.fromCallable {
//            swipeToReceiveHelper.updateAndStoreBitcoinAddresses()
//            swipeToReceiveHelper.updateAndStoreBitcoinCashAddresses()
//            Void.TYPE
//        }.subscribeOn(Schedulers.computation())
//                .compose(RxUtil.addCompletableToCompositeDisposable(this))
//                .subscribe(
//                        { /* No-op */ },
//                        { Timber.e(it) })
//    }
//
//    private fun subscribeToEvents() {
//
//        authEventObservable = rxBus.register(AuthEvent::class.java).apply {
//            subscribe({
//                displayList.clear()
//                transactionListDataManager.clearTransactionList()
//            })
//        }
//
//        notificationObservable = rxBus.register(NotificationPayload::class.java).apply {
//            subscribe({ notificationPayload ->
//                //no-op
//            })
//        }
//    }
//
                //    private fun getBtcBalanceString(isBtc: Boolean, btcBalance: Long): String {
                //        val strFiat = getFiatCurrency()
                //        val fiatBalance = exchangeRateFactory.getLastBtcPrice(strFiat) * (btcBalance / 1e8)
                //        var balance = monetaryUtil.getDisplayAmountWithFormatting(btcBalance)
                //        // Replace 0.0 with 0 to match web
                //        if (balance == "0.0") balance = "0"
                //
                //        return if (isBtc) {
                //            "$balance ${getBtcDisplayUnits()}"
                //        } else {
                //            "${monetaryUtil.getFiatFormat(strFiat).format(fiatBalance)} $strFiat"
                //        }
                //    }
                //
                //    private fun getEthBalanceString(isEth: Boolean, ethBalance: BigDecimal): String {
                //        val strFiat = getFiatCurrency()
                //        val fiatBalance = BigDecimal.valueOf(exchangeRateFactory.getLastEthPrice(strFiat))
                //                .multiply(Convert.fromWei(ethBalance, Convert.Unit.ETHER))
                //        val number = DecimalFormat.getInstance().apply { maximumFractionDigits = 8 }
                //                .run { format(Convert.fromWei(ethBalance, Convert.Unit.ETHER)) }
                //
                //        return if (isEth) {
                //            "$number ETH"
                //        } else {
                //            "${monetaryUtil.getFiatFormat(strFiat).format(fiatBalance.toDouble())} $strFiat"
                //        }
                //    }
//
//    private fun getLastBtcPrice(fiat: String) = exchangeRateFactory.getLastBtcPrice(fiat)
//
//    private fun getLastEthPrice(fiat: String) = exchangeRateFactory.getLastEthPrice(fiat)
//
                //    private fun getBtcDisplayUnits() = monetaryUtil.getBtcUnits()[getBtcUnitType()]
                //
                //    private fun getBtcUnitType() =
                //            prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
//
                //    private fun getFiatCurrency() =
                //            prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
//
//    private fun getShapeShiftTxNotesObservable() =
//            payloadDataManager.metadataNodeFactory
//                    .flatMap { shapeShiftDataManager.initShapeshiftTradeData(it.metadataNode) }
//                    .compose(RxUtil.addObservableToCompositeDisposable(this))
//                    .map {
//                        val map: MutableMap<String, String> = mutableMapOf()
//
//                        for (trade in it.trades) {
//                            trade.hashIn?.let {
//                                map.put(it, stringUtils.getString(R.string.shapeshift_deposit_to))
//                            }
//                            trade.hashOut?.let {
//                                map.put(it, stringUtils.getString(R.string.shapeshift_deposit_from))
//                            }
//                        }
//                        return@map map
//                    }
//                    .doOnError { Timber.e(it) }
//                    .onErrorReturn { mutableMapOf() }
//
//}