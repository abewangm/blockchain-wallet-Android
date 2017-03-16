package piuk.blockchain.android.ui.contacts.pairing

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import com.nhaarman.mockito_kotlin.*
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
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager
import piuk.blockchain.android.data.notifications.NotificationPayload
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.injection.*
import piuk.blockchain.android.ui.contacts.pairing.ContactsQrViewModel.DIMENSION_QR_CODE
import piuk.blockchain.android.ui.customviews.ToastCustom

@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class ContactsQrViewModelTest {

    private lateinit var subject: ContactsQrViewModel
    private val mockActivity: ContactsQrViewModel.DataListener = mock()
    private val mockQrCodeDataManager: QrCodeDataManager = mock()
    private val mockNotificationManager: NotificationManager = mock()
    private val mockRxBus: RxBus = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                MockApplicationModule(RuntimeEnvironment.application),
                ApiModule(),
                MockDataManagerModule())

        subject = ContactsQrViewModel(mockActivity)

    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyNoFragmentBundle() {
        // Arrange
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        whenever(mockActivity.fragmentBundle).thenReturn(null)
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).fragmentBundle
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
        verifyZeroInteractions(mockQrCodeDataManager)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadySuccess() {
        // Arrange
        val name = "NAME"
        val uri = "URI"
        val mockBitmap: Bitmap = mock()
        val bundle = Bundle().apply {
            putString(ContactsInvitationBuilderQrFragment.ARGUMENT_NAME, name)
            putString(ContactsInvitationBuilderQrFragment.ARGUMENT_URI, uri)
        }
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        whenever(mockActivity.fragmentBundle).thenReturn(bundle)
        whenever(mockQrCodeDataManager.generateQrCode(uri, DIMENSION_QR_CODE))
                .thenReturn(Observable.just(mockBitmap))
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).fragmentBundle
        verify(mockActivity).updateDisplayMessage(name)
        verify(mockActivity).onQrLoaded(mockBitmap)
        verifyNoMoreInteractions(mockActivity)
        verify(mockQrCodeDataManager).generateQrCode(uri, DIMENSION_QR_CODE)
        verifyNoMoreInteractions(mockQrCodeDataManager)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyFailure() {
        // Arrange
        val name = "NAME"
        val uri = "URI"
        val bundle = Bundle().apply {
            putString(ContactsInvitationBuilderQrFragment.ARGUMENT_NAME, name)
            putString(ContactsInvitationBuilderQrFragment.ARGUMENT_URI, uri)
        }
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        whenever(mockActivity.fragmentBundle).thenReturn(bundle)
        whenever(mockQrCodeDataManager.generateQrCode(uri, DIMENSION_QR_CODE))
                .thenReturn(Observable.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).fragmentBundle
        verify(mockActivity).updateDisplayMessage(name)
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
        verify(mockQrCodeDataManager).generateQrCode(uri, DIMENSION_QR_CODE)
        verifyNoMoreInteractions(mockQrCodeDataManager)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadySubscribeAndEmitEvent() {
        // Arrange
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        val notificationPayload: NotificationPayload = mock()
        whenever(notificationPayload.type).thenReturn(NotificationPayload.NotificationType.CONTACT_REQUEST)
        whenever(mockActivity.fragmentBundle).thenReturn(null)
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        // Act
        subject.onViewReady()
        notificationObservable.onNext(notificationPayload)
        // Assert
        verify(mockActivity).fragmentBundle
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verify(mockActivity).finishPage()
        verifyNoMoreInteractions(mockActivity)
        verifyZeroInteractions(mockQrCodeDataManager)
        verify(mockRxBus).register(NotificationPayload::class.java)
        verifyNoMoreInteractions(mockRxBus)
        verify(mockNotificationManager).cancel(any())
        verifyNoMoreInteractions(mockNotificationManager)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadySubscribeAndEmitWrongEvent() {
        // Arrange
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        val notificationPayload: NotificationPayload = mock()
        whenever(notificationPayload.type).thenReturn(NotificationPayload.NotificationType.PAYMENT)
        whenever(mockActivity.fragmentBundle).thenReturn(null)
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        // Act
        subject.onViewReady()
        notificationObservable.onNext(notificationPayload)
        // Assert
        verify(mockActivity).fragmentBundle
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
        verifyZeroInteractions(mockQrCodeDataManager)
        verify(mockRxBus).register(NotificationPayload::class.java)
        verifyNoMoreInteractions(mockRxBus)
        verifyZeroInteractions(mockNotificationManager)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadySubscribeAndEmitNullEvent() {
        // Arrange
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        val notificationPayload: NotificationPayload = mock()
        whenever(mockActivity.fragmentBundle).thenReturn(null)
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        // Act
        subject.onViewReady()
        notificationObservable.onNext(notificationPayload)
        // Assert
        verify(mockActivity).fragmentBundle
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
        verifyZeroInteractions(mockQrCodeDataManager)
        verify(mockRxBus).register(NotificationPayload::class.java)
        verifyNoMoreInteractions(mockRxBus)
        verifyZeroInteractions(mockNotificationManager)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadySubscribeAndEmitErrorEvent() {
        // Arrange
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockActivity.fragmentBundle).thenReturn(null)
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(notificationObservable)
        // Act
        subject.onViewReady()
        notificationObservable.onError(Throwable())
        // Assert
        verify(mockActivity).fragmentBundle
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
        verifyZeroInteractions(mockQrCodeDataManager)
        verify(mockRxBus).register(NotificationPayload::class.java)
        verifyNoMoreInteractions(mockRxBus)
        verifyZeroInteractions(mockNotificationManager)
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

    inner class MockDataManagerModule : DataManagerModule() {
        override fun provideQrDataManager(): QrCodeDataManager {
            return mockQrCodeDataManager
        }

    }

    inner class MockApplicationModule(application: Application?) : ApplicationModule(application) {
        override fun provideNotificationManager(context: Context?): NotificationManager {
            return mockNotificationManager
        }

        override fun provideRxBus(): RxBus {
            return mockRxBus
        }
    }

}
