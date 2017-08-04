package piuk.blockchain.android.data.exchange

import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.api.data.WalletOptions
import io.reactivex.Observable
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.data.access.AccessState
import piuk.blockchain.android.data.auth.AuthDataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.settings.SettingsDataManager

class BuyDataManagerTest : RxTest() {

    private lateinit var subject: BuyDataManager
    private val mockSettingsDataManager: SettingsDataManager = mock(defaultAnswer = RETURNS_DEEP_STUBS)
    private val mockAuthDataManager: AuthDataManager = mock()
    private val mockPayloadDataManager: PayloadDataManager = mock(defaultAnswer = RETURNS_DEEP_STUBS)
    private val mockAccessState: AccessState = mock()
    private val mockExchangeService: ExchangeService = mock()

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        subject = BuyDataManager(mockSettingsDataManager,
                mockAuthDataManager,
                mockPayloadDataManager,
                mockAccessState,
                mockExchangeService)
    }

    @Test
    @Throws(Exception::class)
    fun `getCanBuy is sepa, no account and rolled out`() {
        // Arrange
        val mockWalletOptions: WalletOptions = mock()
        val mockSettings: Settings = mock()
        val rolloutPercent = 1.0
        whenever(mockPayloadDataManager.wallet.guid).thenReturn("7279615c-23eb-4a1c-92df-2440acea8e1a")
        whenever(mockAuthDataManager.walletOptions).thenReturn(Observable.just(mockWalletOptions))
        whenever(mockSettingsDataManager.settings)
                .thenReturn(Observable.just(mockSettings))
        whenever(mockWalletOptions.buySellCountries).thenReturn(listOf("GB"))
        whenever(mockWalletOptions.rolloutPercentage).thenReturn(rolloutPercent)
        whenever(mockSettings.countryCode).thenReturn("GB")
        whenever(mockAccessState.buySellRolloutPercent).thenReturn(1.0)
        whenever(mockExchangeService.hasCoinifyAccount()).thenReturn(Observable.just(false))
        // Act
        val testObserver = subject.canBuy.test()
        // Assert
        verify(mockAuthDataManager).walletOptions
        verifyNoMoreInteractions(mockAuthDataManager)
        verify(mockSettingsDataManager).settings
        verifyNoMoreInteractions(mockSettingsDataManager)
        verify(mockAccessState).inSepaCountry = true
        verify(mockAccessState).buySellRolloutPercent = rolloutPercent
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }


    @Test
    @Throws(Exception::class)
    fun `getCanBuy is sepa, no account, not rolled out`() {
        // Arrange
        val mockWalletOptions: WalletOptions = mock()
        val mockSettings: Settings = mock()
        val rolloutPercent = 0.0
        whenever(mockPayloadDataManager.wallet.guid).thenReturn("7279615c-23eb-4a1c-92df-2440acea8e1a")
        whenever(mockAuthDataManager.walletOptions).thenReturn(Observable.just(mockWalletOptions))
        whenever(mockSettingsDataManager.settings)
                .thenReturn(Observable.just(mockSettings))
        whenever(mockWalletOptions.buySellCountries).thenReturn(listOf("GB"))
        whenever(mockWalletOptions.rolloutPercentage).thenReturn(rolloutPercent)
        whenever(mockSettings.countryCode).thenReturn("GB")
        whenever(mockAccessState.buySellRolloutPercent).thenReturn(0.0)
        whenever(mockExchangeService.hasCoinifyAccount()).thenReturn(Observable.just(false))
        // Act
        val testObserver = subject.canBuy.test()
        // Assert
        verify(mockAuthDataManager).walletOptions
        verifyNoMoreInteractions(mockAuthDataManager)
        verify(mockSettingsDataManager).settings
        verifyNoMoreInteractions(mockSettingsDataManager)
        verify(mockAccessState).inSepaCountry = true
        verify(mockAccessState).buySellRolloutPercent = rolloutPercent
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }


    @Test
    @Throws(Exception::class)
    fun `getCanBuy account is whitelisted`() {
        // Arrange
        val mockWalletOptions: WalletOptions = mock()
        val mockSettings: Settings = mock()
        whenever(mockAuthDataManager.walletOptions).thenReturn(Observable.just(mockWalletOptions))
        whenever(mockSettingsDataManager.settings)
                .thenReturn(Observable.just(mockSettings))
        whenever(mockWalletOptions.buySellCountries).thenReturn(listOf("GB"))
        whenever(mockSettings.countryCode).thenReturn("GB")
        whenever(mockExchangeService.hasCoinifyAccount()).thenReturn(Observable.just(true))
        // Act
        val testObserver = subject.canBuy.test()
        // Assert
        verify(mockAuthDataManager).walletOptions
        verifyNoMoreInteractions(mockAuthDataManager)
        verify(mockSettingsDataManager).settings
        verifyNoMoreInteractions(mockSettingsDataManager)
        verify(mockAccessState).inSepaCountry = true
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }


    @Test
    @Throws(Exception::class)
    fun `getCanBuy not sepa, no account, rolled out`() {
        // Arrange
        val mockWalletOptions: WalletOptions = mock()
        val mockSettings: Settings = mock()
        val rolloutPercent = 1.0
        whenever(mockPayloadDataManager.wallet.guid).thenReturn("7279615c-23eb-4a1c-92df-2440acea8e1a")
        whenever(mockAuthDataManager.walletOptions).thenReturn(Observable.just(mockWalletOptions))
        whenever(mockSettingsDataManager.settings)
                .thenReturn(Observable.just(mockSettings))
        whenever(mockWalletOptions.buySellCountries).thenReturn(listOf("GB"))
        whenever(mockWalletOptions.rolloutPercentage).thenReturn(rolloutPercent)
        whenever(mockSettings.countryCode).thenReturn("US")
        whenever(mockAccessState.buySellRolloutPercent).thenReturn(1.0)
        whenever(mockExchangeService.hasCoinifyAccount()).thenReturn(Observable.just(false))
        // Act
        val testObserver = subject.canBuy.test()
        // Assert
        verify(mockAuthDataManager).walletOptions
        verifyNoMoreInteractions(mockAuthDataManager)
        verify(mockSettingsDataManager).settings
        verifyNoMoreInteractions(mockSettingsDataManager)
        verify(mockAccessState).inSepaCountry = false
        verify(mockAccessState).buySellRolloutPercent = rolloutPercent
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `allowRollout should return true for guid`() {
        // Arrange
        val rolloutPercentage = 0.5
        whenever(mockPayloadDataManager.wallet.guid).thenReturn("7279615c-23eb-4a1c-92df-2440acea8e1a")
        whenever(mockAccessState.buySellRolloutPercent).thenReturn(rolloutPercentage)
        // Act
        val rollout = subject.isRolloutAllowed
        // Assert
        verify(mockAccessState).buySellRolloutPercent
        verifyNoMoreInteractions(mockAccessState)
        Assert.assertTrue(rollout)
    }

    @Test
    fun `allowRollout should return false for guid`() {
        // Arrange
        val rolloutPercentage = 0.5
        whenever(mockPayloadDataManager.wallet.guid).thenReturn("99617f39-0f7d-41ae-8b8f-4f8984a73606")
        whenever(mockAccessState.buySellRolloutPercent).thenReturn(rolloutPercentage)
        // Act
        val rollout = subject.isRolloutAllowed
        // Assert
        verify(mockAccessState).buySellRolloutPercent
        verifyNoMoreInteractions(mockAccessState)
        Assert.assertFalse(rollout)
    }

    @Test
    @Throws(Exception::class)
    fun getIfSepaCountry() {
        // Arrange
        val mockWalletOptions: WalletOptions = mock()
        val mockSettings: Settings = mock()
        val rolloutPercent = 0.5
        whenever(mockAuthDataManager.walletOptions).thenReturn(Observable.just(mockWalletOptions))
        whenever(mockSettingsDataManager.settings)
                .thenReturn(Observable.just(mockSettings))
        whenever(mockWalletOptions.buySellCountries).thenReturn(listOf("GB"))
        whenever(mockWalletOptions.rolloutPercentage).thenReturn(rolloutPercent)
        whenever(mockSettings.countryCode).thenReturn("GB")
        // Act
        val testObserver = subject.isCoinifyAllowed.test()
        // Assert
        verify(mockAuthDataManager).walletOptions
        verifyNoMoreInteractions(mockAuthDataManager)
        verify(mockSettingsDataManager).settings
        verifyNoMoreInteractions(mockSettingsDataManager)
        verify(mockAccessState).inSepaCountry = true
        verify(mockAccessState).buySellRolloutPercent = rolloutPercent
        verifyNoMoreInteractions(mockAccessState)
        verifyZeroInteractions(mockPayloadDataManager)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

}