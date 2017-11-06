package piuk.blockchain.android.data.walletoptions

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.api.data.ShapeShiftOptions
import info.blockchain.wallet.api.data.WalletOptions
import io.reactivex.Observable
import io.reactivex.subjects.ReplaySubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.data.auth.AuthDataManager
import piuk.blockchain.android.data.settings.SettingsDataManager
import java.util.*
import kotlin.test.assertEquals

class WalletOptionsDataManagerTest : RxTest() {

    private lateinit var subject: WalletOptionsDataManager

    private val mockAuthDataManager: AuthDataManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private var walletOptionsState = WalletOptionsState.getInstance(
            ReplaySubject.create(1),
            ReplaySubject.create(1))
    private val mockSettingsDataManager: SettingsDataManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        walletOptionsState.destroy()
        walletOptionsState = WalletOptionsState.getInstance(
                ReplaySubject.create(1),
                ReplaySubject.create(1))
        subject = WalletOptionsDataManager(mockAuthDataManager, walletOptionsState, mockSettingsDataManager)
    }

    @Test
    @Throws(Exception::class)
    fun getMobileNotice_en() {

        val message = "English notice"

        // Arrange
        val walletOptions : WalletOptions = mock()
        val map = hashMapOf("en" to message, "de" to "German notice")
        whenever(walletOptions.mobileNotice).thenReturn(map)
        whenever(mockAuthDataManager.getWalletOptions()).thenReturn(Observable.just(walletOptions))
        whenever(mockAuthDataManager.locale).thenReturn(Locale.ENGLISH)
        // Act
        val testObserver = subject.getMobileNotice().test()
        // Assert
        assertEquals(message, testObserver.values().get(0))
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun getMobileNotice_german() {

        val message = "German notice"

        // Arrange
        val walletOptions : WalletOptions = mock()
        val map = hashMapOf("en" to "english notice", "de" to message)
        whenever(walletOptions.mobileNotice).thenReturn(map)
        whenever(mockAuthDataManager.getWalletOptions()).thenReturn(Observable.just(walletOptions))
        whenever(mockAuthDataManager.locale).thenReturn(Locale.GERMAN)

        Locale.setDefault(Locale.GERMAN)

        // Act
        val testObserver = subject.getMobileNotice().test()
        // Assert
        assertEquals(message, testObserver.values().get(0))
        testObserver.assertComplete()
        Locale.setDefault(Locale.ENGLISH)
    }

    @Test
    @Throws(Exception::class)
    fun getMobileNotice_default() {

        val message = "English notice"

        // Arrange
        val walletOptions : WalletOptions = mock()
        val map = hashMapOf("en" to message)
        whenever(walletOptions.mobileNotice).thenReturn(map)
        whenever(mockAuthDataManager.getWalletOptions()).thenReturn(Observable.just(walletOptions))
        whenever(mockAuthDataManager.locale).thenReturn(Locale.GERMAN)

        Locale.setDefault(Locale.JAPAN)

        // Act
        val testObserver = subject.getMobileNotice().test()
        // Assert
        assertEquals(message, testObserver.values().get(0))
        testObserver.assertComplete()
        Locale.setDefault(Locale.ENGLISH)
    }

    @Test
    @Throws(Exception::class)
    fun showShapeshift_flag_false() {

        val showShapeshiftFlag = false

        // Arrange
        //Shapeshift flag
        val walletOptions : WalletOptions = mock()
        val shapeshift : ShapeShiftOptions = mock()
        val flagmap = hashMapOf("showShapeshift" to showShapeshiftFlag)
        whenever(walletOptions.androidFlags).thenReturn(flagmap)
        whenever(walletOptions.shapeshift).thenReturn(shapeshift)
        whenever(mockAuthDataManager.getWalletOptions()).thenReturn(Observable.just(walletOptions))

        //Country code
        val settings : Settings = mock()
        whenever(settings.countryCode).thenReturn("GB")
        whenever(mockSettingsDataManager.settings).thenReturn(Observable.just(settings))

        //State code - none

        // Act
        val testObserver = subject.showShapeshift().test()
        // Assert
        assertEquals(showShapeshiftFlag, testObserver.values().get(0))
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun showShapeshift_flag_true() {

        val showShapeshiftFlag = true

        // Arrange
        //Shapeshift flag
        val walletOptions : WalletOptions = mock()
        val shapeshift : ShapeShiftOptions = mock()
        val flagmap = hashMapOf("showShapeshift" to showShapeshiftFlag)
        whenever(walletOptions.androidFlags).thenReturn(flagmap)
        whenever(walletOptions.shapeshift).thenReturn(shapeshift)
        whenever(mockAuthDataManager.getWalletOptions()).thenReturn(Observable.just(walletOptions))

        //Country code
        val settings : Settings = mock()
        whenever(settings.countryCode).thenReturn("GB")
        whenever(mockSettingsDataManager.settings).thenReturn(Observable.just(settings))

        //State code - none

        // Act
        val testObserver = subject.showShapeshift().test()
        // Assert
        assertEquals(showShapeshiftFlag, testObserver.values().get(0))
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun showShapeshift_flag_true_blacklisted_country() {

        val showShapeshiftFlag = true

        // Arrange
        //Shapeshift falg
        val walletOptions : WalletOptions = mock()
        val shapeshift : ShapeShiftOptions = mock()
        val flagmap = hashMapOf("showShapeshift" to showShapeshiftFlag)
        whenever(walletOptions.androidFlags).thenReturn(flagmap)
        whenever(walletOptions.shapeshift).thenReturn(shapeshift)
        whenever(mockAuthDataManager.getWalletOptions()).thenReturn(Observable.just(walletOptions))

        //Country code
        val settings : Settings = mock()
        whenever(settings.countryCode).thenReturn("DE")
        whenever(mockSettingsDataManager.settings).thenReturn(Observable.just(settings))
        //Blacklist me
        whenever(shapeshift.countriesBlacklist).thenReturn(listOf("GB", "DE"))

        //State code - none

        // Act
        val testObserver = subject.showShapeshift().test()
        // Assert
        assertEquals(false, testObserver.values().get(0))
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun showShapeshift_flag_true_state_not_whitelisted() {

        val showShapeshiftFlag = true

        // Arrange
        //Shapeshift falg
        val walletOptions : WalletOptions = mock()
        val shapeshift : ShapeShiftOptions = mock()
        val flagmap = hashMapOf("showShapeshift" to showShapeshiftFlag)
        whenever(walletOptions.androidFlags).thenReturn(flagmap)
        whenever(walletOptions.shapeshift).thenReturn(shapeshift)
        whenever(mockAuthDataManager.getWalletOptions()).thenReturn(Observable.just(walletOptions))

        //Country code
        val settings : Settings = mock()
        whenever(settings.countryCode).thenReturn("US")
        whenever(mockSettingsDataManager.settings).thenReturn(Observable.just(settings))

        //State code
        //Don't whitelist me
        whenever(shapeshift.statesWhitelist).thenReturn(listOf("AR", "AZ"))
        whenever(settings.state).thenReturn("CA")

        // Act
        val testObserver = subject.showShapeshift().test()
        // Assert
        assertEquals(false, testObserver.values().get(0))
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun showShapeshift_flag_true_state_whitelisted() {

        val showShapeshiftFlag = true

        // Arrange
        //Shapeshift falg
        val walletOptions : WalletOptions = mock()
        val shapeshift : ShapeShiftOptions = mock()
        whenever(walletOptions.androidFlags).thenReturn(hashMapOf("showShapeshift" to showShapeshiftFlag))
        whenever(walletOptions.shapeshift).thenReturn(shapeshift)
        whenever(mockAuthDataManager.getWalletOptions()).thenReturn(Observable.just(walletOptions))

        //Country code
        val settings : Settings = mock()
        whenever(settings.countryCode).thenReturn("US")
        whenever(mockSettingsDataManager.settings).thenReturn(Observable.just(settings))

        //State code
        //Whitelist me
        whenever(shapeshift.statesWhitelist).thenReturn(listOf("AR", "AZ"))
        whenever(settings.state).thenReturn("AR")

        // Act
        val testObserver = subject.showShapeshift().test()
        // Assert
        assertEquals(true, testObserver.values().get(0))
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun showShapeshift_flag_false_state_whitelisted() {

        val showShapeshiftFlag = false

        // Arrange
        //Shapeshift falg
        val walletOptions : WalletOptions = mock()
        val shapeshift : ShapeShiftOptions = mock()
        whenever(walletOptions.androidFlags).thenReturn(hashMapOf("showShapeshift" to showShapeshiftFlag))
        whenever(walletOptions.shapeshift).thenReturn(shapeshift)
        whenever(mockAuthDataManager.getWalletOptions()).thenReturn(Observable.just(walletOptions))

        //Country code
        val settings : Settings = mock()
        whenever(settings.countryCode).thenReturn("US")
        whenever(mockSettingsDataManager.settings).thenReturn(Observable.just(settings))

        //State code
        //Whitelist me
        whenever(shapeshift.statesWhitelist).thenReturn(listOf("AR", "AZ"))
        whenever(settings.state).thenReturn("AR")

        // Act
        val testObserver = subject.showShapeshift().test()
        // Assert
        assertEquals(false, testObserver.values().get(0))
        testObserver.assertComplete()
    }
}
