package piuk.blockchain.android.ui.contacts.payments

import android.os.Bundle
import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.contacts.data.PaymentRequest
import info.blockchain.wallet.contacts.data.RequestForPaymentRequest
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.Completable
import io.reactivex.Observable
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.data.contacts.PaymentRequestType
import piuk.blockchain.android.data.datamanagers.ContactsDataManager
import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.injection.*
import piuk.blockchain.android.ui.contacts.payments.ContactPaymentRequestNotesFragment.*
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.send.SendFragment.ARGUMENT_CONTACT_ID
import kotlin.test.assertNull

@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class ContactsPaymentRequestViewModelTest {

    private lateinit var subject: ContactsPaymentRequestViewModel
    private val mockActivity: ContactsPaymentRequestViewModel.DataListener = mock()
    private val mockContactsManager: ContactsDataManager = mock()
    private val mockPayloadDataManager: PayloadDataManager = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                ApplicationModule(RuntimeEnvironment.application),
                MockApiModule(),
                MockDataManagerModule())

        subject = ContactsPaymentRequestViewModel(mockActivity)
    }

    @Test(expected = AssertionError::class)
    @Throws(Exception::class)
    fun onViewReadyNullBundle() {
        // Arrange
        whenever(mockActivity.fragmentBundle).thenReturn(Bundle())
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).fragmentBundle
        verifyNoMoreInteractions(mockActivity)
    }

    @Test(expected = AssertionError::class)
    @Throws(Exception::class)
    fun onViewReadyNullValues() {
        // Arrange
        val contactId = "CONTACT_ID"
        val satoshis = 21000000000L
        val bundle = Bundle().apply {
            putString(ARGUMENT_CONTACT_ID, contactId)
            putLong(ARGUMENT_SATOSHIS, satoshis)
            putSerializable(ARGUMENT_REQUEST_TYPE, null)
        }
        whenever(mockActivity.fragmentBundle).thenReturn(bundle)
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).fragmentBundle
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyLoadContactsSuccess() {
        // Arrange
        val contactId = "CONTACT_ID"
        val contactName = "CONTACT_NAME"
        val satoshis = 21000000000L
        val accountPosition = 3
        val paymentRequestType = PaymentRequestType.REQUEST
        val bundle = Bundle().apply {
            putString(ARGUMENT_CONTACT_ID, contactId)
            putLong(ARGUMENT_SATOSHIS, satoshis)
            putInt(ARGUMENT_ACCOUNT_POSITION, accountPosition)
            putSerializable(ARGUMENT_REQUEST_TYPE, paymentRequestType)
        }
        val contact0 = Contact()
        val contact1 = Contact().apply {
            id = contactId
            name = contactName
        }
        val contact2 = Contact()
        val contactList = listOf(contact0, contact1, contact2)
        whenever(mockActivity.fragmentBundle).thenReturn(bundle)
        whenever(mockContactsManager.contactList).thenReturn(Observable.fromIterable(contactList))
        // Act
        subject.onViewReady()
        // Assert
        verify(mockContactsManager).contactList
        verifyNoMoreInteractions(mockContactsManager)
        verify(mockActivity).fragmentBundle
        verify(mockActivity).contactLoaded(contactName, paymentRequestType)
        verifyNoMoreInteractions(mockActivity)
        subject.recipient shouldEqual contact1
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyLoadContactsFailure() {
        // Arrange
        val contactId = "CONTACT_ID"
        val satoshis = 21000000000L
        val accountPosition = 3
        val paymentRequestType = PaymentRequestType.REQUEST
        val bundle = Bundle().apply {
            putString(ARGUMENT_CONTACT_ID, contactId)
            putLong(ARGUMENT_SATOSHIS, satoshis)
            putInt(ARGUMENT_ACCOUNT_POSITION, accountPosition)
            putSerializable(ARGUMENT_REQUEST_TYPE, paymentRequestType)
        }
        whenever(mockActivity.fragmentBundle).thenReturn(bundle)
        whenever(mockContactsManager.contactList).thenReturn(Observable.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(mockContactsManager).contactList
        verifyNoMoreInteractions(mockContactsManager)
        verify(mockActivity).fragmentBundle
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verify(mockActivity).finishPage()
        verifyNoMoreInteractions(mockActivity)
        assertNull(subject.recipient)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyLoadContactsNotFound() {
        // Arrange
        val contactId = "CONTACT_ID"
        val satoshis = 21000000000L
        val accountPosition = 3
        val paymentRequestType = PaymentRequestType.REQUEST
        val bundle = Bundle().apply {
            putString(ARGUMENT_CONTACT_ID, contactId)
            putLong(ARGUMENT_SATOSHIS, satoshis)
            putInt(ARGUMENT_ACCOUNT_POSITION, accountPosition)
            putSerializable(ARGUMENT_REQUEST_TYPE, paymentRequestType)
        }
        val contact0 = Contact()
        val contact1 = Contact()
        val contact2 = Contact()
        val contactList = listOf(contact0, contact1, contact2)
        whenever(mockActivity.fragmentBundle).thenReturn(bundle)
        whenever(mockContactsManager.contactList).thenReturn(Observable.fromIterable(contactList))
        // Act
        subject.onViewReady()
        // Assert
        verify(mockContactsManager).contactList
        verifyNoMoreInteractions(mockContactsManager)
        verify(mockActivity).fragmentBundle
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verify(mockActivity).finishPage()
        verifyNoMoreInteractions(mockActivity)
        assertNull(subject.recipient)
    }

    @Test
    @Throws(Exception::class)
    fun sendRequestAmountInvalid() {
        // Arrange
        subject.satoshis = 0L
        // Act
        subject.sendRequest()
        // Assert
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun sendRequestSuccessTypeRequest() {
        // Arrange
        val contactName = "CONTACT_NAME"
        val contactMdid = "CONTACT_MDID"
        val note = "NOTE"
        val satoshis = 21000000000L
        val accountPosition = 3
        val paymentRequestType = PaymentRequestType.REQUEST
        val receiveAddress = "RECEIVE_ADDRESS"
        val recipient = Contact().apply {
            name = contactName
            mdid = contactMdid
        }
        subject.apply {
            this.recipient = recipient
            this.satoshis = satoshis
            this.accountPosition = accountPosition
            this.paymentRequestType = paymentRequestType
        }
        whenever(mockPayloadDataManager.getNextReceiveAddress(accountPosition))
                .thenReturn(Observable.just(receiveAddress))
        whenever(mockContactsManager.requestSendPayment(eq(contactMdid), any()))
                .thenReturn(Completable.complete())
        whenever(mockActivity.note).thenReturn(note)
        // Act
        subject.sendRequest()
        // Assert
        verify(mockPayloadDataManager).getNextReceiveAddress(accountPosition)
        verify(mockContactsManager).requestSendPayment(eq(contactMdid), any<PaymentRequest>())
        verifyNoMoreInteractions(mockContactsManager)
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).note
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).showSendSuccessfulDialog(contactName)
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun sendRequestSuccessTypeSend() {
        // Arrange
        val contactName = "CONTACT_NAME"
        val contactMdid = "CONTACT_MDID"
        val note = "NOTE"
        val satoshis = 21000000000L
        val accountPosition = 3
        val paymentRequestType = PaymentRequestType.SEND
        val recipient = Contact().apply {
            name = contactName
            mdid = contactMdid
        }
        subject.apply {
            this.recipient = recipient
            this.satoshis = satoshis
            this.accountPosition = accountPosition
            this.paymentRequestType = paymentRequestType
        }
        whenever(mockContactsManager.requestReceivePayment(eq(contactMdid), any()))
                .thenReturn(Completable.complete())
        whenever(mockActivity.note).thenReturn(note)
        // Act
        subject.sendRequest()
        // Assert
        verifyZeroInteractions(mockPayloadDataManager)
        verify(mockContactsManager).requestReceivePayment(eq(contactMdid), any<RequestForPaymentRequest>())
        verifyNoMoreInteractions(mockContactsManager)
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).note
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).showRequestSuccessfulDialog()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun sendRequestFailureTypeRequest() {
        // Arrange
        val contactName = "CONTACT_NAME"
        val contactMdid = "CONTACT_MDID"
        val note = "NOTE"
        val satoshis = 21000000000L
        val accountPosition = 3
        val paymentRequestType = PaymentRequestType.REQUEST
        val receiveAddress = "RECEIVE_ADDRESS"
        val recipient = Contact().apply {
            name = contactName
            mdid = contactMdid
        }
        subject.apply {
            this.recipient = recipient
            this.satoshis = satoshis
            this.accountPosition = accountPosition
            this.paymentRequestType = paymentRequestType
        }
        whenever(mockPayloadDataManager.getNextReceiveAddress(accountPosition))
                .thenReturn(Observable.just(receiveAddress))
        whenever(mockContactsManager.requestSendPayment(eq(contactMdid), any()))
                .thenReturn(Completable.error { Throwable() })
        whenever(mockActivity.note).thenReturn(note)
        // Act
        subject.sendRequest()
        // Assert
        verify(mockPayloadDataManager).getNextReceiveAddress(accountPosition)
        verify(mockContactsManager).requestSendPayment(eq(contactMdid), any<PaymentRequest>())
        verifyNoMoreInteractions(mockContactsManager)
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).note
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun sendRequestFailureTypeSend() {
        // Arrange
        val contactName = "CONTACT_NAME"
        val contactMdid = "CONTACT_MDID"
        val note = "NOTE"
        val satoshis = 21000000000L
        val accountPosition = 3
        val paymentRequestType = PaymentRequestType.SEND
        val recipient = Contact().apply {
            name = contactName
            mdid = contactMdid
        }
        subject.apply {
            this.recipient = recipient
            this.satoshis = satoshis
            this.accountPosition = accountPosition
            this.paymentRequestType = paymentRequestType
        }
        whenever(mockContactsManager.requestReceivePayment(eq(contactMdid), any()))
                .thenReturn(Completable.error { Throwable() })
        whenever(mockActivity.note).thenReturn(note)
        // Act
        subject.sendRequest()
        // Assert
        verifyZeroInteractions(mockPayloadDataManager)
        verify(mockContactsManager).requestReceivePayment(eq(contactMdid), any<RequestForPaymentRequest>())
        verifyNoMoreInteractions(mockContactsManager)
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).note
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
    }

    inner class MockApiModule : ApiModule() {
        override fun provideContactsManager(rxBus: RxBus?): ContactsDataManager {
            return mockContactsManager
        }
    }

    inner class MockDataManagerModule : DataManagerModule() {
        override fun providePayloadDataManager(payloadManager: PayloadManager?, rxBus: RxBus?): PayloadDataManager {
            return mockPayloadDataManager
        }
    }

}
