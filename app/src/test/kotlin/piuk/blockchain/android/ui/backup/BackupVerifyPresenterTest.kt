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
import piuk.blockchain.android.R
import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.injection.*
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.BackupWalletUtil
import piuk.blockchain.android.util.PrefsUtil

@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class BackupVerifyPresenterTest {

    private lateinit var subject: BackupVerifyPresenter
    private val view: BackupVerifyView = mock()
    private val payloadDataManager: PayloadDataManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val prefsUtil: PrefsUtil = mock()
    private val backupWalletUtil: BackupWalletUtil = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                MockApplicationModule(RuntimeEnvironment.application),
                ApiModule(),
                MockDataManagerModule())

        subject = BackupVerifyPresenter()
        subject.initView(view)
    }

    @Test
    fun onViewReady() {
        // Arrange
        val pairOne = 1 to "word_one"
        val pairTwo = 6 to "word_two"
        val pairThree = 7 to "word_three"
        val sequence = listOf(pairOne, pairTwo, pairThree)
        whenever(view.getPageBundle()).thenReturn(null)
        whenever(backupWalletUtil.getConfirmSequence(null)).thenReturn(sequence)
        // Act
        subject.onViewReady()
        // Assert
        verify(view).getPageBundle()
        verify(view).showWordHints(listOf(1, 6, 7))
        verifyNoMoreInteractions(view)
        verify(backupWalletUtil).getConfirmSequence(null)
        verifyNoMoreInteractions(backupWalletUtil)
    }

    @Test
    fun `onVerifyClicked failure`() {
        // Arrange
        val pairOne = 1 to "word_one"
        val pairTwo = 6 to "word_two"
        val pairThree = 7 to "word_three"
        val sequence = listOf(pairOne, pairTwo, pairThree)
        whenever(backupWalletUtil.getConfirmSequence(null)).thenReturn(sequence)
        // Act
        subject.onVerifyClicked(pairOne.second, pairTwo.second, pairTwo.second)
        // Assert
        verify(view).getPageBundle()
        verify(view).showToast(R.string.backup_word_mismatch, ToastCustom.TYPE_ERROR)
        verifyNoMoreInteractions(view)
        verify(backupWalletUtil).getConfirmSequence(null)
        verifyNoMoreInteractions(backupWalletUtil)
    }

    @Test
    fun `onVerifyClicked success`() {
        // Arrange
        val pairOne = 1 to "word_one"
        val pairTwo = 6 to "word_two"
        val pairThree = 7 to "word_three"
        val sequence = listOf(pairOne, pairTwo, pairThree)
        whenever(backupWalletUtil.getConfirmSequence(null)).thenReturn(sequence)
        whenever(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete())
        whenever(payloadDataManager.wallet.hdWallets[0]).thenReturn(mock<HDWallet>())
        // Act
        subject.onVerifyClicked(pairOne.second, pairTwo.second, pairThree.second)
        // Assert
        verify(backupWalletUtil).getConfirmSequence(null)
        verifyNoMoreInteractions(backupWalletUtil)
        verify(payloadDataManager).syncPayloadWithServer()
        verify(payloadDataManager, times(2)).wallet
        verifyNoMoreInteractions(payloadDataManager)
        verify(view).getPageBundle()
        verify(view).showProgressDialog()
        verify(view).hideProgressDialog()
        verify(view).showToast(any(), eq(ToastCustom.TYPE_OK))
        verify(view).showCompletedFragment()
        verifyNoMoreInteractions(view)
        verify(prefsUtil).setValue(eq(BackupWalletActivity.BACKUP_DATE_KEY), any<Int>())
        verifyNoMoreInteractions(prefsUtil)
    }

    @Test
    @Throws(Exception::class)
    fun `updateBackupStatus success`() {
        // Arrange
        whenever(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete())
        whenever(payloadDataManager.wallet.hdWallets[0]).thenReturn(mock<HDWallet>())
        // Act
        subject.updateBackupStatus()
        // Assert
        verify(payloadDataManager).syncPayloadWithServer()
        verify(payloadDataManager, times(2)).wallet
        verifyNoMoreInteractions(payloadDataManager)
        verify(view).showProgressDialog()
        verify(view).hideProgressDialog()
        verify(view).showToast(any(), eq(ToastCustom.TYPE_OK))
        verify(view).showCompletedFragment()
        verifyNoMoreInteractions(view)
        verify(prefsUtil).setValue(eq(BackupWalletActivity.BACKUP_DATE_KEY), any<Int>())
        verifyNoMoreInteractions(prefsUtil)
    }

    @Test
    @Throws(Exception::class)
    fun `updateBackupStatus failure`() {
        // Arrange
        whenever(payloadDataManager.syncPayloadWithServer())
                .thenReturn(Completable.error { Throwable() })
        whenever(payloadDataManager.wallet.hdWallets[0]).thenReturn(mock<HDWallet>())
        // Act
        subject.updateBackupStatus()
        // Assert
        verify(payloadDataManager).syncPayloadWithServer()
        verify(payloadDataManager, times(2)).wallet
        verifyNoMoreInteractions(payloadDataManager)
        verify(view).showProgressDialog()
        verify(view).hideProgressDialog()
        verify(view).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verify(view).showStartingFragment()
        verifyNoMoreInteractions(view)
        verifyZeroInteractions(prefsUtil)
    }

    inner class MockApplicationModule(application: Application?) : ApplicationModule(application) {
        override fun providePrefsUtil(): PrefsUtil {
            return prefsUtil
        }
    }

    inner class MockDataManagerModule : DataManagerModule() {
        override fun providePayloadDataManager(
                payloadManager: PayloadManager?,
                rxBus: RxBus?
        ) = payloadDataManager

        override fun provideBackupWalletUtil(payloadDataManager: PayloadDataManager?) =
                backupWalletUtil
    }

}
