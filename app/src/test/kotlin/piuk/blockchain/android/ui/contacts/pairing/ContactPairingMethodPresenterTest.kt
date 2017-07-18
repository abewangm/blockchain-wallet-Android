package piuk.blockchain.android.ui.contacts.pairing

import android.app.Activity
import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.contacts.data.Contact
import io.reactivex.Observable
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.data.datamanagers.ContactsDataManager
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.AppUtil

class ContactPairingMethodPresenterTest {

    private lateinit var subject: ContactPairingMethodPresenter
    private val mockActivity: ContactsPairingMethodView = mock()
    private val mockAppUtil: AppUtil = mock()
    private val mockContactManager: ContactsDataManager = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        subject = ContactPairingMethodPresenter(mockAppUtil, mockContactManager)
        subject.initView(mockActivity)
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

}
