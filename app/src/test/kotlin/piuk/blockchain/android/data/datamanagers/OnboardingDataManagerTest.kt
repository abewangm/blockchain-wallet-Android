package piuk.blockchain.android.data.datamanagers

import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.api.data.WalletOptions
import io.reactivex.Observable
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.data.access.AccessState

class OnboardingDataManagerTest: RxTest() {

    private lateinit var subject: OnboardingDataManager
    private val mockSettingsDataManager: SettingsDataManager = mock()
    private val mockAuthDataManager: AuthDataManager = mock()
    private val mockPayloadDataManager: PayloadDataManager = mock(defaultAnswer = RETURNS_DEEP_STUBS)
    private val mockAccessState: AccessState = mock()

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        subject = OnboardingDataManager(mockSettingsDataManager,
                mockAuthDataManager,
                mockPayloadDataManager,
                mockAccessState)
    }

    @Test
    @Throws(Exception::class)
    fun isSEPA() {
        // Arrange
        whenever(mockAccessState.inSepaCountry).thenReturn(true)
        // Act
        val result = subject.isSepa
        // Assert
        verify(mockAccessState).inSepaCountry
        verifyNoMoreInteractions(mockAccessState)
        result shouldEqual true
    }

    @Test
    @Throws(Exception::class)
    fun getIfSepaCountry() {
        // Arrange
        val mockWalletOptions: WalletOptions = mock()
        val mockSettings: Settings = mock()
        val guid = "GUID"
        val sharedKey = "SHARED_KEY"
        whenever(mockAuthDataManager.walletOptions).thenReturn(Observable.just(mockWalletOptions))
        whenever(mockPayloadDataManager.wallet.guid).thenReturn(guid)
        whenever(mockPayloadDataManager.wallet.sharedKey).thenReturn(sharedKey)
        whenever(mockSettingsDataManager.initSettings(guid, sharedKey))
                .thenReturn(Observable.just(mockSettings))
        whenever(mockWalletOptions.buySellCountries).thenReturn(listOf("GB"))
        whenever(mockSettings.countryCode).thenReturn("GB")
        // Act
        val testObserver = subject.ifSepaCountry.test()
        // Assert
        verify(mockAuthDataManager).walletOptions
        verifyNoMoreInteractions(mockAuthDataManager)
        verify(mockPayloadDataManager, atLeastOnce()).wallet
        verifyNoMoreInteractions(mockPayloadDataManager)
        verify(mockSettingsDataManager).initSettings(guid, sharedKey)
        verifyNoMoreInteractions(mockSettingsDataManager)
        verify(mockAccessState).inSepaCountry = true
        verifyNoMoreInteractions(mockAccessState)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

}