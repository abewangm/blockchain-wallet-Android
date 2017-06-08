package piuk.blockchain.android.ui.upgrade

import android.app.Application
import android.graphics.Bitmap
import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.Observable
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.data.access.AccessState
import piuk.blockchain.android.data.datamanagers.AuthDataManager
import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.injection.*
import piuk.blockchain.android.ui.pairing_code.PairingCodePresenter
import piuk.blockchain.android.ui.pairing_code.PairingCodeView
import piuk.blockchain.android.util.AESUtilWrapper
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils

@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class PairingCodePresenterTest {

    private lateinit var subject: PairingCodePresenter
    private val mockActivity: PairingCodeView = mock()

    private val mockQrCodeDataManager: QrCodeDataManager = mock()
    private val mockStringUtils: StringUtils = mock()
    private val mockPayloadDataManager: PayloadDataManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val mockAuthDataManager: AuthDataManager = mock()


    @Before
    @Throws(Exception::class)
    fun setUp() {
        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                MockApplicationModule(RuntimeEnvironment.application),
                ApiModule(),
                MockDataManagerModule())

        subject = PairingCodePresenter()
        subject.initView(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun generatePairingQr() {

        // Arrange
        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.RGB_565);
        whenever(mockPayloadDataManager.wallet.guid).thenReturn("asdf")
        whenever(mockPayloadDataManager.wallet.sharedKey).thenReturn("ghjk")
        whenever(mockPayloadDataManager.tempPassword).thenReturn("zxcv")
        whenever(mockQrCodeDataManager.generatePairingCode(any(), any(), any(), any(), any())).thenReturn(Observable.just(bitmap))

        val body = ResponseBody.create(MediaType.parse("application/text"), "asdasdasd");
        whenever(mockAuthDataManager.getPairingEncryptionPassword(any()))
                .thenReturn(Observable.just(body))
        // Act
        subject.generatePairingQr()

        // Assert
        verify(mockAuthDataManager).getPairingEncryptionPassword(any())
        verify(mockQrCodeDataManager).generatePairingCode(any(), any(), any(), any(), any())
        verify(mockActivity).showProgressSpinner()
        verify(mockActivity).onQrLoaded(bitmap)
        verify(mockActivity).hideProgressSpinner()
        verifyNoMoreInteractions(mockActivity)
    }

    inner class MockDataManagerModule : DataManagerModule() {

        override fun providePayloadDataManager(payloadManager: PayloadManager?,
                                               rxBus: RxBus?): PayloadDataManager {
            return mockPayloadDataManager
        }

        override fun provideAuthDataManager(payloadDataManager: PayloadDataManager?,
                                            prefsUtil: PrefsUtil?, appUtil: AppUtil?,
                                            accessState: AccessState?, stringUtils: StringUtils?,
                                            aesUtilWrapper: AESUtilWrapper?, rxBus: RxBus?): AuthDataManager {
            return mockAuthDataManager
        }

        override fun provideQrDataManager(): QrCodeDataManager {
            return mockQrCodeDataManager
        }
    }

    inner class MockApplicationModule(application: Application?) : ApplicationModule(application) {

        override fun provideStringUtils(): StringUtils {
            return mockStringUtils
        }
    }

}
