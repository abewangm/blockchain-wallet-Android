package piuk.blockchain.android.ui.balance

import android.app.Application
import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.contacts.data.FacilitatedTransaction
import info.blockchain.wallet.contacts.data.PaymentRequest
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`should equal to`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.data.access.AccessState
import piuk.blockchain.android.data.access.AuthEvent
import piuk.blockchain.android.data.contacts.ContactTransactionModel
import piuk.blockchain.android.data.contacts.ContactsEvent
import piuk.blockchain.android.data.datamanagers.*
import piuk.blockchain.android.data.notifications.NotificationPayload
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.stores.PendingTransactionListStore
import piuk.blockchain.android.data.stores.TransactionListStore
import piuk.blockchain.android.injection.*
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.UiState
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.*
import java.math.BigInteger

@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class BalancePresenterTest {

    private lateinit var subject: BalancePresenter
    private var view: BalanceView = mock()
    private var exchangeRateFactory: ExchangeRateFactory = mock()
    private var transactionListDataManager: TransactionListDataManager = mock()
    private var contactsDataManager: ContactsDataManager = mock()
    private var swipeToReceiveHelper: SwipeToReceiveHelper = mock()
    private var payloadDataManager: PayloadDataManager = mock()
    private var buyDataManager: BuyDataManager = mock()
    private var stringUtils: StringUtils = mock()
    private var prefsUtil: PrefsUtil = mock()
    private var accessState: AccessState = mock()
    private var rxBus: RxBus = mock()
    private var appUtil: AppUtil = mock()

    @Before
    fun setUp() {

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                MockApplicationModule(RuntimeEnvironment.application),
                MockApiModule(),
                MockDataManagerModule())

        subject = BalancePresenter()
        subject.initView(view)
    }

    @Test
    fun onViewReady() {
        // This *could* be tested but would be absolutely enormous, and most of it's child functions
        // have been tested elsewhere in this class.
    }

    @Test
    fun onViewDestroyed() {
        // Arrange
        val contactsEventObservable = Observable.just(ContactsEvent.INIT)
        val notificationObservable = Observable.just(NotificationPayload(emptyMap()))
        val authEventObservable = Observable.just(AuthEvent.LOGOUT)
        subject.contactsEventObservable = contactsEventObservable
        subject.notificationObservable = notificationObservable
        subject.authEventObservable = authEventObservable
        // Act
        subject.onViewDestroyed()
        // Assert
        verify(rxBus).unregister(ContactsEvent::class.java, contactsEventObservable)
        verify(rxBus).unregister(NotificationPayload::class.java, notificationObservable)
        verify(rxBus).unregister(AuthEvent::class.java, authEventObservable)
    }

    @Test
    fun onResume() {
        // Arrange
        val itemAccount = ItemAccount()
        subject.chosenAccount = itemAccount
        whenever(accessState.isBtc).thenReturn(true)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(exchangeRateFactory.getLastPrice("USD")).thenReturn(2717.0)
        // Act
        subject.onResume()
        // Assert
        verify(accessState, times(2)).isBtc
        verifyNoMoreInteractions(accessState)
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, times(2)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastPrice("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(view).onViewTypeChanged(true, 0)
        verify(view).onExchangeRateUpdated(2717.0, true)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onAccountChosen success update ui with content state`() {
        // Arrange
        val itemAccount = ItemAccount()
        val transactionSummary = TransactionSummary()
        subject.activeAccountAndAddressList.add(itemAccount)
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(transactionListDataManager.getBtcBalance(itemAccount)).thenReturn(0L)
        whenever(transactionListDataManager.fetchTransactions(itemAccount, 50, 0))
                .thenReturn(Observable.just(listOf(transactionSummary)))
        whenever(accessState.isBtc).thenReturn(true)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(exchangeRateFactory.getLastPrice("USD")).thenReturn(2717.0)
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(false))
        // Act
        subject.onAccountChosen(0)
        // Assert
        verify(payloadDataManager).updateAllBalances()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).getBtcBalance(itemAccount)
        verify(transactionListDataManager).fetchTransactions(itemAccount, 50, 0)
        verifyNoMoreInteractions(transactionListDataManager)
        verify(accessState).isBtc
        verifyNoMoreInteractions(accessState)
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, times(2)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verify(prefsUtil).setValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_DISMISSED, true)
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastPrice("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
        verify(view).onTotalBalanceUpdated("0.0 BTC")
        verify(view).setUiState(UiState.CONTENT)
        verify(view).onTransactionsUpdated(listOf(transactionSummary))
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onAccountChosen success empty account update ui with empty state`() {
        // Arrange
        val itemAccount = ItemAccount()
        subject.activeAccountAndAddressList.add(itemAccount)
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(transactionListDataManager.getBtcBalance(itemAccount)).thenReturn(0L)
        whenever(transactionListDataManager.fetchTransactions(itemAccount, 50, 0))
                .thenReturn(Observable.just(emptyList()))
        whenever(accessState.isBtc).thenReturn(true)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(exchangeRateFactory.getLastPrice("USD")).thenReturn(2717.0)
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(false))
        // Act
        subject.onAccountChosen(0)
        // Assert
        verify(payloadDataManager).updateAllBalances()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).getBtcBalance(itemAccount)
        verify(transactionListDataManager).fetchTransactions(itemAccount, 50, 0)
        verifyNoMoreInteractions(transactionListDataManager)
        verify(accessState).isBtc
        verifyNoMoreInteractions(accessState)
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, times(2)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verify(prefsUtil).setValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_DISMISSED, true)
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastPrice("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
        verify(view).onTotalBalanceUpdated("0.0 BTC")
        verify(view).setUiState(UiState.EMPTY)
        verify(view).onTransactionsUpdated(emptyList())
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onAccountChosen failure`() {
        // Arrange
        val itemAccount = ItemAccount()
        subject.activeAccountAndAddressList.add(itemAccount)
        whenever(payloadDataManager.updateAllBalances())
                .thenReturn(Completable.error { Throwable() })
        whenever(transactionListDataManager.fetchTransactions(itemAccount, 50, 0))
                .thenReturn(Observable.just(emptyList()))
        // Act
        subject.onAccountChosen(0)
        // Assert
        verify(payloadDataManager).updateAllBalances()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).fetchTransactions(itemAccount, 50, 0)
        verifyNoMoreInteractions(transactionListDataManager)
        verify(view).setUiState(UiState.FAILURE)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onRefreshRequested failure`() {
        val itemAccount = ItemAccount()
        val transactionSummary = TransactionSummary()
        subject.chosenAccount = itemAccount
        whenever(payloadDataManager.updateAllBalances())
                .thenReturn(Completable.error { Throwable() })
        whenever(transactionListDataManager.fetchTransactions(itemAccount, 50, 0))
                .thenReturn(Observable.just(listOf(transactionSummary)))
        whenever(view.getIfContactsEnabled()).thenReturn(false)
        // Act
        subject.onRefreshRequested()
        // Assert
        verify(payloadDataManager).updateAllBalances()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).fetchTransactions(itemAccount, 50, 0)
        verifyNoMoreInteractions(transactionListDataManager)
        verify(view).setUiState(UiState.FAILURE)
        verify(view).getIfContactsEnabled()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onRefreshRequested contacts not enabled`() {
        val itemAccount = ItemAccount()
        val transactionSummary = TransactionSummary()
        subject.chosenAccount = itemAccount
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(transactionListDataManager.getBtcBalance(itemAccount)).thenReturn(0L)
        whenever(transactionListDataManager.fetchTransactions(itemAccount, 50, 0))
                .thenReturn(Observable.just(listOf(transactionSummary)))
        whenever(accessState.isBtc).thenReturn(true)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(exchangeRateFactory.getLastPrice("USD")).thenReturn(2717.0)
        whenever(view.getIfContactsEnabled()).thenReturn(false)
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(false))
        // Act
        subject.onRefreshRequested()
        // Assert
        verify(payloadDataManager).updateAllBalances()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).getBtcBalance(itemAccount)
        verify(transactionListDataManager).fetchTransactions(itemAccount, 50, 0)
        verifyNoMoreInteractions(transactionListDataManager)
        verify(accessState).isBtc
        verifyNoMoreInteractions(accessState)
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, times(2)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verify(prefsUtil).setValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_DISMISSED, true)
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastPrice("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
        verify(view).onTotalBalanceUpdated("0.0 BTC")
        verify(view).setUiState(UiState.CONTENT)
        verify(view).onTransactionsUpdated(listOf(transactionSummary))
        verify(view).getIfContactsEnabled()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onRefreshRequested contacts enabled`() {
        val itemAccount = ItemAccount()
        val transactionSummary = TransactionSummary()
        subject.chosenAccount = itemAccount
        val contactName = "CONTACT_NAME"
        val fctx = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
            role = FacilitatedTransaction.ROLE_RPR_RECEIVER
        }
        val transactionModel = ContactTransactionModel(contactName, fctx)
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(transactionListDataManager.getBtcBalance(itemAccount)).thenReturn(0L)
        whenever(transactionListDataManager.fetchTransactions(itemAccount, 50, 0))
                .thenReturn(Observable.just(listOf(transactionSummary)))
        whenever(accessState.isBtc).thenReturn(true)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)).thenReturn(true)
        whenever(exchangeRateFactory.getLastPrice("USD")).thenReturn(2717.0)
        whenever(view.getIfContactsEnabled()).thenReturn(true)
        whenever(contactsDataManager.fetchContacts()).thenReturn(Completable.complete())
        whenever(contactsDataManager.contactsWithUnreadPaymentRequests)
                .thenReturn(Observable.empty())
        whenever(contactsDataManager.refreshFacilitatedTransactions())
                .thenReturn(Observable.just(transactionModel))
        whenever(contactsDataManager.contactsTransactionMap).thenReturn(HashMap())
        whenever(contactsDataManager.notesTransactionMap).thenReturn(HashMap())
        whenever(stringUtils.getString(R.string.contacts_pending_transaction)).thenReturn("")
        whenever(stringUtils.getString(R.string.contacts_transaction_history)).thenReturn("")
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(false))
        // Act
        subject.onRefreshRequested()
        // Assert
        verify(payloadDataManager).updateAllBalances()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).getBtcBalance(itemAccount)
        verify(transactionListDataManager).fetchTransactions(itemAccount, 50, 0)
        verifyNoMoreInteractions(transactionListDataManager)
        verify(accessState).isBtc
        verifyNoMoreInteractions(accessState)
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, times(2)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verify(prefsUtil).setValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_DISMISSED, true)
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastPrice("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(contactsDataManager).fetchContacts()
        verify(contactsDataManager).contactsWithUnreadPaymentRequests
        verify(contactsDataManager).refreshFacilitatedTransactions()
        verify(contactsDataManager).contactsTransactionMap
        verify(contactsDataManager).notesTransactionMap
        verifyNoMoreInteractions(contactsDataManager)
        verify(stringUtils).getString(R.string.contacts_pending_transaction)
        verify(stringUtils).getString(R.string.contacts_transaction_history)
        verifyNoMoreInteractions(stringUtils)
        verify(view).onTotalBalanceUpdated("0.0 BTC")
        verify(view, times(2)).setUiState(UiState.CONTENT)
        verify(view, times(2)).onTransactionsUpdated(any())
        verify(view).getIfContactsEnabled()
        verify(view).onContactsHashMapUpdated(HashMap(), HashMap())
        verify(view).showFctxRequiringAttention(1)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun setViewType() {
        // Arrange
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(exchangeRateFactory.getLastPrice("USD")).thenReturn(0.0)
        // Act
        subject.setViewType(true)
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, times(3)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastPrice("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(accessState).setIsBtc(true)
        verifyNoMoreInteractions(accessState)
        verify(view).onViewTypeChanged(true, 0)
        verify(view).onTotalBalanceUpdated("0.0 BTC")
    }

    @Test
    fun invertViewType() {
        // Arrange
        whenever(accessState.isBtc).thenReturn(true)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(exchangeRateFactory.getLastPrice("USD")).thenReturn(0.0)
        // Act
        subject.invertViewType()
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, times(2)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastPrice("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(accessState).isBtc
        verify(accessState).setIsBtc(false)
        verifyNoMoreInteractions(accessState)
        verify(view).onViewTypeChanged(false, 0)
        verify(view).onTotalBalanceUpdated("0.00 USD")
    }

    @Test
    fun areLauncherShortcutsEnabled() {
        // Arrange
        whenever(prefsUtil.getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true))
                .thenReturn(false)
        // Act
        val result = subject.areLauncherShortcutsEnabled()
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true)
        verifyNoMoreInteractions(prefsUtil)
        result `should equal to` false
    }

    @Test
    fun `onPendingTransactionClicked contact not found`() {
        // Arrange
        val fctxId = "FCTX_ID"
        whenever(contactsDataManager.getContactFromFctxId(fctxId))
                .thenReturn(Single.error { Throwable() })
        // Act
        subject.onPendingTransactionClicked(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showToast(R.string.contacts_not_found_error, ToastCustom.TYPE_ERROR)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onPendingTransactionClicked transaction not found`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val contact = Contact()
        whenever(contactsDataManager.getContactFromFctxId(fctxId)).thenReturn(Single.just(contact))
        // Act
        subject.onPendingTransactionClicked(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showToast(R.string.contacts_transaction_not_found_error, ToastCustom.TYPE_ERROR)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onPendingTransactionClicked waiting for address & initiator`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val facilitatedTransactions = HashMap<String, FacilitatedTransaction>()
        val fctx = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
            role = FacilitatedTransaction.ROLE_RPR_INITIATOR
        }
        facilitatedTransactions.put(fctxId, fctx)
        val contact = Contact().apply { this.facilitatedTransactions = facilitatedTransactions }
        whenever(contactsDataManager.getContactFromFctxId(fctxId)).thenReturn(Single.just(contact))
        // Act
        subject.onPendingTransactionClicked(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showWaitingForAddressDialog()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onPendingTransactionClicked waiting for payment & initiator`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val facilitatedTransactions = HashMap<String, FacilitatedTransaction>()
        val fctx = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
            role = FacilitatedTransaction.ROLE_PR_INITIATOR
        }
        facilitatedTransactions.put(fctxId, fctx)
        val contact = Contact().apply { this.facilitatedTransactions = facilitatedTransactions }
        whenever(contactsDataManager.getContactFromFctxId(fctxId)).thenReturn(Single.just(contact))
        // Act
        subject.onPendingTransactionClicked(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showWaitingForPaymentDialog()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onPendingTransactionClicked waiting for address & receiver, only one account`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val facilitatedTransactions = HashMap<String, FacilitatedTransaction>()
        val fctx = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
            role = FacilitatedTransaction.ROLE_PR_RECEIVER
        }
        facilitatedTransactions.put(fctxId, fctx)
        val contact = Contact().apply { this.facilitatedTransactions = facilitatedTransactions }
        whenever(contactsDataManager.getContactFromFctxId(fctxId)).thenReturn(Single.just(contact))
        val account = Account().apply { label = "" }
        whenever(payloadDataManager.accounts).thenReturn(listOf(account))
        // Act
        subject.onPendingTransactionClicked(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showSendAddressDialog(fctxId)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onPendingTransactionClicked waiting for address & receiver, multiple accounts`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val facilitatedTransactions = HashMap<String, FacilitatedTransaction>()
        val fctx = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
            role = FacilitatedTransaction.ROLE_PR_RECEIVER
        }
        facilitatedTransactions.put(fctxId, fctx)
        val contact = Contact().apply { this.facilitatedTransactions = facilitatedTransactions }
        whenever(contactsDataManager.getContactFromFctxId(fctxId)).thenReturn(Single.just(contact))
        val account = Account().apply { label = "" }
        whenever(payloadDataManager.accounts).thenReturn(listOf(account, account))
        // Act
        subject.onPendingTransactionClicked(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showAccountChoiceDialog(listOf("", ""), fctxId)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onPendingTransactionClicked waiting for payment & receiver`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val facilitatedTransactions = HashMap<String, FacilitatedTransaction>()
        val txId = "TX_ID"
        val bitcoinUri = "BITCOIN_URI"
        val fctx: FacilitatedTransaction = mock()
        whenever(fctx.state).thenReturn(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)
        whenever(fctx.role).thenReturn(FacilitatedTransaction.ROLE_RPR_RECEIVER)
        whenever(fctx.id).thenReturn(txId)
        whenever(fctx.toBitcoinURI()).thenReturn(bitcoinUri)
        facilitatedTransactions.put(fctxId, fctx)
        val mdid = "MDID"
        val id = "ID"
        val contact = Contact().apply {
            this.facilitatedTransactions = facilitatedTransactions
            this.mdid = mdid
            this.id = id
        }
        whenever(contactsDataManager.getContactFromFctxId(fctxId)).thenReturn(Single.just(contact))
        // Act
        subject.onPendingTransactionClicked(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).initiatePayment(bitcoinUri, id, mdid, txId)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onPendingTransactionLongClicked waiting for address & receiver`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val fctx = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
            role = FacilitatedTransaction.ROLE_PR_RECEIVER
            id = fctxId
        }
        val transactionModel = ContactTransactionModel("Contact name", fctx)
        val facilitatedTransactions = mutableListOf<ContactTransactionModel>().apply {
            add(transactionModel)
        }
        whenever(contactsDataManager.facilitatedTransactions)
                .thenReturn(Observable.fromIterable(facilitatedTransactions))
        // Act
        subject.onPendingTransactionLongClicked(fctxId)
        // Assert
        verify(contactsDataManager).facilitatedTransactions
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showTransactionDeclineDialog(fctxId)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onPendingTransactionLongClicked waiting for address & initiator`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val fctx = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
            role = FacilitatedTransaction.ROLE_RPR_INITIATOR
            id = fctxId
        }
        val transactionModel = ContactTransactionModel("Contact name", fctx)
        val facilitatedTransactions = mutableListOf<ContactTransactionModel>().apply {
            add(transactionModel)
        }
        whenever(contactsDataManager.facilitatedTransactions)
                .thenReturn(Observable.fromIterable(facilitatedTransactions))
        // Act
        subject.onPendingTransactionLongClicked(fctxId)
        // Assert
        verify(contactsDataManager).facilitatedTransactions
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showTransactionCancelDialog(fctxId)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onPendingTransactionLongClicked waiting for payment & receiver`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val fctx = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
            role = FacilitatedTransaction.ROLE_RPR_RECEIVER
            id = fctxId
        }
        val transactionModel = ContactTransactionModel("Contact name", fctx)
        val facilitatedTransactions = mutableListOf<ContactTransactionModel>().apply {
            add(transactionModel)
        }
        whenever(contactsDataManager.facilitatedTransactions)
                .thenReturn(Observable.fromIterable(facilitatedTransactions))
        // Act
        subject.onPendingTransactionLongClicked(fctxId)
        // Assert
        verify(contactsDataManager).facilitatedTransactions
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showTransactionDeclineDialog(fctxId)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onPendingTransactionLongClicked waiting for payment & initiator`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val fctx = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
            role = FacilitatedTransaction.ROLE_PR_INITIATOR
            id = fctxId
        }
        val transactionModel = ContactTransactionModel("Contact name", fctx)
        val facilitatedTransactions = mutableListOf<ContactTransactionModel>().apply {
            add(transactionModel)
        }
        whenever(contactsDataManager.facilitatedTransactions)
                .thenReturn(Observable.fromIterable(facilitatedTransactions))
        // Act
        subject.onPendingTransactionLongClicked(fctxId)
        // Assert
        verify(contactsDataManager).facilitatedTransactions
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showTransactionCancelDialog(fctxId)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onPendingTransactionLongClicked transaction not found`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val fctx = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
            role = FacilitatedTransaction.ROLE_PR_INITIATOR
            id = ""
        }
        val transactionModel = ContactTransactionModel("Contact name", fctx)
        val facilitatedTransactions = listOf(transactionModel)
        whenever(contactsDataManager.facilitatedTransactions)
                .thenReturn(Observable.fromIterable(facilitatedTransactions))
        // Act
        subject.onPendingTransactionLongClicked(fctxId)
        // Assert
        verify(contactsDataManager).facilitatedTransactions
        verifyNoMoreInteractions(contactsDataManager)
        verifyZeroInteractions(view)
    }

    @Test
    fun `onAccountChosen for payment contact not found`() {
        // Arrange
        val accountPosition = 0
        val fctxId = "FCTX_ID"
        whenever(contactsDataManager.getContactFromFctxId(fctxId))
                .thenReturn(Single.error { Throwable() })
        // Act
        subject.onAccountChosen(accountPosition, fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showToast(R.string.contacts_transaction_not_found_error, ToastCustom.TYPE_ERROR)
        verify(view).showToast(R.string.contacts_address_sent_failed, ToastCustom.TYPE_ERROR)
    }

    @Test
    fun `onAccountChosen for payment successful`() {
        // Arrange
        val accountPosition = 0
        val correctedPosition = 0
        val fctxId = "FCTX_ID"
        val mdid = "MDID"
        val intendedAmount = 100L
        val address = "ADDRESS"
        val fctx = FacilitatedTransaction().apply {
            id = fctxId
            this.intendedAmount = intendedAmount
        }
        val facilitatedTransactions =
                HashMap<String, FacilitatedTransaction>().apply { put(fctxId, fctx) }
        val contact = Contact().apply {
            this.facilitatedTransactions = facilitatedTransactions
            this.mdid = mdid
        }
        whenever(contactsDataManager.getContactFromFctxId(fctxId)).thenReturn(Single.just(contact))
        whenever(payloadDataManager.getPositionOfAccountInActiveList(accountPosition))
                .thenReturn(correctedPosition)
        whenever(payloadDataManager.getNextReceiveAddressAndReserve(
                correctedPosition,
                "Payment request $fctxId"
        )).thenReturn(Observable.just(address))
        whenever(contactsDataManager.sendPaymentRequestResponse(
                eq(mdid),
                any<PaymentRequest>(),
                eq(fctxId))
        ).thenReturn(Completable.complete())
        // Act
        subject.onAccountChosen(accountPosition, fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verify(contactsDataManager).sendPaymentRequestResponse(eq(mdid), any<PaymentRequest>(), eq(fctxId))
        verifyNoMoreInteractions(contactsDataManager)
        verify(payloadDataManager).getNextReceiveAddressAndReserve(
                correctedPosition,
                "Payment request $fctxId"
        )
        verify(payloadDataManager).getPositionOfAccountInActiveList(accountPosition)
        verifyNoMoreInteractions(payloadDataManager)
        verify(view).showProgressDialog()
        verify(view).showToast(R.string.contacts_address_sent_success, ToastCustom.TYPE_OK)
        verify(view).dismissProgressDialog()
        // There'll be more interactions here as the transactions are refreshed
    }

    @Test
    fun `onAccountChosen for payment failed`() {
        // Arrange
        val accountPosition = 0
        val correctedPosition = 0
        val fctxId = "FCTX_ID"
        val mdid = "MDID"
        val intendedAmount = 100L
        val address = "ADDRESS"
        val fctx = FacilitatedTransaction().apply {
            id = fctxId
            this.intendedAmount = intendedAmount
        }
        val facilitatedTransactions =
                HashMap<String, FacilitatedTransaction>().apply { put(fctxId, fctx) }
        val contact = Contact().apply {
            this.facilitatedTransactions = facilitatedTransactions
            this.mdid = mdid
        }
        whenever(contactsDataManager.getContactFromFctxId(fctxId)).thenReturn(Single.just(contact))
        whenever(payloadDataManager.getPositionOfAccountInActiveList(accountPosition))
                .thenReturn(correctedPosition)
        whenever(payloadDataManager.getNextReceiveAddressAndReserve(
                correctedPosition,
                "Payment request $fctxId"
        )).thenReturn(Observable.just(address))
        whenever(contactsDataManager.sendPaymentRequestResponse(
                eq(mdid),
                any<PaymentRequest>(),
                eq(fctxId))
        ).thenReturn(Completable.error { Throwable() })
        // Act
        subject.onAccountChosen(accountPosition, fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verify(contactsDataManager).sendPaymentRequestResponse(eq(mdid), any<PaymentRequest>(), eq(fctxId))
        verifyNoMoreInteractions(contactsDataManager)
        verify(payloadDataManager).getNextReceiveAddressAndReserve(
                correctedPosition,
                "Payment request $fctxId"
        )
        verify(payloadDataManager).getPositionOfAccountInActiveList(accountPosition)
        verifyNoMoreInteractions(payloadDataManager)
        verify(view).showProgressDialog()
        verify(view).showToast(R.string.contacts_address_sent_failed, ToastCustom.TYPE_ERROR)
        verify(view).dismissProgressDialog()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `confirmDeclineTransaction successful`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val mdid = "MDID"
        val contact = Contact().apply { this.mdid = mdid }
        whenever(contactsDataManager.getContactFromFctxId(fctxId))
                .thenReturn(Single.just(contact))
        whenever(contactsDataManager.sendPaymentDeclinedResponse(mdid, fctxId))
                .thenReturn(Completable.complete())
        whenever(view.getIfContactsEnabled()).thenReturn(false)
        // Act
        subject.confirmDeclineTransaction(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verify(contactsDataManager).sendPaymentDeclinedResponse(mdid, fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).getIfContactsEnabled()
        verify(view).showToast(
                R.string.contacts_pending_transaction_decline_success,
                ToastCustom.TYPE_OK
        )
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `confirmDeclineTransaction failed`() {
        // Arrange
        val fctxId = "FCTX_ID"
        whenever(contactsDataManager.getContactFromFctxId(fctxId))
                .thenReturn(Single.error { Throwable() })
        whenever(contactsDataManager.fetchContacts())
                .thenReturn(Completable.complete())
        whenever(view.getIfContactsEnabled()).thenReturn(false)
        // Act
        subject.confirmDeclineTransaction(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verify(contactsDataManager).fetchContacts()
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).getIfContactsEnabled()
        verify(view).showToast(
                R.string.contacts_pending_transaction_decline_failure,
                ToastCustom.TYPE_ERROR
        )
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `confirmCancelTransaction successful`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val mdid = "MDID"
        val contact = Contact().apply { this.mdid = mdid }
        whenever(contactsDataManager.getContactFromFctxId(fctxId))
                .thenReturn(Single.just(contact))
        whenever(contactsDataManager.sendPaymentCancelledResponse(mdid, fctxId))
                .thenReturn(Completable.complete())
        whenever(view.getIfContactsEnabled()).thenReturn(false)
        // Act
        subject.confirmCancelTransaction(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verify(contactsDataManager).sendPaymentCancelledResponse(mdid, fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).getIfContactsEnabled()
        verify(view).showToast(
                R.string.contacts_pending_transaction_cancel_success,
                ToastCustom.TYPE_OK
        )
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `confirmCancelTransaction failed`() {
        // Arrange
        val fctxId = "FCTX_ID"
        whenever(contactsDataManager.getContactFromFctxId(fctxId))
                .thenReturn(Single.error { Throwable() })
        whenever(contactsDataManager.fetchContacts())
                .thenReturn(Completable.complete())
        whenever(view.getIfContactsEnabled()).thenReturn(false)
        // Act
        subject.confirmCancelTransaction(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verify(contactsDataManager).fetchContacts()
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).getIfContactsEnabled()
        verify(view).showToast(
                R.string.contacts_pending_transaction_cancel_failure,
                ToastCustom.TYPE_ERROR
        )
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `isOnboardingComplete true stored in prefs`() {
        // Arrange
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)).thenReturn(true)
        // Act
        val result = subject.isOnboardingComplete()
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)
        verifyNoMoreInteractions(prefsUtil)
        verifyZeroInteractions(appUtil)
        result `should equal to` true
    }

    @Test
    fun `isOnboardingComplete is not newly created`() {
        // Arrange
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)).thenReturn(false)
        whenever(appUtil.isNewlyCreated).thenReturn(false)
        // Act
        val result = subject.isOnboardingComplete()
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)
        verifyNoMoreInteractions(prefsUtil)
        verify(appUtil).isNewlyCreated
        verifyNoMoreInteractions(appUtil)
        result `should equal to` true
    }

    @Test
    fun setOnboardingComplete() {
        // Arrange

        // Act
        subject.setOnboardingComplete(true)
        // Assert
        verify(prefsUtil).setValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, true)
        verifyNoMoreInteractions(prefsUtil)
    }

    @Test
    fun `getBitcoinClicked canBuy returns true`() {
        // Arrange
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(true))
        // Act
        subject.getBitcoinClicked()
        // Assert
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
        verify(view).startBuyActivity()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `getBitcoinClicked canBuy returns false`() {
        // Arrange
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(false))
        // Act
        subject.getBitcoinClicked()
        // Assert
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
        verify(view).startReceiveFragment()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun disableAnnouncement() {
        // Arrange

        // Act
        subject.disableAnnouncement()
        // Assert
        verify(prefsUtil).setValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_DISMISSED, true)
        verifyNoMoreInteractions(prefsUtil)
    }

    @Test
    fun getAllDisplayableAccounts() {
        // Arrange
        val legacyAddrArchived = LegacyAddress().apply { tag = LegacyAddress.ARCHIVED_ADDRESS }
        val legacyAddr = LegacyAddress().apply { tag = LegacyAddress.NORMAL_ADDRESS }
        val legacyAddresses = listOf(legacyAddrArchived, legacyAddr)
        whenever(payloadDataManager.legacyAddresses).thenReturn(legacyAddresses)
        val xPub = "X_PUB"
        val label = "LABEL"
        val accountArchived = Account().apply { isArchived = true }
        val account1 = Account().apply {
            xpub = xPub
            this.label = label
        }
        val account2 = Account().apply {
            xpub = xPub
            this.label = label
        }
        val accounts = listOf(accountArchived, account1, account2)
        whenever(payloadDataManager.accounts).thenReturn(accounts)
        whenever(payloadDataManager.getAddressBalance(xPub)).thenReturn(BigInteger.TEN)
        whenever(payloadDataManager.walletBalance).thenReturn(BigInteger.valueOf(1_000_000L))
        whenever(payloadDataManager.importedAddressesBalance)
                .thenReturn(BigInteger.valueOf(1_000_000L))
        whenever(accessState.isBtc).thenReturn(true)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(0)
        // Act
        val result = subject.getAllDisplayableAccounts()
        // Assert
        verify(payloadDataManager).legacyAddresses
        verify(payloadDataManager).accounts
        verify(payloadDataManager, times(2)).getAddressBalance(xPub)
        verify(payloadDataManager).walletBalance
        verify(payloadDataManager).importedAddressesBalance
        verifyNoMoreInteractions(payloadDataManager)
        verify(accessState, times(4)).isBtc
        verifyNoMoreInteractions(accessState)
        verify(prefsUtil, times(4)).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, times(5)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verifyNoMoreInteractions(prefsUtil)
        // 2 accounts, "All" and "Imported"
        result.size `should equal to` 4
    }

    inner class MockDataManagerModule : DataManagerModule() {
        override fun provideTransactionListDataManager(
                payloadManager: PayloadManager?,
                transactionListStore: TransactionListStore?,
                rxBus: RxBus?
        ) = transactionListDataManager

        override fun provideSwipeToReceiveHelper(
                payloadDataManager: PayloadDataManager?,
                prefsUtil: PrefsUtil?
        ) = swipeToReceiveHelper

        override fun providePayloadDataManager(
                payloadManager: PayloadManager?,
                rxBus: RxBus?
        ) = payloadDataManager

        override fun provideBuyDataManager(onboardingDataManager: OnboardingDataManager?) =
                buyDataManager
    }

    inner class MockApiModule : ApiModule() {
        override fun provideContactsManager(
                pendingTransactionListStore: PendingTransactionListStore?,
                rxBus: RxBus?
        ) = contactsDataManager
    }

    inner class MockApplicationModule(application: Application?) : ApplicationModule(application) {
        override fun provideExchangeRateFactory() = exchangeRateFactory

        override fun provideStringUtils() = stringUtils

        override fun providePrefsUtil() = prefsUtil

        override fun provideAccessState() = accessState

        override fun provideRxBus() = rxBus

        override fun provideAppUtil() = appUtil
    }

}
