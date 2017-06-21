package piuk.blockchain.android.ui.balance

import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.contacts.data.FacilitatedTransaction
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.data.access.AccessState
import piuk.blockchain.android.data.access.AuthEvent
import piuk.blockchain.android.data.contacts.ContactTransactionDateComparator
import piuk.blockchain.android.data.contacts.ContactTransactionModel
import piuk.blockchain.android.data.contacts.ContactsEvent
import piuk.blockchain.android.data.datamanagers.ContactsDataManager
import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.notifications.NotificationPayload
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.account.ConsolidatedAccount
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.base.UiState
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import javax.inject.Inject

class BalancePresenter : BasePresenter<BalanceView>() {

    @Inject lateinit var exchangeRateFactory: ExchangeRateFactory
    @Inject lateinit var transactionListDataManager: TransactionListDataManager
    @Inject lateinit var contactsDataManager: ContactsDataManager
    @Inject lateinit var payloadDataManager: PayloadDataManager
    @Inject lateinit var stringUtils: StringUtils
    @Inject lateinit var prefsUtil: PrefsUtil
    @Inject lateinit var accessState: AccessState
    @Inject lateinit var rxBus: RxBus
    @Inject lateinit var swipeToReceiveHelper: SwipeToReceiveHelper

    private var contactsEventObservable: Observable<ContactsEvent>? = null
    private var notificationObservable: Observable<NotificationPayload>? = null
    private var authEventObservable: Observable<AuthEvent>? = null

    private var activeAccountAndAddressList: MutableList<ItemAccount> = mutableListOf()
    private var displayList: MutableList<Any> = mutableListOf()
    private var chosenAccount: ItemAccount? = null

    init {
        Injector.getInstance().dataManagerComponent.inject(this)
    }

    override fun onViewReady() {
        view.setUiState(UiState.LOADING)

        subscribeToEvents()
        storeSwipeReceiveAddresses()

        activeAccountAndAddressList = getAllDisplayableAccounts()
        chosenAccount = activeAccountAndAddressList[0]

        chosenAccount?.let { chosenAccount ->
            Observable.merge(
                    getBalanceObservable(chosenAccount),
                    getTransactionsListObservable(chosenAccount),
                    getUpdateTickerObservable()
            ).compose(RxUtil.addObservableToCompositeDisposable(this))
                    .subscribe(
                            { /* No-op */ },
                            { view.setUiState(UiState.FAILURE) })
        }
    }

    fun onAccountChosen(position: Int) {
        chosenAccount = activeAccountAndAddressList[position]
        chosenAccount?.let { chosenAccount ->
            Observable.merge(
                    getBalanceObservable(chosenAccount),
                    getTransactionsListObservable(chosenAccount)
            ).compose(RxUtil.addObservableToCompositeDisposable(this))
                    .subscribe(
                            { /* No-op */ },
                            { view.setUiState(UiState.FAILURE) })
        }
    }

    fun onRefreshRequested() {
        chosenAccount?.let { chosenAccount ->
            Observable.merge(
                    getBalanceObservable(chosenAccount),
                    getTransactionsListObservable(chosenAccount),
                    getFacilitatedTransactionsObservable()
            ).compose(RxUtil.addObservableToCompositeDisposable(this))
                    .subscribe(
                            { /* No-op */ },
                            { view.setUiState(UiState.FAILURE) })
        }
    }

    fun setViewType(isBtc: Boolean) {
        accessState.setIsBtc(isBtc)
        view.onViewTypeChanged(isBtc)
        view.onTotalBalanceUpdated(getBalanceString(isBtc, chosenAccount?.absoluteBalance!!))
    }

    fun invertViewType() {
        setViewType(!accessState.isBtc)
    }

    fun onResume() {
        // Here we check the Fiat and Btc formats and let the UI handle any potential updates
        val btcBalance = transactionListDataManager.getBtcBalance(chosenAccount?.accountObject)
        val balanceTotal = getBalanceString(accessState.isBtc, btcBalance)
        view.onTotalBalanceUpdated(balanceTotal)
        view.onViewTypeChanged(accessState.isBtc)
    }

    fun areLauncherShortcutsEnabled(): Boolean {
        return prefsUtil.getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true)
    }

    private fun getAllDisplayableAccounts(): MutableList<ItemAccount> {
        val mutableList = mutableListOf<ItemAccount>()

        val legacyAddresses = payloadDataManager.legacyAddresses
                .filter { it.tag != LegacyAddress.ARCHIVED_ADDRESS }

        val accounts = payloadDataManager.accounts
                .filter { !it.isArchived }
                .map { it ->
                    val bigIntBalance = payloadDataManager.getAddressBalance(it.xpub)
                    ItemAccount().apply {
                        label = it.label
                        displayBalance = getBalanceString(accessState.isBtc, bigIntBalance.toLong())
                        absoluteBalance = bigIntBalance.toLong()
                        accountObject = it
                    }
                }

        // Show "All Accounts" if necessary
        if (accounts.size > 1 || !legacyAddresses.isEmpty()) {
            val all = ConsolidatedAccount().apply {
                label = stringUtils.getString(R.string.all_accounts)
                type = ConsolidatedAccount.Type.ALL_ACCOUNTS
            }

            val bigIntBalance = payloadDataManager.walletBalance

            mutableList.add(ItemAccount().apply {
                label = all.label
                displayBalance = getBalanceString(accessState.isBtc, bigIntBalance.toLong())
                absoluteBalance = bigIntBalance.toLong()
                accountObject = all
            })
        }

        mutableList.addAll(accounts)

        // Show "Imported Addresses" if necessary
        if (!legacyAddresses.isEmpty()) {
            val importedAddresses = ConsolidatedAccount().apply {
                label = stringUtils.getString(R.string.imported_addresses)
                type = ConsolidatedAccount.Type.ALL_IMPORTED_ADDRESSES
            }

            val bigIntBalance = payloadDataManager.importedAddressesBalance

            mutableList.add(ItemAccount().apply {
                label = importedAddresses.label
                displayBalance = getBalanceString(accessState.isBtc, bigIntBalance.toLong())
                absoluteBalance = bigIntBalance.toLong()
                accountObject = importedAddresses
            })
        }

        return mutableList
    }

    private fun getTransactionsListObservable(itemAccount: ItemAccount) =
            transactionListDataManager.fetchTransactions(itemAccount.accountObject, 50, 0)
                    .doAfterTerminate(this::storeSwipeReceiveAddresses)
                    .doOnNext { list ->
                        displayList.removeAll { it is TransactionSummary }
                        displayList.addAll(list)

                        when {
                            list.isEmpty() -> view.setUiState(UiState.EMPTY)
                            else -> view.setUiState(UiState.CONTENT)
                        }
                        view.onTransactionsUpdated(displayList)
                    }

    private fun getBalanceObservable(itemAccount: ItemAccount) =
            payloadDataManager.updateAllBalances()
                    .doOnComplete {
                        val btcBalance = transactionListDataManager.getBtcBalance(itemAccount.accountObject)
                        val balanceTotal = getBalanceString(accessState.isBtc, btcBalance)
                        view.onTotalBalanceUpdated(balanceTotal)
                    }.toObservable<Nothing>()

    private fun getUpdateTickerObservable() =
            exchangeRateFactory.updateTicker()
                    .doOnComplete {
                        view.onAccountsUpdated(
                                activeAccountAndAddressList,
                                getLastPrice(getFiatCurrency()),
                                getFiatCurrency(),
                                monetaryUtil,
                                accessState.isBtc
                        )
                        view.onExchangeRateUpdated(
                                exchangeRateFactory.getLastPrice(getFiatCurrency()),
                                accessState.isBtc
                        )
                    }.toObservable<Nothing>()

    private fun getFacilitatedTransactionsObservable(): Observable<MutableList<ContactTransactionModel>> {
        if (view.getIfContactsEnabled()) {
            return contactsDataManager.fetchContacts()
                    .andThen<Contact>(contactsDataManager.contactsWithUnreadPaymentRequests)
                    .toList()
                    .flatMapObservable { contactsDataManager.refreshFacilitatedTransactions() }
                    .toList()
                    .onErrorReturnItem(emptyList())
                    .toObservable()
                    .doOnNext {
                        handlePendingTransactions(it)
                        view.onContactsHashMapUpdated(
                                contactsDataManager.contactsTransactionMap,
                                contactsDataManager.notesTransactionMap
                        )
                    }
        } else {
            return Observable.empty<MutableList<ContactTransactionModel>>()
        }
    }

    private fun refreshFacilitatedTransactions() {
        getFacilitatedTransactionsObservable()
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(
                        { /* No-op */ },
                        { throwable -> throwable.printStackTrace() })
    }

    private fun storeSwipeReceiveAddresses() {
        // Defer to background thread as deriving addresses is quite processor intensive
        Completable.fromCallable {
            swipeToReceiveHelper.updateAndStoreAddresses()
            Void.TYPE
        }.subscribeOn(Schedulers.computation())
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .subscribe(
                        { /* No-op */ },
                        { it.printStackTrace() })
    }

    private fun subscribeToEvents() {
        contactsEventObservable = rxBus.register(ContactsEvent::class.java)
        contactsEventObservable?.subscribe({ _: ContactsEvent -> refreshFacilitatedTransactions() })

        authEventObservable = rxBus.register(AuthEvent::class.java)
        authEventObservable?.subscribe({ _: AuthEvent ->
            displayList.clear()
            transactionListDataManager.clearTransactionList()
            contactsDataManager.resetContacts()
        })

        notificationObservable = rxBus.register(NotificationPayload::class.java)
        notificationObservable?.subscribe({ notificationPayload ->
            if (notificationPayload.type != null
                    && notificationPayload.type == NotificationPayload.NotificationType.PAYMENT) {
                refreshFacilitatedTransactions()
            }
        })
    }

    private fun handlePendingTransactions(transactions: List<ContactTransactionModel>) {
        displayList.removeAll { it !is TransactionSummary }
        view.showFctxRequiringAttention(getNumberOfFctxRequiringAttention(transactions))
        if (!transactions.isEmpty()) {
            val reversed = transactions.sortedWith(ContactTransactionDateComparator()).reversed()
            displayList.add(0, stringUtils.getString(R.string.contacts_pending_transaction))
            displayList.addAll(1, reversed)
            displayList.add(reversed.size + 1, stringUtils.getString(R.string.contacts_transaction_history))
            view.onTransactionsUpdated(displayList)
        } else {
            view.onTransactionsUpdated(displayList)
        }
    }

    private fun getNumberOfFctxRequiringAttention(facilitatedTransactions: List<ContactTransactionModel>): Int {
        var value = 0
        facilitatedTransactions
                .asSequence()
                .map { it.facilitatedTransaction }
                .forEach {
                    if (it.state == FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
                            && it.role == FacilitatedTransaction.ROLE_RPR_RECEIVER) {
                        value++
                    } else if (it.state == FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
                            && it.role == FacilitatedTransaction.ROLE_RPR_RECEIVER) {
                        value++
                    }
                }
        return value
    }

    private fun getBalanceString(isBTC: Boolean, btcBalance: Long): String {
        val strFiat = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        val fiatBalance = exchangeRateFactory.getLastPrice(strFiat) * (btcBalance / 1e8)

        return if (isBTC) {
            monetaryUtil.getDisplayAmountWithFormatting(btcBalance) + " " + getDisplayUnits()
        } else {
            monetaryUtil.getFiatFormat(strFiat).format(fiatBalance) + " " + strFiat
        }
    }

    private fun getLastPrice(fiat: String): Double = exchangeRateFactory.getLastPrice(fiat)

    private fun getDisplayUnits(): String =
            monetaryUtil.btcUnits[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)].toString()

    private fun getFiatCurrency(): String =
            prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

    private val monetaryUtil: MonetaryUtil by lazy(LazyThreadSafetyMode.NONE) {
        MonetaryUtil(prefsUtil.getValue(
                PrefsUtil.KEY_BTC_UNITS,
                MonetaryUtil.UNIT_BTC
        ))
    }

}
