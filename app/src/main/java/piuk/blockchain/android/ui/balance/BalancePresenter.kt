package piuk.blockchain.android.ui.balance

import android.annotation.SuppressLint
import android.support.annotation.VisibleForTesting
import info.blockchain.wallet.ethereum.data.EthAddressResponse
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
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.base.UiState
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.math.BigDecimal
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
        private val bchDataManager: BchDataManager,
        private val walletAccountHelper: WalletAccountHelper
) : BasePresenter<BalanceView>() {

    @VisibleForTesting var notificationObservable: Observable<NotificationPayload>? = null
    @VisibleForTesting var authEventObservable: Observable<AuthEvent>? = null

    private val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(getBtcUnitType()) }
    private var shortcutsGenerated = false

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
            subscribe({
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
                .doOnError { view.setUiState(UiState.FAILURE) }
                .doOnSubscribe { view.setUiState(UiState.LOADING) }
                .doOnComplete {
                    refreshBalanceHeader(account)
                    refreshAccountDataSet()
                    if (!shortcutsGenerated) {
                        shortcutsGenerated = true
                        refreshLauncherShortcuts()
                    }
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

    @VisibleForTesting
    internal fun getUpdateTickerCompletable(): Completable {
        return Completable.fromObservable(exchangeRateFactory.updateTickers())
    }

    /**
     * API call - Update eth address
     */

    @VisibleForTesting
    internal fun updateEthAddress() =
        Completable.fromObservable(ethDataManager.fetchEthAddress()
                .onExceptionResumeNext { Observable.empty<EthAddressResponse>() })

    /**
     * API call - Update bitcoincash wallet
     */

    @VisibleForTesting
    internal fun updateBchWallet() = bchDataManager.refreshMetadataCompletable()
            .doOnError{ Timber.e(it) }

    /**
     * API call - Fetches latest balance for selected currency and updates UI balance
     */
    @VisibleForTesting
    internal fun updateBalancesCompletable() =
            when (currencyState.cryptoCurrency) {
                CryptoCurrencies.BTC -> payloadDataManager.updateAllBalances()
                CryptoCurrencies.ETHER -> ethDataManager.fetchEthAddressCompletable()
                CryptoCurrencies.BCH -> bchDataManager.updateAllBalances()
                else -> throw IllegalArgumentException("${currencyState.cryptoCurrency.unit} is not currently supported")
            }

    /**
     * API call - Fetches latest transactions for selected currency and account, and updates UI tx list
     */
    internal fun updateTransactionsListCompletable(account: ItemAccount): Completable {
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
                                                        else -> throw IllegalArgumentException("${currencyState.cryptoCurrency.unit} is not currently supported")
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
                    if (it && view.shouldShowBuy()) {
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

        val account = getAccounts()[position]

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
    internal fun refreshBalanceHeader(account: ItemAccount) {
        view.updateSelectedCurrency(currencyState.cryptoCurrency)
        view.updateBalanceHeader(account.displayBalance ?: "")
    }

    internal fun refreshAccountDataSet() {
        val accountList = getAccounts()
        view.updateAccountsDataSet(accountList)
    }

    private fun refreshLauncherShortcuts() {
        view.generateLauncherShortcuts()
    }
    //endregion

    //region Adapter data

    fun onTxFeedAdapterSetup() {
        view.setupTxFeedAdapter(currencyState.isDisplayingCryptoCurrency)
    }

    /**
     * Get accounts based on selected currency
     */
    private fun getAccounts() = walletAccountHelper.getAccountItemsForOverview().toMutableList()

    private fun getCurrenctAccount(): ItemAccount {
        return getAccountAt(view.getCurrentAccountPosition() ?: 0)
    }

    /*
    Don't over use this method. It's a bit hacky, but fast enough to work.
     */
    private fun getAccountAt(position: Int): ItemAccount {
        return getAccounts()[if (position < 0) 0 else position]
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