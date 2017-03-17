package piuk.blockchain.android.ui.backup

import android.app.Application
import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.HDWallet
import io.reactivex.Completable
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.injection.*
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.PrefsUtil

@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class BackupVerifyViewModelTest {

    private lateinit var subject: BackupVerifyViewModel
    private val mockActivity: BackupVerifyViewModel.DataListener = mock()
    private val mockPayloadDataManager: PayloadDataManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val mockPrefsUtil: PrefsUtil = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                MockApplicationModule(RuntimeEnvironment.application),
                ApiModule(),
                MockDataManagerModule())

        subject = BackupVerifyViewModel(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onVerifyClickedSuccess() {
        // Arrange
        whenever(mockPayloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete())
        whenever(mockPayloadDataManager.wallet.hdWallets[0]).thenReturn(mock<HDWallet>())
        // Act
        subject.onVerifyClicked()
        // Assert
        verify(mockPayloadDataManager).syncPayloadWithServer()
        verify(mockPayloadDataManager, times(2)).wallet
        verifyNoMoreInteractions(mockPayloadDataManager)
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).hideProgressDialog()
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_OK))
        verify(mockActivity).showCompletedFragment()
        verifyNoMoreInteractions(mockActivity)
        verify(mockPrefsUtil).setValue(eq(BackupWalletActivity.BACKUP_DATE_KEY), any<Int>())
        verifyNoMoreInteractions(mockPrefsUtil)
    }

    @Test
    @Throws(Exception::class)
    fun onVerifyClickedFailure() {
        // Arrange
        whenever(mockPayloadDataManager.syncPayloadWithServer()).thenReturn(Completable.error { Throwable() })
        whenever(mockPayloadDataManager.wallet.hdWallets[0]).thenReturn(mock<HDWallet>())
        // Act
        subject.onVerifyClicked()
        // Assert
        verify(mockPayloadDataManager).syncPayloadWithServer()
        verify(mockPayloadDataManager, times(2)).wallet
        verifyNoMoreInteractions(mockPayloadDataManager)
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).hideProgressDialog()
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verify(mockActivity).showStartingFragment()
        verifyNoMoreInteractions(mockActivity)
        verifyZeroInteractions(mockPrefsUtil)
    }

    inner class MockApplicationModule(application: Application?) : ApplicationModule(application) {
        override fun providePrefsUtil(): PrefsUtil {
            return mockPrefsUtil
        }
    }

    inner class MockDataManagerModule : DataManagerModule() {
        override fun providePayloadDataManager(payloadManager: PayloadManager?): PayloadDataManager {
            return mockPayloadDataManager
        }
    }

}
