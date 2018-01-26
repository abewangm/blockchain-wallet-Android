package piuk.blockchain.android.data.metadata

import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import info.blockchain.wallet.metadata.MetadataNodeFactory
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.Completable
import io.reactivex.Observable
import org.bitcoinj.crypto.DeterministicKey
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.util.StringUtils

@Suppress("IllegalIdentifier")
class MetadataManagerTest : RxTest() {

    private lateinit var subject: MetadataManager
    private val payloadDataManager: PayloadDataManager = mock()
    private val ethDataManager: EthDataManager = mock()
    private val bchDataManager: BchDataManager = mock()
    private val stringUtils: StringUtils = mock()
    private val shapeShiftDataManager: ShapeShiftDataManager = mock()

    @Before
    override fun setUp() {
        super.setUp()
        subject = MetadataManager(
                payloadDataManager,
                ethDataManager,
                bchDataManager,
                shapeShiftDataManager,
                stringUtils
        )
    }

    @Test
    @Throws(Exception::class)
    fun `attemptMetadataSetup load success`() {
        // Arrange
        whenever(payloadDataManager.loadNodes()).thenReturn(Observable.just(true))
        val metadataNodeFactory: MetadataNodeFactory = mock()

        val key: DeterministicKey = mock()
        whenever(payloadDataManager.metadataNodeFactory).thenReturn(Observable.just(metadataNodeFactory))
        whenever(metadataNodeFactory.metadataNode).thenReturn(key)

        whenever(stringUtils.getString(any())).thenReturn("label")
        whenever(ethDataManager.initEthereumWallet(key, "label")).thenReturn(Completable.complete())
        whenever(bchDataManager.initBchWallet(key, "label")).thenReturn(Completable.complete())
        whenever(shapeShiftDataManager.initShapeshiftTradeData(key)).thenReturn(Observable.empty())
        // Act
        val testObserver = subject.attemptMetadataSetup().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(payloadDataManager).loadNodes()
        verify(payloadDataManager).metadataNodeFactory
        verify(ethDataManager).initEthereumWallet(key, "label")
        verify(bchDataManager).initBchWallet(key, "label")
        verify(shapeShiftDataManager).initShapeshiftTradeData(key)
        verifyNoMoreInteractions(payloadDataManager)
    }

    @Test
    @Throws(Exception::class)
    fun `attemptMetadataSetup load fails wo 2nd pw`() {
        // Arrange
        whenever(payloadDataManager.loadNodes()).thenReturn(Observable.just(false))
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)

        val key: DeterministicKey = mock()
        val metadataNodeFactory: MetadataNodeFactory = mock()
        whenever(metadataNodeFactory.metadataNode).thenReturn(key)

        whenever(payloadDataManager.generateAndReturnNodes(null)).thenReturn(Observable.just(metadataNodeFactory))
//
        whenever(stringUtils.getString(any())).thenReturn("label")
        whenever(ethDataManager.initEthereumWallet(key, "label")).thenReturn(Completable.complete())
        whenever(bchDataManager.initBchWallet(key, "label")).thenReturn(Completable.complete())
        whenever(shapeShiftDataManager.initShapeshiftTradeData(key)).thenReturn(Observable.empty())

        // Act
        val testObserver = subject.attemptMetadataSetup().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(payloadDataManager).loadNodes()
        verify(payloadDataManager).generateAndReturnNodes(null)
        verify(payloadDataManager).isDoubleEncrypted
        verify(ethDataManager).initEthereumWallet(key, "label")
        verify(bchDataManager).initBchWallet(key, "label")
        verify(shapeShiftDataManager).initShapeshiftTradeData(key)
    }


    @Test
    @Throws(Exception::class)
    fun `attemptMetadataSetup load fails with 2nd pw`() {
        // Arrange
        whenever(payloadDataManager.loadNodes()).thenReturn(Observable.just(false))
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(true)
        // Act
        val testObserver = subject.attemptMetadataSetup().test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertError(InvalidCredentialsException::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun `generateAndSetupMetadata load success`() {
        // Arrange
        whenever(payloadDataManager.loadNodes()).thenReturn(Observable.just(true))
        val metadataNodeFactory: MetadataNodeFactory = mock()

        val key: DeterministicKey = mock()
        whenever(payloadDataManager.metadataNodeFactory).thenReturn(Observable.just(metadataNodeFactory))
        whenever(metadataNodeFactory.metadataNode).thenReturn(key)

        whenever(stringUtils.getString(any())).thenReturn("label")
        whenever(ethDataManager.initEthereumWallet(key, "label")).thenReturn(Completable.complete())
        whenever(bchDataManager.initBchWallet(key, "label")).thenReturn(Completable.complete())
        whenever(shapeShiftDataManager.initShapeshiftTradeData(key)).thenReturn(Observable.empty())
        // Act
        val testObserver = subject.generateAndSetupMetadata("hello").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(payloadDataManager).generateNodes("hello")
        verify(payloadDataManager).loadNodes()
        verify(payloadDataManager).metadataNodeFactory
        verify(ethDataManager).initEthereumWallet(key, "label")
        verify(bchDataManager).initBchWallet(key, "label")
        verify(shapeShiftDataManager).initShapeshiftTradeData(key)
        verifyNoMoreInteractions(payloadDataManager)
    }
}