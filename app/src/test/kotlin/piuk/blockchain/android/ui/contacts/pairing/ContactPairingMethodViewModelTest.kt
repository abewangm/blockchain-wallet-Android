package piuk.blockchain.android.ui.contacts.pairing

import android.app.Activity
import android.app.Application
import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.contacts.data.Contact
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
import piuk.blockchain.android.util.AppUtil

@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class ContactPairingMethodViewModelTest {

    private lateinit var subject: ContactPairingMethodViewModel
    private val mockActivity: ContactPairingMethodViewModel.DataListener = mock()
    private val mockAppUtil: AppUtil = mock()
    private val mockContactManager: ContactsDataManager = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                MockApplicationModule(RuntimeEnvironment.application),
                MockApiModule(),
                DataManagerModule())

        subject = ContactPairingMethodViewModel(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun handleScanInputSuccess() {
        // Arrange
        val invitationUrl = "INVITATION_URL"
        val contact = Contact()
        whenever(mockContactManager.acceptInvitation(invitationUrl))
                .thenReturn(Observable.just(contact))
        // Act
        subject.handleScanInput(invitationUrl)
        // Assert
        verify(mockContactManager).acceptInvitation(invitationUrl)
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_OK))
        verify(mockActivity).finishActivityWithResult(Activity.RESULT_OK)
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun handleScanInputFailure() {
        // Arrange
        val invitationUrl = "INVITATION_URL"
        whenever(mockContactManager.acceptInvitation(invitationUrl))
                .thenReturn(Observable.error { Throwable() })
        // Act
        subject.handleScanInput(invitationUrl)
        // Assert
        verify(mockContactManager).acceptInvitation(invitationUrl)
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun isCameraOpen() {
        // Arrange
        whenever(mockAppUtil.isCameraOpen).thenReturn(true)
        // Act
        val result = subject.isCameraOpen
        // Assert
        result shouldEqual true
    }

    inner class MockApiModule : ApiModule() {
        override fun provideContactsManager(
                pendingTransactionListStore: PendingTransactionListStore?,
                rxBus: RxBus?
        ) = mockContactManager
    }

    inner class MockApplicationModule(application: Application?) : ApplicationModule(application) {
        override fun provideAppUtil() = mockAppUtil
    }

}
