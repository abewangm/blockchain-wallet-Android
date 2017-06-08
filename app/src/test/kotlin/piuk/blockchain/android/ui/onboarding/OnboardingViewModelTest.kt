package piuk.blockchain.android.ui.onboarding

import android.app.Application
import android.content.Context
import android.content.Intent
import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.Observable
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.data.access.AccessState
import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.data.datamanagers.SettingsDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.injection.*
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper
import piuk.blockchain.android.ui.onboarding.OnboardingActivity.EXTRAS_EMAIL_ONLY
import piuk.blockchain.android.util.PrefsUtil
import java.lang.IllegalStateException

@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class OnboardingViewModelTest {

    private lateinit var subject: OnboardingViewModel
    private val mockFingerprintHelper: FingerprintHelper = mock()
    private val mockAccessState: AccessState = mock()
    private val mockSettingsDataManager: SettingsDataManager = mock()
    private val mockPayloadDataManager: PayloadDataManager = mock(defaultAnswer = RETURNS_DEEP_STUBS)
    private val mockActivity: OnboardingViewModel.DataListener = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                MockApplicationModule(RuntimeEnvironment.application),
                ApiModule(),
                MockDataManagerModule())

        subject = OnboardingViewModel(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadySettingsFailureEmailOnly() {
        // Arrange
        val guid = "GUID"
        val sharedKey = "SHARED_KEY"
        val intent = Intent().apply { putExtra(EXTRAS_EMAIL_ONLY, true) }
        whenever(mockActivity.pageIntent).thenReturn(intent)
        whenever(mockPayloadDataManager.wallet.guid).thenReturn(guid)
        whenever(mockPayloadDataManager.wallet.sharedKey).thenReturn(sharedKey)
        whenever(mockSettingsDataManager.initSettings(guid, sharedKey))
                .thenReturn(Observable.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(mockPayloadDataManager, atLeastOnce()).wallet
        verifyNoMoreInteractions(mockPayloadDataManager)
        verify(mockSettingsDataManager).initSettings(guid, sharedKey)
        verifyNoMoreInteractions(mockSettingsDataManager)
        verify(mockActivity).pageIntent
        verify(mockActivity).showEmailPrompt()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyFingerprintHardwareAvailable() {
        // Arrange
        val guid = "GUID"
        val sharedKey = "SHARED_KEY"
        val mockSettings: Settings = mock()
        whenever(mockPayloadDataManager.wallet.guid).thenReturn(guid)
        whenever(mockPayloadDataManager.wallet.sharedKey).thenReturn(sharedKey)
        whenever(mockSettingsDataManager.initSettings(guid, sharedKey))
                .thenReturn(Observable.just(mockSettings))
        whenever(mockFingerprintHelper.isHardwareDetected()).thenReturn(true)
        // Act
        subject.onViewReady()
        // Assert
        verify(mockPayloadDataManager, atLeastOnce()).wallet
        verifyNoMoreInteractions(mockPayloadDataManager)
        verify(mockSettingsDataManager).initSettings(guid, sharedKey)
        verifyNoMoreInteractions(mockSettingsDataManager)
        verify(mockFingerprintHelper).isHardwareDetected()
        verifyNoMoreInteractions(mockFingerprintHelper)
        verify(mockActivity).pageIntent
        verify(mockActivity).showFingerprintPrompt()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyNoFingerprintHardware() {
        // Arrange
        val guid = "GUID"
        val sharedKey = "SHARED_KEY"
        val mockSettings: Settings = mock()
        whenever(mockPayloadDataManager.wallet.guid).thenReturn(guid)
        whenever(mockPayloadDataManager.wallet.sharedKey).thenReturn(sharedKey)
        whenever(mockSettingsDataManager.initSettings(guid, sharedKey))
                .thenReturn(Observable.just(mockSettings))
        whenever(mockFingerprintHelper.isHardwareDetected()).thenReturn(false)
        // Act
        subject.onViewReady()
        // Assert
        verify(mockPayloadDataManager, atLeastOnce()).wallet
        verifyNoMoreInteractions(mockPayloadDataManager)
        verify(mockSettingsDataManager).initSettings(guid, sharedKey)
        verifyNoMoreInteractions(mockSettingsDataManager)
        verify(mockFingerprintHelper).isHardwareDetected()
        verifyNoMoreInteractions(mockFingerprintHelper)
        verify(mockActivity).pageIntent
        verify(mockActivity).showEmailPrompt()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onEnableFingerprintClickedFingerprintEnrolled() {
        // Arrange
        val captor = argumentCaptor<String>()
        val pin = "1234"
        whenever(mockFingerprintHelper.isFingerprintAvailable()).thenReturn(true)
        whenever(mockAccessState.pin).thenReturn(pin)
        // Act
        subject.onEnableFingerprintClicked()
        // Assert
        verify(mockFingerprintHelper).isFingerprintAvailable()
        verifyNoMoreInteractions(mockFingerprintHelper)
        verify(mockAccessState, times(3)).pin
        verifyNoMoreInteractions(mockAccessState)
        verify(mockActivity).showFingerprintDialog(captor.capture())
        verifyNoMoreInteractions(mockActivity)
        captor.firstValue shouldEqual pin
    }

    @Test(expected = IllegalStateException::class)
    @Throws(Exception::class)
    fun onEnableFingerprintClickedNoPinFound() {
        // Arrange
        whenever(mockFingerprintHelper.isFingerprintAvailable()).thenReturn(true)
        whenever(mockAccessState.pin).thenReturn(null)
        // Act
        subject.onEnableFingerprintClicked()
        // Assert
        verify(mockFingerprintHelper).isFingerprintAvailable()
        verifyNoMoreInteractions(mockFingerprintHelper)
        verify(mockAccessState, times(3)).pin
        verifyNoMoreInteractions(mockAccessState)
        verifyZeroInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onEnableFingerprintClickedNoFingerprintEnrolled() {
        // Arrange
        whenever(mockFingerprintHelper.isFingerprintAvailable()).thenReturn(false)
        whenever(mockFingerprintHelper.isHardwareDetected()).thenReturn(true)
        // Act
        subject.onEnableFingerprintClicked()
        // Assert
        verify(mockFingerprintHelper).isFingerprintAvailable()
        verify(mockFingerprintHelper).isHardwareDetected()
        verifyNoMoreInteractions(mockFingerprintHelper)
        verify(mockActivity).showEnrollFingerprintsDialog()
        verifyNoMoreInteractions(mockActivity)
        verifyZeroInteractions(mockAccessState)
    }

    @Test(expected = IllegalStateException::class)
    @Throws(Exception::class)
    fun onEnableFingerprintClickedNoHardwareMethodCalledAccidentally() {
        // Arrange
        whenever(mockFingerprintHelper.isFingerprintAvailable()).thenReturn(false)
        whenever(mockFingerprintHelper.isHardwareDetected()).thenReturn(false)
        // Act
        subject.onEnableFingerprintClicked()
        // Assert
        verify(mockFingerprintHelper).isFingerprintAvailable()
        verify(mockFingerprintHelper).isHardwareDetected()
        verifyNoMoreInteractions(mockFingerprintHelper)
        verifyZeroInteractions(mockActivity)
        verifyZeroInteractions(mockAccessState)
    }

    @Test
    @Throws(Exception::class)
    fun setFingerprintUnlockEnabledTrue() {
        // Arrange

        // Act
        subject.setFingerprintUnlockEnabled(true)
        // Assert
        verify(mockFingerprintHelper).setFingerprintUnlockEnabled(true)
        verifyNoMoreInteractions(mockFingerprintHelper)
    }

    @Test
    @Throws(Exception::class)
    fun setFingerprintUnlockEnabledFalse() {
        // Arrange

        // Act
        subject.setFingerprintUnlockEnabled(false)
        // Assert
        verify(mockFingerprintHelper).setFingerprintUnlockEnabled(false)
        verify(mockFingerprintHelper).clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)
        verifyNoMoreInteractions(mockFingerprintHelper)
    }

    @Test
    @Throws(Exception::class)
    fun getEmail() {
        // Arrange
        val email = "EMAIL"
        subject.email = email
        // Act
        val result = subject.getEmail()
        // Assert
        result shouldEqual email
    }

    inner class MockApplicationModule(application: Application?) : ApplicationModule(application) {
        override fun provideAccessState(): AccessState {
            return mockAccessState
        }
    }

    inner class MockDataManagerModule : DataManagerModule() {
        override fun provideFingerprintHelper(applicationContext: Context?,
                                              prefsUtil: PrefsUtil?): FingerprintHelper {
            return mockFingerprintHelper
        }

        override fun provideSettingsDataManager(rxBus: RxBus?): SettingsDataManager {
            return mockSettingsDataManager
        }

        override fun providePayloadDataManager(payloadManager: PayloadManager?, rxBus: RxBus?): PayloadDataManager {
            return mockPayloadDataManager
        }
    }

}
