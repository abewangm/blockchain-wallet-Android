package piuk.blockchain.android.data.bitcoincash

import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.metadata.Metadata
import org.amshove.kluent.*
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.util.MetadataUtils
import piuk.blockchain.android.util.NetworkParameterUtils
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Suppress("IllegalIdentifier")
class BchDataManagerTest : RxTest(){

    private lateinit var subject: BchDataManager
    private val payloadDataManager: PayloadDataManager = mock()
    private lateinit var bchDataStore: BchDataStore
    private val networkParameterUtils: NetworkParameterUtils = mock()
    private val metadatUtils: MetadataUtils = mock()
    private val rxBus = RxBus()

    @Before
    override fun setUp() {
        super.setUp()

        bchDataStore = BchDataStore()

        subject = BchDataManager(
                payloadDataManager,
                bchDataStore,
                networkParameterUtils,
                metadatUtils,
                rxBus
        )
    }

    @Test
    @Throws(Exception::class)
    fun clearEthAccountDetails() {
        // Arrange

        // Act
        subject.clearBchAccountDetails()
        // Assert
        assertNull(bchDataStore.bchMetadata)
        assertNull(bchDataStore.bchWallet)
    }

    @Test
    @Throws(Exception::class)
    fun `initBchWallet create new data payload wo second pw`() {
        // Arrange
        val key: DeterministicKey = mock()

        //restoreBchWallet
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)
        whenever(networkParameterUtils.bitcoinCashParams).thenReturn(BitcoinCashMainNetParams.get())
        whenever(payloadDataManager.mnemonic).thenReturn(split("all all all all all all all all all all all all"))

        val metadata: Metadata = mock()
        whenever(metadatUtils.getMetadataNode(org.amshove.kluent.any(), org.amshove.kluent.any()))
                .thenReturn(metadata)
        whenever(metadata.getMetadata()).thenReturn(null)//json is null = no metadata entry

        // create 1 account
        val accounts: List<Account> = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(payloadDataManager.accounts).thenReturn(accounts)
        whenever(accounts.size).thenReturn(1)

        // Act
        val testObserver = subject.initBchWallet(key, "Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertNotNull(bchDataStore.bchWallet)
        assertNotNull(bchDataStore.bchMetadata)
    }

    @Test
    @Throws(Exception::class)
    fun `initBchWallet retrieve existing data payload wo second pw`() {
        // Arrange
        val key: DeterministicKey = mock()
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)
        whenever(networkParameterUtils.bitcoinCashParams).thenReturn(BitcoinCashMainNetParams.get())
        whenever(payloadDataManager.mnemonic).thenReturn(split("all all all all all all all all all all all all"))

        val metadata: Metadata = mock()
        whenever(metadatUtils.getMetadataNode(org.amshove.kluent.any(), org.amshove.kluent.any()))
                .thenReturn(metadata)
        whenever(metadata.getMetadata()).thenReturn("{\"some\": \"data\"}")

        // 1 account
        val accounts: List<Account> = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(payloadDataManager.accounts).thenReturn(accounts)
        whenever(accounts.size).thenReturn(1)

        // Act
        val testObserver = subject.initBchWallet(key, "Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertNotNull(bchDataStore.bchWallet)
        assertNotNull(bchDataStore.bchMetadata)
    }

    @Test
    @Throws(Exception::class)
    fun `initBchWallet create new data payload with second pw`() {
        // Arrange
        val key: DeterministicKey = mock()
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(true)
        val mnemonic: List<String> = mock()
        whenever(payloadDataManager.mnemonic).thenReturn(mnemonic)
        whenever(networkParameterUtils.bitcoinCashParams).thenReturn(BitcoinCashMainNetParams.get())

        val metadata: Metadata = mock()
        whenever(metadatUtils.getMetadataNode(org.amshove.kluent.any(), org.amshove.kluent.any()))
                .thenReturn(metadata)
        whenever(metadata.getMetadata()).thenReturn(null)//json is null = no metadata entry

        // 1 account
        val accounts: List<Account> = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(payloadDataManager.accounts).thenReturn(accounts)
        whenever(accounts.size).thenReturn(1)

        // Act
        val testObserver = subject.initBchWallet(key, "Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertNotNull(bchDataStore.bchWallet)
        assertNotNull(bchDataStore.bchMetadata)
    }

    @Test
    @Throws(Exception::class)
    fun `initBchWallet retrieve existing data payload with second pw`() {
        // Arrange
        val key: DeterministicKey = mock()
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(true)
        val mnemonic: List<String> = mock()
        whenever(payloadDataManager.mnemonic).thenReturn(mnemonic)
        whenever(networkParameterUtils.bitcoinCashParams).thenReturn(BitcoinCashMainNetParams.get())

        val metadata: Metadata = mock()
        whenever(metadatUtils.getMetadataNode(org.amshove.kluent.any(), org.amshove.kluent.any()))
                .thenReturn(metadata)
        whenever(metadata.getMetadata()).thenReturn("{\"some\": \"data\"}")

        // 1 account
        val accounts: List<Account> = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(payloadDataManager.accounts).thenReturn(accounts)
        whenever(accounts.size).thenReturn(1)

        // Act
        val testObserver = subject.initBchWallet(key, "Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertNotNull(bchDataStore.bchWallet)
        assertNotNull(bchDataStore.bchMetadata)
    }

    fun split(words: String): List<String> {
        return words.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
    }
}