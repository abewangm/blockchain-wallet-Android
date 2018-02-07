package piuk.blockchain.android.data.bitcoincash

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.api.blockexplorer.BlockExplorer
import info.blockchain.wallet.metadata.Metadata
import info.blockchain.wallet.payload.data.Account
import org.amshove.kluent.mock
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
import piuk.blockchain.android.util.StringUtils

class BchDataManagerTest : RxTest() {

    private lateinit var subject: BchDataManager

    private val payloadDataManager: PayloadDataManager = mock()
    private var bchDataStore: BchDataStore = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val networkParameterUtils: NetworkParameterUtils = mock()
    private val metadataUtils: MetadataUtils = mock()
    private val blockExplorer: BlockExplorer = mock()
    private val stringUtils: StringUtils = mock()
    private val rxBus = RxBus()

    @Before
    override fun setUp() {
        super.setUp()

        whenever(networkParameterUtils.bitcoinCashParams).thenReturn(BitcoinCashMainNetParams.get())

        subject = BchDataManager(
                payloadDataManager,
                bchDataStore,
                metadataUtils,
                networkParameterUtils,
                blockExplorer,
                stringUtils,
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
        verify(bchDataStore).clearBchData()
        verifyNoMoreInteractions(bchDataStore)
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
        whenever(metadataUtils.getMetadataNode(org.amshove.kluent.any(), org.amshove.kluent.any()))
                .thenReturn(metadata)
        whenever(metadata.metadata).thenReturn(null)//json is null = no metadata entry

        // create 1 account
        val account: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val accounts = mutableListOf(account)
        whenever(payloadDataManager.accounts).thenReturn(accounts)

        // Act
        val testObserver = subject.initBchWallet(key, "Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
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
        whenever(metadataUtils.getMetadataNode(org.amshove.kluent.any(), org.amshove.kluent.any()))
                .thenReturn(metadata)
        whenever(metadata.metadata).thenReturn("{\"some\": \"data\"}")

        // 1 account
        val accounts: List<Account> = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(payloadDataManager.accounts).thenReturn(accounts)
        whenever(accounts.size).thenReturn(1)

        // Act
        val testObserver = subject.initBchWallet(key, "Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
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
        whenever(metadataUtils.getMetadataNode(org.amshove.kluent.any(), org.amshove.kluent.any()))
                .thenReturn(metadata)
        whenever(metadata.metadata).thenReturn(null)//json is null = no metadata entry

        // 1 account
        val account: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val accounts = mutableListOf(account)
        whenever(payloadDataManager.accounts).thenReturn(accounts)
        whenever(account.xpub).thenReturn("xpub6BpNXEHYiYidePgYPQGEFSL46N5phqiddhdkfw5yibtcjr2o9DtMMEaLntH2wPLtFoUN8eW7MZfRA9VfVQju368cnisuPzdBvkEYVrFZ2s5")

        // Act
        val testObserver = subject.initBchWallet(key, "Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
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
        whenever(metadataUtils.getMetadataNode(org.amshove.kluent.any(), org.amshove.kluent.any()))
                .thenReturn(metadata)
        whenever(metadata.metadata).thenReturn("{\"some\": \"data\"}")

        // 1 account
        val accounts: List<Account> = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(payloadDataManager.accounts).thenReturn(accounts)
        whenever(accounts.size).thenReturn(1)

        // Act
        val testObserver = subject.initBchWallet(key, "Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    private fun split(words: String): List<String> {
        return words.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
    }
}