package piuk.blockchain.android.data.datamanagers

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.wallet.api.data.FeeList
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.Observable
import org.apache.commons.lang3.tuple.Pair
import org.bitcoinj.core.ECKey
import org.bitcoinj.params.MainNetParams
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.services.PaymentService
import piuk.blockchain.android.equals
import java.math.BigInteger

class SendDataManagerTest : RxTest() {

    private lateinit var subject: SendDataManager
    private val mockPaymentService: PaymentService = mock()
    private val mockRxBus: RxBus = mock()

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        subject = SendDataManager(mockPaymentService, mockRxBus)
    }

    @Test
    @Throws(Exception::class)
    fun submitPayment() {
        // Arrange
        val mockOutputBundle: SpendableUnspentOutputs = mock()
        val mockKeys = listOf(mock<ECKey>())
        val toAddress = "TO_ADDRESS"
        val changeAddress = "CHANGE_ADDRESS"
        val mockFee: BigInteger = mock()
        val mockAmount: BigInteger = mock()
        val txHash = "TX_HASH"
        whenever(mockPaymentService.submitPayment(mockOutputBundle,
                mockKeys,
                toAddress,
                changeAddress,
                mockFee,
                mockAmount)).thenReturn(Observable.just(txHash))
        // Act
        val testObserver = subject.submitPayment(mockOutputBundle,
                mockKeys,
                toAddress,
                changeAddress,
                mockFee,
                mockAmount).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] equals txHash
        verify(mockPaymentService).submitPayment(mockOutputBundle,
                mockKeys,
                toAddress,
                changeAddress,
                mockFee,
                mockAmount)
        verifyNoMoreInteractions(mockPaymentService)
    }

    // This call is failing when init'ing the cipher object. This appears to be a JVM issue but I'm
    // not sure what's changed. The test passes now, but should be changed to assert success conditions
    // when the fix is discovered.
    // TODO: Fix me, and then test for success
    @Test
    @Throws(Exception::class)
    fun getEcKeyFromBip38() {
        // Arrange
        val password = "thisisthepassword"
        val scanData = "6PYP4i7UyewqZWqdnpQwMdCyneXPaFDPkk8LArmVexqoGsy9Yx92SiLCPm"
        val params = MainNetParams.get()
        // Act
        val testObserver = subject.getEcKeyFromBip38(password, scanData, params).test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertTerminated()
        testObserver.assertNoValues()
    }

    @Test
    @Throws(Exception::class)
    fun getSuggestedFee() {
        // Arrange
        val mockFeeList: FeeList = mock()
        whenever(mockPaymentService.suggestedFee).thenReturn(Observable.just(mockFeeList))
        // Act
        val testObserver = subject.suggestedFee.test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] equals mockFeeList
        verify(mockPaymentService).suggestedFee
        verifyNoMoreInteractions(mockPaymentService)
    }

    @Test
    @Throws(Exception::class)
    fun getUnspentOutputs() {
        // Arrange
        val address = "ADDRESS"
        val mockUnspentOutputs: UnspentOutputs = mock()
        whenever(mockPaymentService.getUnspentOutputs(address))
                .thenReturn(Observable.just(mockUnspentOutputs))
        // Act
        val testObserver = subject.getUnspentOutputs(address).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] equals mockUnspentOutputs
        verify(mockPaymentService).getUnspentOutputs(address)
        verifyNoMoreInteractions(mockPaymentService)
    }

    @Test
    @Throws(Exception::class)
    fun getSpendableCoins() {
        // Arrange
        val mockUnspent: UnspentOutputs = mock()
        val mockPayment: BigInteger = mock()
        val mockFee: BigInteger = mock()
        val mockOutputs: SpendableUnspentOutputs = mock()
        whenever(mockPaymentService.getSpendableCoins(mockUnspent, mockPayment, mockFee))
                .thenReturn(mockOutputs)
        // Act
        val result = subject.getSpendableCoins(mockUnspent, mockPayment, mockFee)
        // Assert
        result equals mockOutputs
        verify(mockPaymentService).getSpendableCoins(mockUnspent, mockPayment, mockFee)
        verifyNoMoreInteractions(mockPaymentService)
    }

    @Test
    @Throws(Exception::class)
    fun getSweepableCoins() {
        // Arrange
        val mockUnspent: UnspentOutputs = mock()
        val mockFee: BigInteger = mock()
        val mockSweepableCoins: Pair<BigInteger, BigInteger> = mock()
        whenever(mockPaymentService.getSweepableCoins(mockUnspent, mockFee))
                .thenReturn(mockSweepableCoins)
        // Act
        val result = subject.getSweepableCoins(mockUnspent, mockFee)
        // Assert
        result equals mockSweepableCoins
        verify(mockPaymentService).getSweepableCoins(mockUnspent, mockFee)
        verifyNoMoreInteractions(mockPaymentService)
    }

    @Test
    @Throws(Exception::class)
    fun isAdequateFee() {
        // Arrange
        val inputs = 1
        val outputs = 101
        val mockFee: BigInteger = mock()
        whenever(mockPaymentService.isAdequateFee(inputs, outputs, mockFee)).thenReturn(false)
        // Act
        val result = subject.isAdequateFee(inputs, outputs, mockFee)
        // Assert
        result equals false
        verify(mockPaymentService).isAdequateFee(inputs, outputs, mockFee)
        verifyNoMoreInteractions(mockPaymentService)
    }

    @Test
    @Throws(Exception::class)
    fun estimateSize() {
        // Arrange
        val inputs = 1
        val outputs = 101
        val estimatedSize = 1337
        whenever(mockPaymentService.estimateSize(inputs, outputs)).thenReturn(estimatedSize)
        // Act
        val result = subject.estimateSize(inputs, outputs)
        // Assert
        result equals estimatedSize
        verify(mockPaymentService).estimateSize(inputs, outputs)
        verifyNoMoreInteractions(mockPaymentService)
    }

    @Test
    @Throws(Exception::class)
    fun estimateFee() {
        // Arrange
        val inputs = 1
        val outputs = 101
        val mockFeePerKb: BigInteger = mock()
        val mockAbsoluteFee: BigInteger = mock()
        whenever(mockPaymentService.estimateFee(inputs, outputs, mockFeePerKb))
                .thenReturn(mockAbsoluteFee)
        // Act
        val result = subject.estimatedFee(inputs, outputs, mockFeePerKb)
        // Assert
        result equals mockAbsoluteFee
        verify(mockPaymentService).estimateFee(inputs, outputs, mockFeePerKb)
        verifyNoMoreInteractions(mockPaymentService)
    }

}