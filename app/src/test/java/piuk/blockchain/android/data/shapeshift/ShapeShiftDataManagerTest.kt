package piuk.blockchain.android.data.shapeshift

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.shapeshift.ShapeShiftApi
import info.blockchain.wallet.shapeshift.ShapeShiftTrades
import info.blockchain.wallet.shapeshift.data.Quote
import info.blockchain.wallet.shapeshift.data.Trade
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.shapeshift.datastore.ShapeShiftDataStore

class ShapeShiftDataManagerTest : RxTest() {

    private lateinit var subject: ShapeShiftDataManager
    private val shapeShiftApi: ShapeShiftApi = mock()
    private val shapeShiftDataStore: ShapeShiftDataStore = mock()
    private val payloadManager: PayloadManager = mock()
    private val rxBus: RxBus = RxBus()

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        subject = ShapeShiftDataManager(shapeShiftApi, shapeShiftDataStore, payloadManager, rxBus)
    }

    @Test
    @Throws(Exception::class)
    fun initShapeshiftTradeData() {
        // Arrange
        // TODO: This isn't testable currently
        // Act

        // Assert

    }

    @Test
    @Throws(Exception::class)
    fun `getTradesList initialized`() {
        // Arrange
        val tradeData: ShapeShiftTrades = mock()
        val list = emptyList<Trade>()
        whenever(shapeShiftDataStore.tradeData).thenReturn(tradeData)
        whenever(tradeData.trades).thenReturn(list)
        // Act
        val testObserver = subject.getTradesList().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(list)
        verify(shapeShiftDataStore).tradeData
        verifyNoMoreInteractions(shapeShiftDataStore)
    }

    @Test(expected = IllegalStateException::class)
    @Throws(Exception::class)
    fun `getTradesList uninitialized`() {
        // Arrange
        whenever(shapeShiftDataStore.tradeData).thenReturn(null)
        // Act
        val testObserver = subject.getTradesList().test()
        // Assert
        testObserver.assertNotComplete()
        verify(shapeShiftDataStore).tradeData
        verifyNoMoreInteractions(shapeShiftDataStore)
    }

    @Test(expected = IllegalStateException::class)
    @Throws(Exception::class)
    fun `findTrade uninitialized`() {
        // Arrange
        val depositAddress = "DEPOSIT_ADDRESS"
        // Act
        val testObserver = subject.findTrade(depositAddress).test()
        // Assert
        testObserver.assertNotComplete()
        verify(shapeShiftDataStore).tradeData
        verifyNoMoreInteractions(shapeShiftDataStore)
    }

    @Test
    @Throws(Exception::class)
    fun `findTrade not found`() {
        // Arrange
        val depositAddress = "DEPOSIT_ADDRESS"
        val tradeData: ShapeShiftTrades = mock()
        val list = emptyList<Trade>()
        whenever(shapeShiftDataStore.tradeData).thenReturn(tradeData)
        whenever(tradeData.trades).thenReturn(list)
        // Act
        val testObserver = subject.findTrade(depositAddress).test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertError(Throwable::class.java)
        verify(shapeShiftDataStore).tradeData
        verifyNoMoreInteractions(shapeShiftDataStore)
    }

    @Test
    @Throws(Exception::class)
    fun `findTrade found`() {
        // Arrange
        val depositAddress = "DEPOSIT_ADDRESS"
        val tradeData: ShapeShiftTrades = mock()
        val trade = Trade().apply { quote = Quote().apply { deposit = depositAddress } }
        val list = listOf(trade)
        whenever(shapeShiftDataStore.tradeData).thenReturn(tradeData)
        whenever(tradeData.trades).thenReturn(list)
        // Act
        val testObserver = subject.findTrade(depositAddress).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(trade)
        verify(shapeShiftDataStore).tradeData
        verifyNoMoreInteractions(shapeShiftDataStore)
    }

    @Test
    @Throws(Exception::class)
    fun addTradeToList() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    @Throws(Exception::class)
    fun clearAllTrades() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    @Throws(Exception::class)
    fun updateTrade() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    @Throws(Exception::class)
    fun getTradeStatus() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    @Throws(Exception::class)
    fun getRate() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    @Throws(Exception::class)
    fun getQuote() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    @Throws(Exception::class)
    fun getApproximateQuote() {
        // Arrange

        // Act

        // Assert

    }

}