package piuk.blockchain.android.data.ethereum

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.ethereum.EthAccountApi
import info.blockchain.wallet.ethereum.data.EthTxDetails
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.data.rxjava.RxBus

class EthDataManagerTest {

    private lateinit var subject: EthDataManager
    private val payloadManager: PayloadManager = mock()
    private val ethAccountApi: EthAccountApi = mock()
    private val ethDataStore: EthDataStore = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val rxBus = RxBus()

    @Before
    fun setUp() {
        subject = EthDataManager(
                payloadManager,
                ethAccountApi,
                ethDataStore,
                rxBus
        )
    }

    @Test
    fun clearEthAccountDetails() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun fetchEthAddress() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun getEthResponseModel() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun getEthWallet() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun getEthTransactions() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun `getTransaction found`() {
        // Arrange
        val hash = "HASH"
        val ethTxDetails: EthTxDetails = mock()
        whenever(ethAccountApi.getTransaction(hash)).thenReturn(Observable.just(ethTxDetails))
        // Act

        // Assert

    }

    @Test
    fun `getTransaction not found`() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun `getTransaction returns error`() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun hasUnconfirmedEthTransactions() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun getLatestBlock() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun getIfContract() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun getTransactionNotes() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun updateTransactionNotes() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun initEthereumWallet() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun createEthTransaction() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun signEthTransaction() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun pushEthTx() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    fun setLastTxHashObservable() {
        // Arrange

        // Act

        // Assert

    }

}