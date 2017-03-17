package piuk.blockchain.android.ui.contacts.detail

import android.app.Application
import android.os.Bundle
import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.contacts.data.FacilitatedTransaction
import info.blockchain.wallet.contacts.data.PaymentRequest
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.HDWallet
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.data.datamanagers.ContactsDataManager
import piuk.blockchain.android.data.notifications.NotificationPayload
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.equals
import piuk.blockchain.android.injection.*
import piuk.blockchain.android.ui.contacts.list.ContactsListActivity.KEY_BUNDLE_CONTACT_ID
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.PrefsUtil

@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class ContactDetailViewModelTest {

    private lateinit var subject: ContactDetailViewModel
    private val mockActivity: ContactDetailViewModel.DataListener = mock()
    private val mockContactsManager: ContactsDataManager = mock()
    private val mockPayloadManager: PayloadManager = mock()
    private val mockPrefsUtil: PrefsUtil = mock()
    private val mockRxBus: RxBus = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                MockApplicationModule(RuntimeEnvironment.application),
                MockApiModule(),
                DataManagerModule())

        subject = ContactDetailViewModel(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyShouldFinishPage() {
        // Arrange
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).pageBundle
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verify(mockActivity).finishPage()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyShouldThrowErrorAndQuitPage() {
        // Arrange
        val contactId = "CONTACT_ID"
        val bundle = Bundle()
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        bundle.putString(KEY_BUNDLE_CONTACT_ID, contactId)
        whenever(mockActivity.pageBundle).thenReturn(bundle)
        whenever(mockContactsManager.contactList)
                .thenReturn(Observable.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).pageBundle
        verify(mockActivity, times(2)).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verify(mockActivity).finishPage()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyShouldSucceed() {
        // Arrange
        val contactId = "CONTACT_ID"
        val bundle = Bundle()
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        bundle.putString(KEY_BUNDLE_CONTACT_ID, contactId)
        whenever(mockActivity.pageBundle).thenReturn(bundle)
        val contactName = "CONTACT_NAME"
        val contact0 = Contact()
        val contact1 = Contact()
        val contact2 = Contact().apply {
            id = contactId
            name = contactName
        }
        whenever(mockContactsManager.contactList)
                .thenReturn(Observable.fromIterable(listOf(contact0, contact1, contact2)))
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.complete())
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).pageBundle
        verify(mockActivity).updateContactName(contactName)
        verify(mockActivity, times(2)).onTransactionsUpdated(any(), eq(contactName))
        verifyNoMoreInteractions(mockActivity)
        verify(mockContactsManager).contactList
        verify(mockContactsManager).fetchContacts()
        verifyNoMoreInteractions(mockContactsManager)
        subject.contact equals contact2
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadySubscribeAndEmitEvent() {
        // Arrange
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        val notificationPayload: NotificationPayload = mock()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        whenever(notificationPayload.type).thenReturn(NotificationPayload.NotificationType.PAYMENT)
        // Act
        subject.onViewReady()
        notificationObservable.onNext(notificationPayload)
        // Assert
        verify(mockActivity, times(2)).pageBundle
        verify(mockActivity, times(2)).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verify(mockActivity, times(2)).finishPage()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadySubscribeAndEmitUnwantedEvent() {
        // Arrange
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        val notificationPayload: NotificationPayload = mock()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        whenever(notificationPayload.type).thenReturn(NotificationPayload.NotificationType.CONTACT_REQUEST)
        // Act
        subject.onViewReady()
        notificationObservable.onNext(notificationPayload)
        // Assert
        verify(mockActivity).pageBundle
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verify(mockActivity).finishPage()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadySubscribeAndEmitNullEvent() {
        // Arrange
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        val notificationPayload: NotificationPayload = mock()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        // Act
        subject.onViewReady()
        notificationObservable.onNext(notificationPayload)
        // Assert
        verify(mockActivity).pageBundle
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verify(mockActivity).finishPage()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadySubscribeAndEmitErrorEvent() {
        // Arrange
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        // Act
        subject.onViewReady()
        notificationObservable.onError(Throwable())
        // Assert
        verify(mockActivity).pageBundle
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verify(mockActivity).finishPage()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun getPrefsUtil() {
        // Arrange

        // Act
        val result = subject.prefsUtil
        // Assert
        result equals mockPrefsUtil
    }

    @Test
    @Throws(Exception::class)
    fun onDeleteContactClicked() {
        // Arrange

        // Act
        subject.onDeleteContactClicked()
        // Assert
        verify(mockActivity).showDeleteUserDialog()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onDeleteContactConfirmedShouldShowSuccessful() {
        // Arrange
        val contact = Contact()
        whenever(mockContactsManager.removeContact(contact)).thenReturn(Completable.complete())
        subject.contact = contact
        // Act
        subject.onDeleteContactConfirmed()
        // Assert
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_OK))
        verify(mockActivity).finishPage()
        verifyNoMoreInteractions(mockActivity)
        verify(mockContactsManager).removeContact(contact)
        verifyNoMoreInteractions(mockContactsManager)
    }

    @Test
    @Throws(Exception::class)
    fun onDeleteContactConfirmedShouldShowError() {
        // Arrange
        val contact = Contact()
        whenever(mockContactsManager.removeContact(contact))
                .thenReturn(Completable.error { Throwable() })
        subject.contact = contact
        // Act
        subject.onDeleteContactConfirmed()
        // Assert
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
        verify(mockContactsManager).removeContact(contact)
        verifyNoMoreInteractions(mockContactsManager)
    }

    @Test
    @Throws(Exception::class)
    fun onRenameContactClicked() {
        // Arrange
        val contact = Contact().apply { name = "CONTACT_NAME" }
        subject.contact = contact
        // Act
        subject.onRenameContactClicked()
        // Assert
        verify(mockActivity).showRenameDialog(contact.name)
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onContactRenamedShouldDoNothingAsNameMatches() {
        // Arrange
        val contactName = "CONTACT_NAME"
        val contact = Contact().apply { name = contactName }
        subject.contact = contact
        // Act
        subject.onContactRenamed(contactName)
        // Assert
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onContactRenamedShouldShowErrorAsNameEmpty() {
        // Arrange
        val emptyName = ""
        val contact = Contact().apply { name = "CONTACT_NAME" }
        subject.contact = contact
        // Act
        subject.onContactRenamed(emptyName)
        // Assert
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onContactRenamedShouldShowErrorAsWebCallFails() {
        // Arrange
        val newName = "CONTACT_NAME"
        val contactId = "CONTACT_ID"
        val contact = Contact().apply {
            name = ""
            id = contactId
        }
        subject.contact = contact
        whenever(mockContactsManager.renameContact(contactId, newName))
                .thenReturn(Completable.error { Throwable() })
        // Act
        subject.onContactRenamed(newName)
        // Assert
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
        verify(mockContactsManager).renameContact(contactId, newName)
        verifyNoMoreInteractions(mockContactsManager)
    }

    @Test
    @Throws(Exception::class)
    fun onContactRenamedShouldShowSuccess() {
        // Arrange
        val newName = "CONTACT_NAME"
        val contactId = "CONTACT_ID"
        val contact = Contact().apply {
            name = ""
            id = contactId
        }
        subject.contact = contact
        whenever(mockContactsManager.renameContact(contactId, newName))
                .thenReturn(Completable.complete())
        // Act
        subject.onContactRenamed(newName)
        // Assert
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).updateContactName(newName)
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_OK))
        verifyNoMoreInteractions(mockActivity)
        verify(mockContactsManager).renameContact(contactId, newName)
        verifyNoMoreInteractions(mockContactsManager)
    }

    @Test
    @Throws(Exception::class)
    fun onTransactionClickedShouldShowNotFound() {
        // Arrange
        val fctxId = "FCTX_ID"
        val contact = Contact()
        subject.contact = contact
        // Act
        subject.onTransactionClicked(fctxId)
        // Assert
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onTransactionClickedShouldShowWaitingForAddress() {
        // Arrange
        val fctxId = "FCTX_ID"
        val contact = Contact()
        subject.contact = contact
        contact.addFacilitatedTransaction(FacilitatedTransaction().apply {
            id = fctxId
            state = FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
            role = FacilitatedTransaction.ROLE_RPR_INITIATOR
        })
        // Act
        subject.onTransactionClicked(fctxId)
        // Assert
        verify(mockActivity).showWaitingForAddressDialog()
        verifyNoMoreInteractions(mockActivity)
    }

    /**
     * Only exists to get 100% coverage
     */
    @Test
    @Throws(Exception::class)
    fun onTransactionClickedShouldShowWaitingForAddressSecondBranch() {
        // Arrange
        val fctxId = "FCTX_ID"
        val contact = Contact()
        subject.contact = contact
        contact.addFacilitatedTransaction(FacilitatedTransaction().apply {
            id = fctxId
            state = FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
            role = FacilitatedTransaction.ROLE_PR_INITIATOR
        })
        // Act
        subject.onTransactionClicked(fctxId)
        // Assert
        verify(mockActivity).showWaitingForAddressDialog()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onTransactionClickedShouldShowWaitingForPayment() {
        // Arrange
        val fctxId = "FCTX_ID"
        val contact = Contact()
        subject.contact = contact
        contact.addFacilitatedTransaction(FacilitatedTransaction().apply {
            id = fctxId
            state = FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
            role = FacilitatedTransaction.ROLE_RPR_INITIATOR
        })
        // Act
        subject.onTransactionClicked(fctxId)
        // Assert
        verify(mockActivity).showWaitingForPaymentDialog()
        verifyNoMoreInteractions(mockActivity)
    }

    /**
     * Only exists to get 100% coverage
     */
    @Test
    @Throws(Exception::class)
    fun onTransactionClickedShouldShowWaitingForPaymentSecondBranch() {
        // Arrange
        val fctxId = "FCTX_ID"
        val contact = Contact()
        subject.contact = contact
        contact.addFacilitatedTransaction(FacilitatedTransaction().apply {
            id = fctxId
            state = FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
            role = FacilitatedTransaction.ROLE_PR_INITIATOR
        })
        // Act
        subject.onTransactionClicked(fctxId)
        // Assert
        verify(mockActivity).showWaitingForPaymentDialog()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onTransactionClickedShouldShowTxDetail() {
        // Arrange
        val fctxId = "FCTX_ID"
        val txHash = "TX_HASH"
        val contact = Contact()
        subject.contact = contact
        contact.addFacilitatedTransaction(FacilitatedTransaction().apply {
            id = fctxId
            state = FacilitatedTransaction.STATE_PAYMENT_BROADCASTED
            this.txHash = txHash
        })
        // Act
        subject.onTransactionClicked(fctxId)
        // Assert
        verify(mockActivity).showTransactionDetail(txHash)
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onTransactionClickedShouldShowSendAddressDialog() {
        // Arrange
        val fctxId = "FCTX_ID"
        val contact = Contact()
        subject.contact = contact
        contact.addFacilitatedTransaction(FacilitatedTransaction().apply {
            id = fctxId
            state = FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
            role = FacilitatedTransaction.ROLE_PR_RECEIVER
        })
        val mockPayload: Wallet = mock()
        whenever(mockPayloadManager.payload).thenReturn(mockPayload)
        val mockHdWallet: HDWallet = mock()
        whenever(mockPayload.hdWallets).thenReturn(listOf(mockHdWallet))
        val account0 = Account().apply { isArchived = true }
        val account1 = Account().apply { isArchived = true }
        val account2 = Account()
        whenever(mockHdWallet.accounts).thenReturn(listOf(account0, account1, account2))
        // Act
        subject.onTransactionClicked(fctxId)
        // Assert
        verify(mockActivity).showSendAddressDialog(fctxId)
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onTransactionClickedShouldShowSendAccountChoiceDialog() {
        // Arrange
        val fctxId = "FCTX_ID"
        val contact = Contact()
        subject.contact = contact
        contact.addFacilitatedTransaction(FacilitatedTransaction().apply {
            id = fctxId
            state = FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
            role = FacilitatedTransaction.ROLE_RPR_RECEIVER
        })
        val mockPayload: Wallet = mock()
        whenever(mockPayloadManager.payload).thenReturn(mockPayload)
        val mockHdWallet: HDWallet = mock()
        whenever(mockPayload.hdWallets).thenReturn(listOf(mockHdWallet))
        val accountLabel0 = "ACCOUNT_0"
        val account0 = Account().apply { label = accountLabel0 }
        val accountLabel1 = "ACCOUNT_1"
        val account1 = Account().apply { label = accountLabel1 }
        val accountLabel2 = "ACCOUNT_2"
        val account2 = Account().apply { label = accountLabel2 }
        whenever(mockHdWallet.accounts).thenReturn(listOf(account0, account1, account2))
        // Act
        subject.onTransactionClicked(fctxId)
        // Assert
        verify(mockActivity).showAccountChoiceDialog(listOf(accountLabel0, accountLabel1, accountLabel2), fctxId)
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onTransactionClickedShouldInitiatePayment() {
        // Arrange
        val fctxId = "FCTX_ID"
        val contact = Contact()
        subject.contact = contact
        val facilitatedTransaction = FacilitatedTransaction().apply {
            id = fctxId
            state = FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
            role = FacilitatedTransaction.ROLE_PR_RECEIVER
            intendedAmount = 0L
            address = ""
        }
        contact.addFacilitatedTransaction(facilitatedTransaction)
        val mockPayload: Wallet = mock()
        whenever(mockPayloadManager.payload).thenReturn(mockPayload)
        val mockHdWallet: HDWallet = mock()
        whenever(mockPayload.hdWallets).thenReturn(listOf(mockHdWallet))
        val defaultIndex = 1337
        whenever(mockHdWallet.defaultAccountIdx).thenReturn(defaultIndex)
        // Act
        subject.onTransactionClicked(fctxId)
        // Assert
        verify(mockActivity).initiatePayment(
                facilitatedTransaction.toBitcoinURI(),
                contact.id,
                contact.mdid,
                fctxId,
                defaultIndex)
        verifyNoMoreInteractions(mockActivity)
    }

    /**
     * Only exists to get 100% coverage
     */
    @Test
    @Throws(Exception::class)
    fun onTransactionClickedShouldInitiatePaymentSecondBranch() {
        // Arrange
        val fctxId = "FCTX_ID"
        val contact = Contact()
        subject.contact = contact
        val facilitatedTransaction = FacilitatedTransaction().apply {
            id = fctxId
            state = FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
            role = FacilitatedTransaction.ROLE_RPR_RECEIVER
            intendedAmount = 0L
            address = ""
        }
        contact.addFacilitatedTransaction(facilitatedTransaction)
        val mockPayload: Wallet = mock()
        whenever(mockPayloadManager.payload).thenReturn(mockPayload)
        val mockHdWallet: HDWallet = mock()
        whenever(mockPayload.hdWallets).thenReturn(listOf(mockHdWallet))
        val defaultIndex = 1337
        whenever(mockHdWallet.defaultAccountIdx).thenReturn(defaultIndex)
        // Act
        subject.onTransactionClicked(fctxId)
        // Assert
        verify(mockActivity).initiatePayment(
                facilitatedTransaction.toBitcoinURI(),
                contact.id,
                contact.mdid,
                fctxId,
                defaultIndex)
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onTransactionLongClicked() {
        // Arrange
        val fctxId = "FCTX_ID"
        // Act
        subject.onTransactionLongClicked(fctxId)
        // Assert
        verify(mockActivity).showDeleteFacilitatedTransactionDialog(fctxId)
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun confirmDeleteFacilitatedTransactionShouldShowSuccess() {
        // Arrange
        val fctxId = "FCTX_ID"
        val contact = Contact()
        whenever(mockContactsManager.getContactFromFctxId(fctxId))
                .thenReturn(Observable.just(contact))
        whenever(mockContactsManager.deleteFacilitatedTransaction(contact.mdid, fctxId))
                .thenReturn(Completable.complete())
        // Act
        subject.confirmDeleteFacilitatedTransaction(fctxId)
        // Assert
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_OK))
        verify(mockContactsManager).getContactFromFctxId(fctxId)
        verify(mockContactsManager).deleteFacilitatedTransaction(contact.mdid, fctxId)
        // More interactions as page is set up again, but we're not testing those
    }

    @Test
    @Throws(Exception::class)
    fun confirmDeleteFacilitatedTransactionShouldShowFailure() {
        // Arrange
        val fctxId = "FCTX_ID"
        val contact = Contact()
        whenever(mockContactsManager.getContactFromFctxId(fctxId))
                .thenReturn(Observable.just(contact))
        whenever(mockContactsManager.deleteFacilitatedTransaction(contact.mdid, fctxId))
                .thenReturn(Completable.error { Throwable() })
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.complete())
        // Act
        subject.confirmDeleteFacilitatedTransaction(fctxId)
        // Assert
        verify(mockActivity, times(2)).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        // More interactions as page is set up again, but we're not testing those
        verify(mockContactsManager).getContactFromFctxId(fctxId)
        verify(mockContactsManager).fetchContacts()
        verify(mockContactsManager).deleteFacilitatedTransaction(contact.mdid, fctxId)
        verifyNoMoreInteractions(mockContactsManager)
    }

    @Test
    @Throws(Exception::class)
    fun onAccountChosenShouldShowSuccess() {
        // Arrange
        val fctxId = "FCTX_ID"
        val accountPosition = 0
        val mdid = "MDID"
        val contact = Contact().apply { this.mdid = mdid }
        subject.contact = contact
        contact.addFacilitatedTransaction(FacilitatedTransaction().apply {
            id = fctxId
            intendedAmount = 21 * 1000 * 1000L
        })
        val address = "ADDRESS"
        val account = Account()
        whenever(mockPayloadManager.getNextReceiveAddress(account)).thenReturn(address)
        val mockPayload: Wallet = mock()
        whenever(mockPayloadManager.payload).thenReturn(mockPayload)
        val mockHdWallet: HDWallet = mock()
        whenever(mockPayload.hdWallets).thenReturn(listOf(mockHdWallet))
        whenever(mockHdWallet.accounts).thenReturn(listOf(account))
        whenever(mockContactsManager.sendPaymentRequestResponse(eq(mdid), any<PaymentRequest>(), eq(fctxId)))
                .thenReturn(Completable.complete())
        // Act
        subject.onAccountChosen(accountPosition, fctxId)
        // Assert
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_OK))
        // More interactions as page is set up again, but we're not testing those
        verify(mockContactsManager).sendPaymentRequestResponse(eq(mdid), any<PaymentRequest>(), eq(fctxId))
        verifyNoMoreInteractions(mockContactsManager)
    }

    @Test
    @Throws(Exception::class)
    fun onAccountChosenShouldShowFailure() {
        // Arrange
        val fctxId = "FCTX_ID"
        val accountPosition = 0
        val mdid = "MDID"
        val contact = Contact().apply { this.mdid = mdid }
        subject.contact = contact
        contact.addFacilitatedTransaction(FacilitatedTransaction().apply {
            id = fctxId
            intendedAmount = 21 * 1000 * 1000L
        })
        val address = "ADDRESS"
        val account = Account()
        whenever(mockPayloadManager.getNextReceiveAddress(account)).thenReturn(address)
        val mockPayload: Wallet = mock()
        whenever(mockPayloadManager.payload).thenReturn(mockPayload)
        val mockHdWallet: HDWallet = mock()
        whenever(mockPayload.hdWallets).thenReturn(listOf(mockHdWallet))
        whenever(mockHdWallet.accounts).thenReturn(listOf(account))
        whenever(mockContactsManager.sendPaymentRequestResponse(eq(mdid), any<PaymentRequest>(), eq(fctxId)))
                .thenReturn(Completable.error { Throwable() })
        // Act
        subject.onAccountChosen(accountPosition, fctxId)
        // Assert
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        // More interactions as page is set up again, but we're not testing those
        verify(mockContactsManager).sendPaymentRequestResponse(eq(mdid), any<PaymentRequest>(), eq(fctxId))
        verifyNoMoreInteractions(mockContactsManager)
    }

    @Test
    @Throws(Exception::class)
    fun destroy() {
        // Arrange

        // Act
        subject.destroy()
        // Assert
        verify(mockRxBus).unregister(eq(NotificationPayload::class.java), anyOrNull())
    }

    inner class MockApplicationModule(application: Application?) : ApplicationModule(application) {
        override fun providePrefsUtil(): PrefsUtil {
            return mockPrefsUtil
        }

        override fun provideRxBus(): RxBus {
            return mockRxBus
        }
    }

    inner class MockApiModule : ApiModule() {
        override fun providePayloadManager(): PayloadManager {
            return mockPayloadManager
        }

        override fun provideContactsManager(payloadManager: PayloadManager?): ContactsDataManager {
            return mockContactsManager
        }
    }

}
