package piuk.blockchain.android.ui.backup.wordlist

import android.os.Bundle
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.payload.PayloadManager
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.injection.*
import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListFragment.Companion.ARGUMENT_SECOND_PASSWORD
import piuk.blockchain.android.util.BackupWalletUtil

@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class BackupWalletWordListPresenterTest {

    private lateinit var subject: BackupWalletWordListPresenter
    private val payloadDataManager: PayloadDataManager = mock()
    private val backupWalletUtil: BackupWalletUtil = mock()
    private val view: BackupWalletWordListView = mock()

    @Before
    fun setUp() {

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                ApplicationModule(RuntimeEnvironment.application),
                ApiModule(),
                MockDataManagerModule())

        subject = BackupWalletWordListPresenter()
        subject.initView(view)
    }

    @Test
    fun `onViewReady no mnemonic`() {
        // Arrange
        val password = "PASSWORD"
        val bundle = Bundle().apply { putString(ARGUMENT_SECOND_PASSWORD, password) }
        whenever(view.getPageBundle()).thenReturn(bundle)
        whenever(backupWalletUtil.getMnemonic(password)).thenReturn(null)
        // Act
        subject.onViewReady()
        // Assert
        verify(view).getPageBundle()
        verify(view).finish()
        verifyNoMoreInteractions(view)
        verify(backupWalletUtil).getMnemonic(password)
        verifyNoMoreInteractions(backupWalletUtil)
    }

    @Test
    fun `onViewReady mnemonic loaded`() {
        // Arrange
        val password = "PASSWORD"
        val bundle = Bundle().apply { putString(ARGUMENT_SECOND_PASSWORD, password) }
        val mnemonic = listOf("one", "two", "three", "four")
        whenever(view.getPageBundle()).thenReturn(bundle)
        whenever(backupWalletUtil.getMnemonic(password)).thenReturn(mnemonic)
        // Act
        subject.onViewReady()
        // Assert
        verify(view).getPageBundle()
        verifyNoMoreInteractions(view)
        verify(backupWalletUtil).getMnemonic(password)
        verifyNoMoreInteractions(backupWalletUtil)
    }

    @Test
    fun getWordForIndex() {
        // Arrange
        val password = "PASSWORD"
        val bundle = Bundle().apply { putString(ARGUMENT_SECOND_PASSWORD, password) }
        val mnemonic = listOf("one", "two", "three", "four")
        whenever(view.getPageBundle()).thenReturn(bundle)
        whenever(backupWalletUtil.getMnemonic(password)).thenReturn(mnemonic)
        // Act
        subject.onViewReady()
        val result = subject.getWordForIndex(2)
        // Assert
        verify(view).getPageBundle()
        verifyNoMoreInteractions(view)
        verify(backupWalletUtil).getMnemonic(password)
        verifyNoMoreInteractions(backupWalletUtil)
        result `should equal to` "three"
    }

    @Test
    fun getMnemonicSize() {
        // Arrange
        val password = "PASSWORD"
        val bundle = Bundle().apply { putString(ARGUMENT_SECOND_PASSWORD, password) }
        val mnemonic = listOf("one", "two", "three", "four")
        whenever(view.getPageBundle()).thenReturn(bundle)
        whenever(backupWalletUtil.getMnemonic(password)).thenReturn(mnemonic)
        // Act
        subject.onViewReady()
        val result = subject.getMnemonicSize()
        // Assert
        verify(view).getPageBundle()
        verifyNoMoreInteractions(view)
        verify(backupWalletUtil).getMnemonic(password)
        verifyNoMoreInteractions(backupWalletUtil)
        result `should equal to` 4
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

