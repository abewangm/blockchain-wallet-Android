package piuk.blockchain.android.ui.contacts.pairing

import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.metadata.data.Invitation
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
import piuk.blockchain.android.data.datamanagers.ContactsDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.stores.PendingTransactionListStore
import piuk.blockchain.android.injection.*
import piuk.blockchain.android.ui.customviews.ToastCustom
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class ContactsInvitationBuilderViewModelTest {

    private lateinit var subject: ContactsInvitationBuilderViewModel
    private val mockActivity: ContactsInvitationBuilderViewModel.DataListener = mock()
    private val mockContactManager: ContactsDataManager = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                ApplicationModule(RuntimeEnvironment.application),
                MockApiModule(),
                DataManagerModule())

        subject = ContactsInvitationBuilderViewModel(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun setNameOfSender() {
        // Arrange
        val name = "NAME"
        // Act
        subject.setNameOfSender(name)
        // Assert
        assertNotNull(subject.sender)
        subject.sender.name shouldEqual name
    }

    @Test
    @Throws(Exception::class)
    fun setNameOfRecipient() {
        // Arrange
        val name = "NAME"
        // Act
        subject.setNameOfRecipient(name)
        // Assert
        assertNotNull(subject.recipient)
        subject.recipient.name shouldEqual name
    }

    @Test
    @Throws(Exception::class)
    fun onQrCodeSelectedNoUriSuccess() {
        // Arrange
        val senderName = "SENDER_NAME"
        val recipientName = "RECIPIENT_NAME"
        val sender = Contact().apply {
            name = senderName
            invitationSent = Invitation().apply { id = "" }
        }
        val recipient = Contact().apply { name = recipientName }
        subject.apply {
            this.sender = sender
            this.recipient = recipient
        }
        whenever(mockContactManager.createInvitation(sender, recipient))
                .thenReturn(Observable.just(sender))
        // Act
        subject.onQrCodeSelected()
        // Assert
        verify(mockContactManager).createInvitation(sender, recipient)
        verifyNoMoreInteractions(mockContactManager)
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).onUriGenerated(any(), eq(recipientName))
        verifyNoMoreInteractions(mockActivity)
        assertNotNull(subject.uri)
    }

    @Test
    @Throws(Exception::class)
    fun onQrCodeSelectedNoUriFailure() {
        // Arrange
        val senderName = "SENDER_NAME"
        val recipientName = "RECIPIENT_NAME"
        val sender = Contact().apply { name = senderName }
        val recipient = Contact().apply { name = recipientName }
        subject.apply {
            this.sender = sender
            this.recipient = recipient
        }
        whenever(mockContactManager.createInvitation(sender, recipient))
                .thenReturn(Observable.error { Throwable() })
        // Act
        subject.onQrCodeSelected()
        // Assert
        verify(mockContactManager).createInvitation(sender, recipient)
        verifyNoMoreInteractions(mockContactManager)
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
        assertNull(subject.uri)
    }

    @Test
    @Throws(Exception::class)
    fun onQrCodeSelectedPreexistingUri() {
        // Arrange
        val uri = "URI"
        val recipientName = "RECIPIENT_NAME"
        subject.uri = uri
        val recipient = Contact().apply { name = recipientName }
        subject.apply { this.recipient = recipient }
        // Act
        subject.onQrCodeSelected()
        // Assert
        verify(mockActivity).onUriGenerated(uri, recipientName)
        verifyNoMoreInteractions(mockActivity)
        verifyZeroInteractions(mockContactManager)
    }

    @Test
    @Throws(Exception::class)
    fun onLinkClickedNoUriSuccess() {
        // Arrange
        val senderName = "SENDER_NAME"
        val recipientName = "RECIPIENT_NAME"
        val sender = Contact().apply {
            name = senderName
            invitationSent = Invitation().apply { id = "" }
        }
        val recipient = Contact().apply { name = recipientName }
        subject.apply {
            this.sender = sender
            this.recipient = recipient
        }
        whenever(mockContactManager.createInvitation(sender, recipient))
                .thenReturn(Observable.just(sender))
        // Act
        subject.onLinkClicked()
        // Assert
        verify(mockContactManager).createInvitation(sender, recipient)
        verifyNoMoreInteractions(mockContactManager)
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).onLinkGenerated(any())
        verifyNoMoreInteractions(mockActivity)
        assertNotNull(subject.uri)
    }

    @Test
    @Throws(Exception::class)
    fun onLinkClickedNoUriFailure() {
        // Arrange
        val senderName = "SENDER_NAME"
        val recipientName = "RECIPIENT_NAME"
        val sender = Contact().apply { name = senderName }
        val recipient = Contact().apply { name = recipientName }
        subject.apply {
            this.sender = sender
            this.recipient = recipient
        }
        whenever(mockContactManager.createInvitation(sender, recipient))
                .thenReturn(Observable.error { Throwable() })
        // Act
        subject.onLinkClicked()
        // Assert
        verify(mockContactManager).createInvitation(sender, recipient)
        verifyNoMoreInteractions(mockContactManager)
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
        assertNull(subject.uri)
    }

    @Test
    @Throws(Exception::class)
    fun onLinkClickedPreexistingUri() {
        // Arrange
        val uri = "URI"
        val recipientName = "RECIPIENT_NAME"
        val recipient = Contact().apply { name = recipientName }
        subject.uri = uri
        subject.apply { this.recipient = recipient }
        // Act
        subject.onLinkClicked()
        // Assert
        verify(mockActivity).onLinkGenerated(any())
        verifyNoMoreInteractions(mockActivity)
        verifyZeroInteractions(mockContactManager)
    }

    @Test
    @Throws(Exception::class)
    fun onDoneSelectedSuccess() {
        // Arrange
        val uri = "URI"
        val recipientName = "RECIPIENT_NAME"
        val recipient = Contact().apply { name = recipientName }
        subject.uri = uri
        subject.apply {
            this.recipient = recipient
        }
        whenever(mockContactManager.readInvitationSent(recipient)).thenReturn(Observable.just(true))
        // Act
        subject.onDoneSelected()
        // Assert
        verify(mockContactManager).readInvitationSent(recipient)
        verifyNoMoreInteractions(mockContactManager)
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).finishPage()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onDoneSelectedFailure() {
        // Arrange
        val uri = "URI"
        val recipientName = "RECIPIENT_NAME"
        val recipient = Contact().apply { name = recipientName }
        subject.uri = uri
        subject.apply {
            this.recipient = recipient
        }
        whenever(mockContactManager.readInvitationSent(recipient))
                .thenReturn(Observable.error { Throwable() })
        // Act
        subject.onDoneSelected()
        // Assert
        verify(mockContactManager).readInvitationSent(recipient)
        verifyNoMoreInteractions(mockContactManager)
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).finishPage()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onDoneSelectedNoUri() {
        // Arrange

        // Act
        subject.onDoneSelected()
        // Assert
        verify(mockActivity).finishPage()
        verifyNoMoreInteractions(mockActivity)
        assertNull(subject.uri)
    }

    inner class MockApiModule : ApiModule() {
        override fun provideContactsManager(
                pendingTransactionListStore: PendingTransactionListStore?,
                rxBus: RxBus?
        ) = mockContactManager
    }

}
