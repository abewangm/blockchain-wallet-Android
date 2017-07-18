package piuk.blockchain.android.ui.login

import com.nhaarman.mockito_kotlin.*
import io.reactivex.Completable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.PrefsUtil
import javax.net.ssl.SSLPeerUnverifiedException

class LoginPresenterTest {

    private lateinit var subject: LoginPresenter
    private var view: LoginView = mock()
    private var appUtil: AppUtil = mock()
    private var payloadDataManager: PayloadDataManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private var prefsUtil: PrefsUtil = mock()

    @Before
    fun setUp() {
        subject = LoginPresenter(appUtil, payloadDataManager, prefsUtil)
        subject.initView(view)
    }

    @Test
    fun `pairWithQR success`() {
        // Arrange
        val qrCode = "QR_CODE"
        val sharedKey = "SHARED_KEY"
        val guid = "GUID"
        whenever(payloadDataManager.handleQrCode(qrCode)).thenReturn(Completable.complete())
        whenever(payloadDataManager.wallet.sharedKey).thenReturn(sharedKey)
        whenever(payloadDataManager.wallet.guid).thenReturn(guid)
        // Act
        subject.pairWithQR(qrCode)
        // Assert
        verify(view).showProgressDialog(any())
        verify(view).dismissProgressDialog()
        verify(view).startPinEntryActivity()
        verifyNoMoreInteractions(view)
        verify(appUtil).clearCredentials()
        verify(appUtil).sharedKey = sharedKey
        verifyNoMoreInteractions(appUtil)
        verify(prefsUtil).setValue(PrefsUtil.KEY_GUID, guid)
        verify(prefsUtil).setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true)
        verify(prefsUtil).setValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, true)
        verifyNoMoreInteractions(prefsUtil)
        verify(payloadDataManager).handleQrCode(qrCode)
        verify(payloadDataManager, atLeastOnce()).wallet
        verifyNoMoreInteractions(payloadDataManager)
    }

    @Test
    fun `pairWithQR fail`() {
        // Arrange
        val qrCode = "QR_CODE"
        whenever(payloadDataManager.handleQrCode(qrCode)).thenReturn(Completable.error(Throwable()))
        // Act
        subject.pairWithQR(qrCode)
        // Assert
        verify(view).showProgressDialog(any())
        verify(view).dismissProgressDialog()
        //noinspection WrongConstant
        verify(view).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(view)
        verify(appUtil).clearCredentials()
        verify(appUtil).clearCredentialsAndRestart()
        verifyNoMoreInteractions(appUtil)
        verifyZeroInteractions(prefsUtil)
        verify(payloadDataManager).handleQrCode(qrCode)
        verifyNoMoreInteractions(payloadDataManager)
    }

    @Test
    fun `pairWithQR SSL Exception`() {
        // Arrange
        val qrCode = "QR_CODE"
        whenever(payloadDataManager.handleQrCode(qrCode)).thenReturn(Completable.error(SSLPeerUnverifiedException("")))
        // Act
        subject.pairWithQR(qrCode)
        // Assert
        verify(view).showProgressDialog(any())
        verify(view).dismissProgressDialog()
        verifyNoMoreInteractions(view)
        verify(appUtil, times(2)).clearCredentials()
        verifyNoMoreInteractions(appUtil)
        verifyZeroInteractions(prefsUtil)
        verify(payloadDataManager).handleQrCode(qrCode)
        verifyNoMoreInteractions(payloadDataManager)
        verify(view).showProgressDialog(any())
        verify(view).dismissProgressDialog()
        verifyNoMoreInteractions(appUtil)
    }

}