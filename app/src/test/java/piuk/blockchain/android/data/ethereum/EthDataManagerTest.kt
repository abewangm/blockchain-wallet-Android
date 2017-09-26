package piuk.blockchain.android.data.ethereum

import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.ethereum.EthAccountApi
import info.blockchain.wallet.ethereum.EthereumWallet
import info.blockchain.wallet.ethereum.data.EthAddressResponseMap
import info.blockchain.wallet.ethereum.data.EthLatestBlock
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.ethereum.data.EthTxDetails
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.Observable
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.amshove.kluent.*
import org.bitcoinj.core.ECKey
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.web3j.protocol.core.methods.request.RawTransaction
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.data.ethereum.models.CombinedEthModel
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.stores.Optional
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class EthDataManagerTest : RxTest() {

    private lateinit var subject: EthDataManager
    private val payloadManager: PayloadManager = mock()
    private val ethAccountApi: EthAccountApi = mock()
    private val ethDataStore: EthDataStore = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val rxBus = RxBus()

    @Before
    override fun setUp() {
        super.setUp()
        subject = EthDataManager(
                payloadManager,
                ethAccountApi,
                ethDataStore,
                rxBus
        )
    }

    @Test
    @Throws(Exception::class)
    fun clearEthAccountDetails() {
        // Arrange

        // Act
        subject.clearEthAccountDetails()
        // Assert
        verify(ethDataStore).clearEthData()
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    @Throws(Exception::class)
    fun fetchEthAddress() {
        // Arrange
        val ethAddress = "ADDRESS"
        whenever(ethDataStore.ethWallet!!.account.address).thenReturn(ethAddress)
        val ethAddressResponseMap: EthAddressResponseMap = mock()
        whenever(ethAccountApi.getEthAddress(listOf(ethAddress)))
                .thenReturn(Observable.just(ethAddressResponseMap))
        // Act
        val testObserver = subject.fetchEthAddress().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(ethDataStore, atLeastOnce()).ethWallet
        verify(ethDataStore).ethAddressResponse = any(CombinedEthModel::class)
        verifyZeroInteractions(ethDataStore)
        verify(ethAccountApi).getEthAddress(listOf(ethAddress))
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    @Throws(Exception::class)
    fun getEthResponseModel() {
        // Arrange

        // Act
        subject.getEthResponseModel()
        // Assert
        verify(ethDataStore).ethAddressResponse
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    @Throws(Exception::class)
    fun getEthWallet() {
        // Arrange

        // Act
        subject.getEthWallet()
        // Assert
        verify(ethDataStore).ethWallet
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    @Throws(Exception::class)
    fun `getEthTransactions response found with 3 transactions`() {
        // Arrange
        val combinedEthModel: CombinedEthModel = mock()
        val ethTransaction: EthTransaction = mock()
        whenever(ethDataStore.ethAddressResponse).thenReturn(combinedEthModel)
        whenever(combinedEthModel.getTransactions())
                .thenReturn(listOf(ethTransaction, ethTransaction, ethTransaction))
        // Act
        val testObserver = subject.getEthTransactions().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        val values = testObserver.values()
        values `should contain` ethTransaction
        values.size `should equal to` 3
        verify(ethDataStore).ethAddressResponse
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    @Throws(Exception::class)
    fun `getEthTransactions response not found`() {
        // Arrange
        whenever(ethDataStore.ethAddressResponse).thenReturn(null)
        // Act
        val testObserver = subject.getEthTransactions().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertNoValues()
        verify(ethDataStore).ethAddressResponse
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    @Throws(Exception::class)
    fun `getTransaction found`() {
        // Arrange
        val hash = "HASH"
        val ethTxDetails: EthTxDetails = mock()
        whenever(ethAccountApi.getTransaction(hash)).thenReturn(Observable.just(ethTxDetails))
        // Act
        val testObserver = subject.getTransaction(hash).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        val value = testObserver.values()[0]
        value `should be instance of` Optional.Some::class
        (value as Optional.Some).element `should be` ethTxDetails
        verify(ethAccountApi).getTransaction(hash)
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    @Throws(Exception::class)
    fun `getTransaction not found`() {
        // Arrange
        val hash = "HASH"
        val responseBody = ResponseBody.create(MediaType.parse("application/json"), "{\n" +
                "\t\"message\": \"Transaction not found\"\n" +
                "}")
        val httpException = HttpException(Response.error<EthTxDetails>(400, responseBody))
        whenever(ethAccountApi.getTransaction(hash)).thenReturn(Observable.error { httpException })
        // Act
        val testObserver = subject.getTransaction(hash).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        val value = testObserver.values()[0]
        value `should be instance of` Optional.None::class
        verify(ethAccountApi).getTransaction(hash)
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    @Throws(Exception::class)
    fun `getTransaction returns unknown error`() {
        // Arrange
        val hash = "HASH"
        val responseBody = ResponseBody.create(MediaType.parse("application/json"), "{\n" +
                "\t\"message\": \"Unknown error\"\n" +
                "}")
        val httpException = HttpException(Response.error<EthTxDetails>(500, responseBody))
        whenever(ethAccountApi.getTransaction(hash)).thenReturn(Observable.error { httpException })
        // Act
        val testObserver = subject.getTransaction(hash).test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertError(Throwable::class.java)
        verify(ethAccountApi).getTransaction(hash)
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    @Throws(Exception::class)
    fun `getTransaction returns exception`() {
        // Arrange
        val hash = "HASH"
        whenever(ethAccountApi.getTransaction(hash)).thenReturn(Observable.error { IOException() })
        // Act
        val testObserver = subject.getTransaction(hash).test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertError(IOException::class.java)
        verify(ethAccountApi).getTransaction(hash)
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    @Throws(Exception::class)
    fun `hasUnconfirmedEthTransactions response not found`() {
        // Arrange
        whenever(ethDataStore.ethWallet).thenReturn(null)
        // Act
        val testObserver = subject.hasUnconfirmedEthTransactions().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    @Throws(Exception::class)
    fun `hasUnconfirmedEthTransactions last tx hash not found`() {
        // Arrange
        whenever(ethDataStore.ethWallet!!.lastTransactionHash).thenReturn(null)
        // Act
        val testObserver = subject.hasUnconfirmedEthTransactions().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    @Throws(Exception::class)
    fun `hasUnconfirmedEthTransactions transaction has less than 12 confirmations`() {
        // Arrange
        val hash = "HASH"
        whenever(ethDataStore.ethWallet!!.lastTransactionHash).thenReturn(hash)
        val ethTxDetails: EthTxDetails = mock()
        whenever(ethAccountApi.getTransaction(hash)).thenReturn(Observable.just(ethTxDetails))
        val latestBlock: EthLatestBlock = mock()
        whenever(ethAccountApi.latestBlock).thenReturn(Observable.just(latestBlock))
        whenever(ethTxDetails.blockNumber).thenReturn(1)
        whenever(latestBlock.blockHeight).thenReturn(10)
        // Act
        val testObserver = subject.hasUnconfirmedEthTransactions().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
        verify(ethAccountApi).getTransaction(hash)
        verify(ethAccountApi).latestBlock
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    @Throws(Exception::class)
    fun `hasUnconfirmedEthTransactions transaction has more than 12 confirmations`() {
        // Arrange
        val hash = "HASH"
        whenever(ethDataStore.ethWallet!!.lastTransactionHash).thenReturn(hash)
        val ethTxDetails: EthTxDetails = mock()
        whenever(ethAccountApi.getTransaction(hash)).thenReturn(Observable.just(ethTxDetails))
        val latestBlock: EthLatestBlock = mock()
        whenever(ethAccountApi.latestBlock).thenReturn(Observable.just(latestBlock))
        whenever(ethTxDetails.blockNumber).thenReturn(1)
        whenever(latestBlock.blockHeight).thenReturn(13)
        // Act
        val testObserver = subject.hasUnconfirmedEthTransactions().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
        verify(ethAccountApi).getTransaction(hash)
        verify(ethAccountApi).latestBlock
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    @Throws(Exception::class)
    fun getLatestBlock() {
        // Arrange
        val latestBlock: EthLatestBlock = mock()
        whenever(ethAccountApi.latestBlock).thenReturn(Observable.just(latestBlock))
        // Act
        val testObserver = subject.getLatestBlock().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(latestBlock)
        verify(ethAccountApi).latestBlock
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    @Throws(Exception::class)
    fun getIfContract() {
        // Arrange
        val address = "ADDRESS"
        whenever(ethAccountApi.getIfContract(address)).thenReturn(Observable.just(true))
        // Act
        val testObserver = subject.getIfContract(address).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
        verify(ethAccountApi).getIfContract(address)
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    @Throws(Exception::class)
    fun `getTransactionNotes returns string object`() {
        // Arrange
        val hash = "HASH"
        val notes = "NOTES"
        whenever(ethDataStore.ethWallet!!.txNotes[hash]).thenReturn(notes)
        // Act
        val result = subject.getTransactionNotes(hash)
        // Assert
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
        result `should equal` notes
    }

    @Test
    @Throws(Exception::class)
    fun `getTransactionNotes returns null object as wallet is missing`() {
        // Arrange
        val hash = "HASH"
        whenever(ethDataStore.ethWallet).thenReturn(null)
        // Act
        val result = subject.getTransactionNotes(hash)
        // Assert
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
        result `should equal` null
    }

    @Test
    @Throws(Exception::class)
    fun `updateTransactionNotes success`() {
        // Arrange
        val hash = "HASH"
        val notes = "NOTES"
        val ethereumWallet: EthereumWallet = mock()
        whenever(ethDataStore.ethWallet).thenReturn(ethereumWallet)
        // Act
        val testObserver = subject.updateTransactionNotes(hash, notes).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    @Throws(Exception::class)
    fun `updateTransactionNotes wallet not found`() {
        // Arrange
        val hash = "HASH"
        val notes = "NOTES"
        whenever(ethDataStore.ethWallet).thenReturn(null)
        // Act
        val testObserver = subject.updateTransactionNotes(hash, notes).test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertError(IllegalStateException::class.java)
        verify(ethDataStore).ethWallet
        verifyNoMoreInteractions(ethDataStore)
    }

    // TODO: This is not at all testable
    @Test
    @Throws(Exception::class)
    fun initEthereumWallet() {
        // Arrange

        // Act

        // Assert

    }

    // TODO: This isn't testable either, wrap [RawTransaction] class in interface
    @Test
    @Throws(Exception::class)
    fun createEthTransaction() {
        // Arrange

        // Act

        // Assert

    }

    @Test
    @Throws(Exception::class)
    fun signEthTransaction() {
        // Arrange
        val rawTransaction: RawTransaction = mock()
        val ecKey: ECKey = mock()
        val byteArray = ByteArray(32)
        whenever(ethDataStore.ethWallet!!.account!!.signTransaction(rawTransaction, ecKey))
                .thenReturn(byteArray)
        // Act
        val testObserver = subject.signEthTransaction(rawTransaction, ecKey).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(byteArray)
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    @Throws(Exception::class)
    fun pushEthTx() {
        // Arrange
        val byteArray = ByteArray(32)
        val hash = "HASH"
        whenever(ethAccountApi.pushTx(any(String::class))).thenReturn(Observable.just(hash))
        // Act
        val testObserver = subject.pushEthTx(byteArray).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(hash)
        verify(ethAccountApi).pushTx(any(String::class))
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    @Throws(Exception::class)
    fun setLastTxHashObservable() {
        // Arrange
        val hash = "HASH"
        val ethereumWallet: EthereumWallet = mock()
        whenever(ethDataStore.ethWallet).thenReturn(ethereumWallet)
        // Act
        val testObserver = subject.setLastTxHashObservable(hash).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(hash)
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
        verify(ethereumWallet).lastTransactionHash = hash
        verify(ethereumWallet).save()
        verifyNoMoreInteractions(ethereumWallet)
    }

}