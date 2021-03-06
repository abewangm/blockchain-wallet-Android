package piuk.blockchain.android.ui.upgrade

import android.content.Context
import com.nhaarman.mockito_kotlin.*
import io.reactivex.Completable
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.data.access.AccessState
import piuk.blockchain.android.data.auth.AuthDataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils

class UpgradeWalletPresenterTest {

    private lateinit var subject: UpgradeWalletPresenter
    private val mockActivity: UpgradeWalletView = mock()
    private val mockPrefs: PrefsUtil = mock()
    private val mockAppUtil: AppUtil = mock()
    private val mockAccessState: AccessState = mock()
    private val mockAuthDataManager: AuthDataManager = mock()
    private val mockPayloadDataManager: PayloadDataManager = mock()
    private val mockStringUtils: StringUtils = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {

        subject = UpgradeWalletPresenter(
                mockPrefs,
                mockAppUtil,
                mockAccessState,
                mockAuthDataManager,
                mockPayloadDataManager,
                mockStringUtils
        )
        subject.initView(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady password is null`() {
        // Arrange
        whenever(mockPayloadDataManager.tempPassword).thenReturn(null)
        // Act
        subject.onViewReady()
        // Assert
        verify(mockPayloadDataManager).tempPassword
        verifyNoMoreInteractions(mockPayloadDataManager)
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
        verify(mockAppUtil).clearCredentialsAndRestart()
        verifyNoMoreInteractions(mockAppUtil)
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady password strength is low`() {
        // Arrange
        val password = "PASSWORD"
        whenever(mockPayloadDataManager.tempPassword).thenReturn(password)
        // Act
        subject.onViewReady()
        // Assert
        verify(mockPayloadDataManager).tempPassword
        verifyNoMoreInteractions(mockPayloadDataManager)
        verify(mockActivity).showChangePasswordDialog()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun `submitPasswords length invalid`() {
        // Arrange
        val firstPassword = "ABC"
        val secondPassword = "ABC"
        // Act
        subject.submitPasswords(firstPassword, secondPassword)
        // Assert
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun `submitPasswords passwords don't match`() {
        // Arrange
        val firstPassword = "ABCD"
        val secondPassword = "DCDA"
        // Act
        subject.submitPasswords(firstPassword, secondPassword)
        // Assert
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun `submitPasswords create PIN successful`() {
        // Arrange
        val firstPassword = "ABCD"
        val secondPassword = "ABCD"
        val currentPassword = "CURRENT_PASSWORD"
        val pin = "1234"
        whenever(mockPayloadDataManager.tempPassword).thenReturn(currentPassword)
        whenever(mockAccessState.pin).thenReturn(pin)
        whenever(mockAuthDataManager.createPin(currentPassword, pin))
                .thenReturn(Completable.complete())
        whenever(mockPayloadDataManager.syncPayloadWithServer())
                .thenReturn(Completable.complete())
        // Act
        subject.submitPasswords(firstPassword, secondPassword)
        // Assert
        verify(mockPayloadDataManager).tempPassword
        verify(mockPayloadDataManager).tempPassword = secondPassword
        verify(mockPayloadDataManager).syncPayloadWithServer()
        verifyNoMoreInteractions(mockPayloadDataManager)
        verify(mockAuthDataManager).createPin(currentPassword, pin)
        verifyNoMoreInteractions(mockAuthDataManager)
        verify(mockActivity).showProgressDialog(any())
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_OK))
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun `submitPasswords create PIN failed`() {
        // Arrange
        val firstPassword = "ABCD"
        val secondPassword = "ABCD"
        val currentPassword = "CURRENT_PASSWORD"
        val pin = "1234"
        whenever(mockPayloadDataManager.tempPassword).thenReturn(currentPassword)
        whenever(mockAccessState.pin).thenReturn(pin)
        whenever(mockAuthDataManager.createPin(currentPassword, pin))
                .thenReturn(Completable.error { Throwable() })
        whenever(mockPayloadDataManager.syncPayloadWithServer())
                .thenReturn(Completable.complete())
        // Act
        subject.submitPasswords(firstPassword, secondPassword)
        // Assert
        verify(mockPayloadDataManager).tempPassword
        verify(mockPayloadDataManager).tempPassword = secondPassword
        verify(mockPayloadDataManager).tempPassword = currentPassword
        verify(mockPayloadDataManager).syncPayloadWithServer()
        verifyNoMoreInteractions(mockPayloadDataManager)
        verify(mockAuthDataManager).createPin(currentPassword, pin)
        verifyNoMoreInteractions(mockAuthDataManager)
        verify(mockActivity, times(2)).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verify(mockActivity).showProgressDialog(any())
        verify(mockActivity).dismissProgressDialog()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun `onUpgradeRequested successful`() {
        // Arrange
        val secondPassword = "SECOND_PASSWORD"
        val walletName = "WALLET_NAME"
        whenever(mockStringUtils.getString(any())).thenReturn(walletName)
        whenever(mockPayloadDataManager.upgradeV2toV3(secondPassword, walletName))
                .thenReturn(Completable.complete())
        // Act
        subject.onUpgradeRequested(secondPassword)
        // Assert
        verify(mockStringUtils).getString(any())
        verifyNoMoreInteractions(mockStringUtils)
        verify(mockPayloadDataManager).upgradeV2toV3(secondPassword, walletName)
        verifyNoMoreInteractions(mockPayloadDataManager)
        verify(mockAppUtil).isNewlyCreated = true
        verifyNoMoreInteractions(mockAppUtil)
        verify(mockActivity).onUpgradeStarted()
        verify(mockActivity).onUpgradeCompleted()
    }

    @Test
    @Throws(Exception::class)
    fun `onUpgradeRequested failed`() {
        // Arrange
        val secondPassword = "SECOND_PASSWORD"
        val walletName = "WALLET_NAME"
        whenever(mockStringUtils.getString(any())).thenReturn(walletName)
        whenever(mockPayloadDataManager.upgradeV2toV3(secondPassword, walletName))
                .thenReturn(Completable.error { Throwable() })
        // Act
        subject.onUpgradeRequested(secondPassword)
        // Assert
        verify(mockStringUtils).getString(any())
        verifyNoMoreInteractions(mockStringUtils)
        verify(mockPayloadDataManager).upgradeV2toV3(secondPassword, walletName)
        verifyNoMoreInteractions(mockPayloadDataManager)
        verify(mockAppUtil).isNewlyCreated = false
        verifyNoMoreInteractions(mockAppUtil)
        verify(mockActivity).onUpgradeStarted()
        verify(mockActivity).onUpgradeFailed()
    }

    @Test
    @Throws(Exception::class)
    fun onContinueClicked() {
        // Arrange

        // Act
        subject.onContinueClicked()
        // Assert
        verify(mockPrefs).setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true)
        verifyNoMoreInteractions(mockPrefs)
        verify(mockAccessState).setIsLoggedIn(true)
        verifyNoMoreInteractions(mockAccessState)
        verify(mockAppUtil).restartAppWithVerifiedPin()
        verifyNoMoreInteractions(mockAppUtil)
    }

    @Test
    @Throws(Exception::class)
    fun onBackButtonPressed() {
        // Arrange
        val mockContext: Context = mock()
        // Act
        subject.onBackButtonPressed(mockContext)
        // Assert
        verify(mockAccessState).logout(mockContext)
        verifyNoMoreInteractions(mockAccessState)
        verify(mockActivity).onBackButtonPressed()
        verifyNoMoreInteractions(mockActivity)
    }

}
