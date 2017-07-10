package piuk.blockchain.android.ui.backup.transfer

import android.annotation.SuppressLint
import android.app.Application
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.Completable
import io.reactivex.Observable
import org.apache.commons.lang3.tuple.Triple
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.data.payments.SendDataManager
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.injection.*
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.ui.send.PendingTransaction
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import java.util.*

@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class ConfirmFundsTransferPresenterTest {

    private lateinit var subject: ConfirmFundsTransferPresenter
    @Mock private val view: ConfirmFundsTransferView = mock()
    @Mock private val walletAccountHelper: WalletAccountHelper = mock()
    @Mock private val transferFundsDataManager: TransferFundsDataManager = mock()
    @Mock private val payloadDataManager: PayloadDataManager = mock()
    @Mock private val prefsUtil: PrefsUtil = mock()
    @Mock private val stringUtils: StringUtils = mock()
    @Mock private val exchangeRateFactory: ExchangeRateFactory = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                MockApplicationModule(RuntimeEnvironment.application),
                ApiModule(),
                MockDataManagerModule())

        subject = ConfirmFundsTransferPresenter()
        subject.initView(view)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReady() {
        // Arrange
        val mockPayload = mock(Wallet::class.java, RETURNS_DEEP_STUBS)
        whenever(payloadDataManager.wallet).thenReturn(mockPayload)
        whenever(mockPayload.hdWallets[0].defaultAccountIdx).thenReturn(0)
        val transaction = PendingTransaction()
        val transactions = Arrays.asList(transaction, transaction)
        val triple = Triple.of(transactions, 100000000L, 10000L)
        whenever(transferFundsDataManager.getTransferableFundTransactionList(0))
                .thenReturn(Observable.just(triple))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).setPaymentButtonEnabled(false)
        assertEquals(2, subject.pendingTransactions.size)
    }

    @Test
    @Throws(Exception::class)
    fun `accountSelected error`() {
        // Arrange
        whenever(payloadDataManager.getPositionOfAccountFromActiveList(0)).thenReturn(1)
        whenever(transferFundsDataManager.getTransferableFundTransactionList(1))
                .thenReturn(Observable.error<Triple<List<PendingTransaction>, Long, Long>>(Throwable()))
        // Act
        subject.accountSelected(0)
        // Assert
        verify(view).setPaymentButtonEnabled(false)
        verify(view).showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
        verify(view).dismissDialog()
    }

    @SuppressLint("VisibleForTests")
    @Test
    @Throws(Exception::class)
    fun updateUi() {
        // Arrange
        whenever(stringUtils.getQuantityString(anyInt(), anyInt())).thenReturn("test string")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(MonetaryUtil.UNIT_BTC)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(exchangeRateFactory.getLastPrice(anyString())).thenReturn(100.0)
        whenever(exchangeRateFactory.getSymbol(anyString())).thenReturn("$")
        subject.pendingTransactions = mutableListOf()
        // Act
        subject.updateUi(100000000L, 10000L)
        // Assert
        verify(view).updateFromLabel("test string")
        verify(view).updateTransferAmountBtc("1.0 BTC")
        verify(view).updateTransferAmountFiat("$100.00")
        verify(view).updateFeeAmountBtc("0.0001 BTC")
        verify(view).updateFeeAmountFiat("$0.01")
        verify(view).setPaymentButtonEnabled(true)
        verify(view).onUiUpdated()
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `sendPayment and archive`() {
        // Arrange
        whenever(transferFundsDataManager.sendPayment(anyList<PendingTransaction>(), anyString()))
                .thenReturn(Observable.just("hash"))
        whenever(view.getIfArchiveChecked()).thenReturn(true)
        val transaction = PendingTransaction()
        transaction.sendingObject = ItemAccount()
        transaction.sendingObject.accountObject = LegacyAddress()
        subject.pendingTransactions = mutableListOf(transaction)
        whenever(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete())
        // Act
        subject.sendPayment("password")
        // Assert
        verify(view).getIfArchiveChecked()
        verify(view).setPaymentButtonEnabled(false)
        verify(view, times(2)).showProgressDialog()
        verify(view, times(2)).hideProgressDialog()
        verify(view).showToast(R.string.transfer_confirmed, ToastCustom.TYPE_OK)
        verify(view).showToast(R.string.transfer_archive, ToastCustom.TYPE_OK)
        verify(view).dismissDialog()
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `sendPayment no archive`() {
        // Arrange
        subject.pendingTransactions = mutableListOf()
        whenever(transferFundsDataManager.sendPayment(anyList<PendingTransaction>(), anyString()))
                .thenReturn(Observable.just("hash"))
        whenever(view.getIfArchiveChecked()).thenReturn(false)
        // Act
        subject.sendPayment("password")
        // Assert
        verify(view).getIfArchiveChecked()
        verify(view).setPaymentButtonEnabled(false)
        verify(view).showProgressDialog()
        verify(view).hideProgressDialog()
        verify(view).showToast(R.string.transfer_confirmed, ToastCustom.TYPE_OK)
        verify(view).dismissDialog()
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `sendPayment error`() {
        // Arrange
        subject.pendingTransactions = mutableListOf()
        whenever(transferFundsDataManager.sendPayment(anyList<PendingTransaction>(), anyString()))
                .thenReturn(Observable.error<String>(Throwable()))
        whenever(view.getIfArchiveChecked()).thenReturn(false)
        // Act
        subject.sendPayment("password")
        // Assert
        verify(view).getIfArchiveChecked()
        verify(view).setPaymentButtonEnabled(false)
        verify(view).showProgressDialog()
        verify(view).hideProgressDialog()
        verify(view).showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
        verify(view).dismissDialog()
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun getReceiveToList() {
        // Arrange
        whenever(walletAccountHelper.getAccountItems(anyBoolean())).thenReturn(mutableListOf())
        // Act
        val value = subject.getReceiveToList()
        // Assert
        assertNotNull(value)
        assertTrue(value.isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun getDefaultAccount() {
        // Arrange
        whenever(payloadDataManager.defaultAccountIndex).thenReturn(0)
        whenever(payloadDataManager.getPositionOfAccountFromActiveList(0)).thenReturn(1)
        // Act
        val value = subject.getDefaultAccount()
        // Assert
        assertEquals(0, value.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun `archiveAll successful`() {
        // Arrange
        val transaction = PendingTransaction()
        transaction.sendingObject = ItemAccount()
        transaction.sendingObject.accountObject = LegacyAddress()
        subject.pendingTransactions = mutableListOf(transaction)
        whenever(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete())
        // Act
        subject.archiveAll()
        // Assert
        verify(view).showProgressDialog()
        verify(view).hideProgressDialog()
        verify(view).showToast(R.string.transfer_archive, ToastCustom.TYPE_OK)
        verify(view).dismissDialog()
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `archiveAll unsuccessful`() {
        // Arrange
        val transaction = PendingTransaction()
        transaction.sendingObject = ItemAccount()
        transaction.sendingObject.accountObject = LegacyAddress()
        subject.pendingTransactions = mutableListOf(transaction)
        whenever(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.error(Throwable()))
        // Act
        subject.archiveAll()
        // Assert
        verify(view).showProgressDialog()
        verify(view).hideProgressDialog()
        verify(view).showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
        verify(view).dismissDialog()
        verifyNoMoreInteractions(view)
    }

    private inner class MockApplicationModule(application: Application) : ApplicationModule(application) {

        override fun providePrefsUtil(): PrefsUtil {
            return prefsUtil
        }

        override fun provideStringUtils(): StringUtils {
            return stringUtils
        }

        override fun provideExchangeRateFactory(): ExchangeRateFactory {
            return exchangeRateFactory
        }
    }

    private inner class MockDataManagerModule : DataManagerModule() {
        override fun provideTransferFundsDataManager(payloadDataManager: PayloadDataManager,
                                                     sendDataManager: SendDataManager,
                                                     dynamicFeeCache: DynamicFeeCache): TransferFundsDataManager {
            return transferFundsDataManager
        }

        override fun provideWalletAccountHelper(payloadManager: PayloadManager,
                                                prefsUtil: PrefsUtil,
                                                stringUtils: StringUtils,
                                                exchangeRateFactory: ExchangeRateFactory): WalletAccountHelper {
            return walletAccountHelper
        }

        override fun providePayloadDataManager(payloadManager: PayloadManager,
                                               rxBus: RxBus): PayloadDataManager {
            return payloadDataManager
        }
    }

}