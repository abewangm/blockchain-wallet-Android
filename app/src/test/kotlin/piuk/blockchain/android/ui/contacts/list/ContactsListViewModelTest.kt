package piuk.blockchain.android.ui.contacts.list

import android.app.Application
import android.content.Intent
import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.metadata.MetadataNodeFactory
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType
import okhttp3.ResponseBody
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
import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.data.notifications.NotificationPayload
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.stores.PendingTransactionListStore
import piuk.blockchain.android.injection.*
import piuk.blockchain.android.ui.base.UiState
import piuk.blockchain.android.ui.customviews.ToastCustom
import java.util.*
import kotlin.test.assertNull

@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class ContactsListViewModelTest {

    private lateinit var subject: ContactsListViewModel
    private var mockActivity: ContactsListViewModel.DataListener = mock()
    private var mockContactsManager: ContactsDataManager = mock()
    private var mockPayloadDataManager: PayloadDataManager = mock()
    private val mockRxBus: RxBus = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                MockApplicationModule(RuntimeEnvironment.application),
                MockApiModule(),
                MockDataManagerModule())

        subject = ContactsListViewModel(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun handleLinkSuccessful() {
        // Arrange
        val uri = "METADATA_URI"
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        whenever(mockContactsManager.acceptInvitation(uri)).thenReturn(Observable.just(Contact()))
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.complete())
        whenever(mockContactsManager.contactList).thenReturn(Observable.empty())
        // Act
        subject.handleLink(uri)
        // Assert
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_OK))
        verify(mockContactsManager).acceptInvitation(uri)
        verify(mockContactsManager).fetchContacts()
        verify(mockContactsManager).contactList
        assertNull(subject.link)
    }

    @Test
    @Throws(Exception::class)
    fun handleLinkFailure() {
        // Arrange
        val uri = "METADATA_URI"
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        whenever(mockContactsManager.acceptInvitation(uri)).thenReturn(Observable.error { Throwable() })
        // Act
        subject.handleLink(uri)
        // Assert
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
        verify(mockContactsManager).acceptInvitation(uri)
        verifyNoMoreInteractions(mockContactsManager)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyEmitNotificationEvent() {
        // Arrange
        val notificationPayload: NotificationPayload = mock()
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        whenever(mockPayloadDataManager.loadNodes()).thenReturn(Observable.just(true))
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.complete())
        whenever(mockContactsManager.contactList).thenReturn(Observable.fromIterable(Collections.emptyList()))
        // Act
        subject.onViewReady()
        notificationObservable.onNext(notificationPayload)
        // Assert
        verify(mockRxBus).register(NotificationPayload::class.java)
        verifyNoMoreInteractions(mockRxBus)
        verify(mockActivity, times(3)).setUiState(UiState.LOADING)
        verify(mockPayloadDataManager).loadNodes()
        verify(mockContactsManager, times(2)).fetchContacts()
        verify(mockContactsManager, times(2)).contactList
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyEmitErrorEvent() {
        // Arrange
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        whenever(mockPayloadDataManager.loadNodes()).thenReturn(Observable.just(true))
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.complete())
        whenever(mockContactsManager.contactList).thenReturn(Observable.fromIterable(Collections.emptyList()))
        // Act
        subject.onViewReady()
        notificationObservable.onError(Throwable())
        // Assert
        verify(mockRxBus).register(NotificationPayload::class.java)
        verifyNoMoreInteractions(mockRxBus)
        verify(mockActivity, times(2)).setUiState(UiState.LOADING)
        verify(mockPayloadDataManager).loadNodes()
        verify(mockContactsManager).fetchContacts()
        verify(mockContactsManager).contactList
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyShouldShowSecondPasswordDialog() {
        // Arrange
        val uri = "URI"
        val intent = Intent().apply { putExtra(ContactsListActivity.EXTRA_METADATA_URI, uri) }
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        whenever(mockActivity.pageIntent).thenReturn(intent)
        whenever(mockPayloadDataManager.loadNodes()).thenReturn(Observable.just(false))
        whenever(mockPayloadDataManager.isDoubleEncrypted).thenReturn(true)
        // Act
        subject.onViewReady()
        // Assert
        verify(mockPayloadDataManager).isDoubleEncrypted
        verify(mockActivity).pageIntent
        verify(mockActivity).setUiState(UiState.LOADING)
        verify(mockActivity).showSecondPasswordDialog()
        verify(mockActivity).setUiState(UiState.FAILURE)
        verifyNoMoreInteractions(mockActivity)
        subject.link shouldEqual uri
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyShouldInitContacts() {
        // Arrange
        whenever(mockPayloadDataManager.loadNodes()).thenReturn(Observable.just(false))
        whenever(mockPayloadDataManager.isDoubleEncrypted).thenReturn(false)
        whenever(mockPayloadDataManager.generateNodes(isNull())).thenReturn(Completable.complete())
        val mockNodeFactory: MetadataNodeFactory = mock()
        whenever(mockPayloadDataManager.metadataNodeFactory)
                .thenReturn(Observable.just(mockNodeFactory))
        whenever(mockNodeFactory.sharedMetadataNode).thenReturn(mock())
        whenever(mockNodeFactory.metadataNode).thenReturn(mock())
        whenever(mockContactsManager.initContactsService(any(), any()))
                .thenReturn(Completable.complete())
        whenever(mockPayloadDataManager.registerMdid())
                .thenReturn(Observable.just(ResponseBody.create(MediaType.parse("application/json"), "")))
        whenever(mockContactsManager.publishXpub()).thenReturn(Completable.complete())
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).pageIntent
        verify(mockActivity, times(2)).setUiState(UiState.LOADING)
        verify(mockActivity).setUiState(UiState.FAILURE)
        // There will be other interactions with the mocks, but they are not tested here
        verify(mockPayloadDataManager).isDoubleEncrypted
        verify(mockPayloadDataManager).generateNodes(isNull())
        verify(mockPayloadDataManager).metadataNodeFactory
        verify(mockContactsManager).initContactsService(any(), any())
        verify(mockPayloadDataManager).registerMdid()
        verify(mockContactsManager).publishXpub()
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyShouldLoadContactsSuccessfully() {
        // Arrange
        whenever(mockPayloadDataManager.loadNodes()).thenReturn(Observable.just(true))
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.complete())
        val id = "ID"
        val contacts = listOf(
                Contact().apply { mdid = "mdid" },
                Contact().apply { mdid = "mdid" },
                Contact().apply {
                    mdid = "mdid"
                    this.id = id
                })
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        whenever(mockContactsManager.contactList).thenReturn(Observable.fromIterable(contacts))
        whenever(mockContactsManager.contactsWithUnreadPaymentRequests)
                .thenReturn(Observable.fromIterable(listOf(Contact().apply {
                    mdid = "mdid"
                    this.id = id
                })))
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).pageIntent
        verify(mockActivity, times(2)).setUiState(UiState.LOADING)
        verify(mockActivity).setUiState(UiState.CONTENT)
        verify(mockActivity).onContactsLoaded(any())
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyShouldLoadContactsEmpty() {
        // Arrange
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        whenever(mockPayloadDataManager.loadNodes()).thenReturn(Observable.just(true))
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.complete())
        whenever(mockContactsManager.contactList).thenReturn(Observable.fromIterable(listOf<Contact>()))
        whenever(mockContactsManager.contactsWithUnreadPaymentRequests)
                .thenReturn(Observable.fromIterable(listOf<Contact>()))
        whenever(mockContactsManager.readInvitationSent(any())).thenReturn(Observable.just(true))
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).pageIntent
        verify(mockActivity, times(2)).setUiState(UiState.LOADING)
        verify(mockActivity).setUiState(UiState.EMPTY)
        verify(mockActivity).onContactsLoaded(any())
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyShouldFailLoadingRequests() {
        // Arrange
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        whenever(mockPayloadDataManager.loadNodes()).thenReturn(Observable.just(true))
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.complete())
        whenever(mockContactsManager.contactList)
                .thenReturn(Observable.fromIterable(listOf<Contact>()))
        whenever(mockContactsManager.contactsWithUnreadPaymentRequests)
                .thenReturn(Observable.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).pageIntent
        verify(mockActivity, times(2)).setUiState(UiState.LOADING)
        verify(mockActivity).setUiState(UiState.FAILURE)
        verify(mockActivity).onContactsLoaded(any())
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyShouldFailLoadingContacts() {
        // Arrange
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        whenever(mockPayloadDataManager.loadNodes()).thenReturn(Observable.just(true))
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).pageIntent
        verify(mockActivity, times(2)).setUiState(UiState.LOADING)
        verify(mockActivity).setUiState(UiState.FAILURE)
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun initContactsServiceShouldThrowDecryptionException() {
        // Arrange
        val password = "PASSWORD"
        whenever(mockPayloadDataManager.generateNodes(password))
                .thenReturn(Completable.error { DecryptionException() })
        val mockNodeFactory: MetadataNodeFactory = mock()
        whenever(mockPayloadDataManager.metadataNodeFactory)
                .thenReturn(Observable.just(mockNodeFactory))
        whenever(mockNodeFactory.sharedMetadataNode).thenReturn(mock())
        whenever(mockNodeFactory.metadataNode).thenReturn(mock())
        whenever(mockContactsManager.initContactsService(any(), any()))
                .thenReturn(Completable.complete())
        // Act
        subject.initContactsService(password)
        // Assert
        verify(mockActivity).setUiState(UiState.LOADING)
        verify(mockActivity).setUiState(UiState.FAILURE)
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
        verify(mockPayloadDataManager).generateNodes(password)
        verify(mockPayloadDataManager).metadataNodeFactory
        verifyNoMoreInteractions(mockContactsManager)
    }

    @Test
    @Throws(Exception::class)
    fun initContactsServiceShouldThrowException() {
        // Arrange
        val password = "PASSWORD"
        whenever(mockPayloadDataManager.generateNodes(password))
                .thenReturn(Completable.error { Throwable() })
        val mockNodeFactory: MetadataNodeFactory = mock()
        whenever(mockPayloadDataManager.metadataNodeFactory)
                .thenReturn(Observable.just(mockNodeFactory))
        whenever(mockNodeFactory.sharedMetadataNode).thenReturn(mock())
        whenever(mockNodeFactory.metadataNode).thenReturn(mock())
        whenever(mockContactsManager.initContactsService(any(), any()))
                .thenReturn(Completable.complete())
        // Act
        subject.initContactsService(password)
        // Assert
        verify(mockActivity).setUiState(UiState.LOADING)
        verify(mockActivity).setUiState(UiState.FAILURE)
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
        verify(mockPayloadDataManager).generateNodes(password)
        verify(mockPayloadDataManager).metadataNodeFactory
        verifyNoMoreInteractions(mockContactsManager)
    }

    @Test
    @Throws(Exception::class)
    fun checkStatusOfPendingContactsSuccess() {
        // Arrange
        whenever(mockContactsManager.readInvitationSent(any<Contact>()))
                .thenReturn(Observable.just(true))
        whenever(mockContactsManager.contactList).thenReturn(Observable.error { Throwable() })
        // Act
        subject.checkStatusOfPendingContacts(listOf(Contact(), Contact(), Contact()))
        // Assert
        verify(mockContactsManager, times(3)).readInvitationSent(any<Contact>())
        verify(mockContactsManager, times(3)).contactList
        verifyNoMoreInteractions(mockContactsManager)
        verify(mockActivity, times(3)).setUiState(UiState.LOADING)
        verify(mockActivity, times(3)).setUiState(UiState.FAILURE)
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun refreshContactsFailure() {
        // Arrange
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.complete())
        whenever(mockContactsManager.contactList).thenReturn(Observable.error { Throwable() })
        // Act
        subject.refreshContacts()
        // Assert
        verify(mockContactsManager).fetchContacts()
        verify(mockContactsManager).contactList
        verifyNoMoreInteractions(mockContactsManager)
        verify(mockActivity).setUiState(UiState.LOADING)
        verify(mockActivity).setUiState(UiState.FAILURE)
        verifyNoMoreInteractions(mockActivity)
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
        override fun provideRxBus() = mockRxBus
    }

    inner class MockApiModule : ApiModule() {
        override fun provideContactsManager(
                pendingTransactionListStore: PendingTransactionListStore?,
                rxBus: RxBus?
        ) = mockContactsManager
    }

    inner class MockDataManagerModule : DataManagerModule() {
        override fun providePayloadDataManager(payloadManager: PayloadManager?, rxBus: RxBus?) =
                mockPayloadDataManager
    }

}
