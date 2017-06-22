package piuk.blockchain.android.ui.balance

import android.app.Application
import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.contacts.data.FacilitatedTransaction
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
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
import piuk.blockchain.android.data.api.EnvironmentSettings
import piuk.blockchain.android.data.datamanagers.*
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.settings.SettingsDataManager
import piuk.blockchain.android.data.stores.TransactionListStore
import piuk.blockchain.android.injection.*
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.*
import java.util.*

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
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun onViewDestroyed() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun `onResume`() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun `onAccountChosen`() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun `onRefreshRequested`() {
        // Arrange

        // Act

        // Assert

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
        verify(prefsUtil, times(2)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastPrice("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(accessState).setIsBtc(true)
        verifyNoMoreInteractions(accessState)
        verify(view).onViewTypeChanged(true)
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
        verify(prefsUtil).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastPrice("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(accessState).isBtc
        verify(accessState).setIsBtc(false)
        verifyNoMoreInteractions(accessState)
        verify(view).onViewTypeChanged(false)
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
    fun `onPendingTransactionClicked waiting for address && initiator`() {
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
    fun `onPendingTransactionClicked waiting for payment && initiator`() {
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
    fun `onPendingTransactionClicked waiting for address && receiver, only one account`() {
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
    fun `onPendingTransactionClicked waiting for address && receiver, multiple accounts`() {
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
    fun `onPendingTransactionClicked waiting for payment && receiver`() {
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
    fun `onPendingTransactionLongClicked`() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun `onAccountChosen1`() {
        // Arrange

        // Act

        // Assert

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

        override fun provideBuyDataManager(
                onboardingDataManager: OnboardingDataManager?,
                settingsDataManager: SettingsDataManager?,
                payloadDataManager: PayloadDataManager?,
                environmentSettings: EnvironmentSettings?
        ) = buyDataManager
    }

    inner class MockApiModule : ApiModule() {
        override fun provideContactsManager(rxBus: RxBus?) = contactsDataManager
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
