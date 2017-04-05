package piuk.blockchain.android.data.datamanagers

import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.api.data.WalletOptions
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.data.access.AccessState
import piuk.blockchain.android.equals

class OnboardingDataManagerTest: RxTest() {

    private lateinit var subject: OnboardingDataManager
    private val settingsDataManager: SettingsDataManager = mock()
    private val authDataManager: AuthDataManager = mock()
    private val payloadDataManager: PayloadDataManager = mock(defaultAnswer = RETURNS_DEEP_STUBS)
    private val accessState: AccessState = mock()

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        subject = OnboardingDataManager(settingsDataManager,
                authDataManager,
                payloadDataManager,
                accessState)
    }

    @Test
    @Throws(Exception::class)
    fun isSEPA() {
        // Arrange
        whenever(accessState.inSepaCountry).thenReturn(true)
        // Act
        val result = subject.isSepa
        // Assert
        verify(accessState).inSepaCountry
        verifyNoMoreInteractions(accessState)
        result equals true
    }

    @Test
    @Throws(Exception::class)
    fun getIfSepaCountry() {
        // Arrange
        val mockWalletOptions: WalletOptions = mock()
        val mockSettings: Settings = mock()
        val guid = "GUID"
        val sharedKey = "SHARED_KEY"
        whenever(authDataManager.walletOptions).thenReturn(Observable.just(mockWalletOptions))
        whenever(payloadDataManager.wallet.guid).thenReturn(guid)
        whenever(payloadDataManager.wallet.sharedKey).thenReturn(sharedKey)
        whenever(settingsDataManager.initSettings(guid, sharedKey))
                .thenReturn(Observable.just(mockSettings))
        whenever(mockWalletOptions.buySellCountries).thenReturn(listOf("GB"))
        whenever(mockSettings.countryCode).thenReturn("GB")
        // Act
        val testObserver = subject.ifSepaCountry.test()
        // Assert
        verify(authDataManager).walletOptions
        verifyNoMoreInteractions(authDataManager)
        verify(payloadDataManager, atLeastOnce()).wallet
        verifyNoMoreInteractions(payloadDataManager)
        verify(settingsDataManager).initSettings(guid, sharedKey)
        verifyNoMoreInteractions(settingsDataManager)
        verify(accessState).inSepaCountry = true
        verifyNoMoreInteractions(accessState)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

}