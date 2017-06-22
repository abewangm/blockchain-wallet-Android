package piuk.blockchain.android.ui.balance

import android.app.Application
import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.payload.PayloadManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.data.access.AccessState
import piuk.blockchain.android.data.api.EnvironmentSettings
import piuk.blockchain.android.data.datamanagers.*
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.settings.SettingsDataManager
import piuk.blockchain.android.data.stores.TransactionListStore
import piuk.blockchain.android.injection.*
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.*

@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class BalancePresenterTest {

    private lateinit var subject: BalancePresenter
    private var mockView: BalanceView = mock()
    private var mockExchangeRateFactory: ExchangeRateFactory = mock()
    private var mockTransactionListDataManager: TransactionListDataManager = mock()
    private var mockContactsDataManager: ContactsDataManager = mock()
    private var mockSwipeToReceiveHelper: SwipeToReceiveHelper = mock()
    private var mockPayloadDataManager: PayloadDataManager = mock()
    private var mockBuyDataManager: BuyDataManager = mock()
    private var mockStringUtils: StringUtils = mock()
    private var mockPrefsUtil: PrefsUtil = mock()
    private var mockAccessState: AccessState = mock()
    private var mockRxBus: RxBus = mock()
    private var mockAppUtil: AppUtil = mock()

    @Before
    fun setUp() {

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                MockApplicationModule(RuntimeEnvironment.application),
                MockApiModule(),
                MockDataManagerModule())

        subject = BalancePresenter()
        subject.initView(mockView)
    }
    
    @Test
    fun onViewReady() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun onViewDestroyed() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun `onResume`() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun `onAccountChosen`() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun `onRefreshRequested`() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun setViewType() {
        // Arrange
        whenever(mockPrefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(mockPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(mockExchangeRateFactory.getLastPrice("USD")).thenReturn(0.0)
        // Act
        subject.setViewType(true)
        // Assert
        verify(mockPrefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(mockPrefsUtil, times(2)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verifyNoMoreInteractions(mockPrefsUtil)
        verify(mockExchangeRateFactory).getLastPrice("USD")
        verifyNoMoreInteractions(mockExchangeRateFactory)
        verify(mockAccessState).setIsBtc(true)
        verifyNoMoreInteractions(mockAccessState)
        verify(mockView).onViewTypeChanged(true)
        verify(mockView).onTotalBalanceUpdated("0.0 BTC")
    }

    @Test
    fun invertViewType() {
        // Arrange
        whenever(mockAccessState.isBtc).thenReturn(true)
        whenever(mockPrefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(mockPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(mockExchangeRateFactory.getLastPrice("USD")).thenReturn(0.0)
        // Act
        subject.invertViewType()
        // Assert
        verify(mockPrefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(mockPrefsUtil).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verifyNoMoreInteractions(mockPrefsUtil)
        verify(mockExchangeRateFactory).getLastPrice("USD")
        verifyNoMoreInteractions(mockExchangeRateFactory)
        verify(mockAccessState).isBtc
        verify(mockAccessState).setIsBtc(false)
        verifyNoMoreInteractions(mockAccessState)
        verify(mockView).onViewTypeChanged(false)
        verify(mockView).onTotalBalanceUpdated("0.00 USD")
    }

    @Test
    fun `areLauncherShortcutsEnabled`() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun `onPendingTransactionClicked`() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun `onPendingTransactionLongClicked`() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun `onAccountChosen1`() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun `confirmDeclineTransaction`() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun `confirmCancelTransaction`() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun `isOnboardingComplete`() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun `setOnboardingComplete`() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun `getBitcoinClicked`() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun `disableAnnouncement`() {
        // Arrange

        // Act

        // Assert

    }

    inner class MockDataManagerModule : DataManagerModule() {
        override fun provideTransactionListDataManager(
                payloadManager: PayloadManager?,
                transactionListStore: TransactionListStore?,
                rxBus: RxBus?
        ) = mockTransactionListDataManager

        override fun provideSwipeToReceiveHelper(
                payloadDataManager: PayloadDataManager?,
                prefsUtil: PrefsUtil?
        ) = mockSwipeToReceiveHelper

        override fun providePayloadDataManager(
                payloadManager: PayloadManager?,
                rxBus: RxBus?
        ) = mockPayloadDataManager

        override fun provideBuyDataManager(
                onboardingDataManager: OnboardingDataManager?,
                settingsDataManager: SettingsDataManager?,
                payloadDataManager: PayloadDataManager?,
                environmentSettings: EnvironmentSettings?
        ) = mockBuyDataManager
    }

    inner  class MockApiModule: ApiModule() {
        override fun provideContactsManager(rxBus: RxBus?) = mockContactsDataManager
    }

    inner class MockApplicationModule(application: Application?) : ApplicationModule(application) {
        override fun provideExchangeRateFactory() = mockExchangeRateFactory

        override fun provideStringUtils() = mockStringUtils

        override fun providePrefsUtil() = mockPrefsUtil

        override fun provideAccessState() = mockAccessState

        override fun provideRxBus() = mockRxBus

        override fun provideAppUtil() = mockAppUtil
    }

}
