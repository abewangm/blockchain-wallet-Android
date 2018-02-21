package piuk.blockchain.android.data.bitcoincash

import com.nhaarman.mockito_kotlin.*
import info.blockchain.api.blockexplorer.BlockExplorer
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.metadata.Metadata
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.HDWallet
import info.blockchain.wallet.payload.data.Wallet
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.data.api.EnvironmentSettings
import piuk.blockchain.android.data.metadata.MetadataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.util.MetadataUtils
import piuk.blockchain.android.util.StringUtils
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("IllegalIdentifier")
class BchDataManagerTest : RxTest() {

    private lateinit var subject: BchDataManager

    private val payloadDataManager: PayloadDataManager = mock()
    private var bchDataStore: BchDataStore = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val environmentSettings: EnvironmentSettings = mock()
    private val metadataUtils: MetadataUtils = mock()
    private val blockExplorer: BlockExplorer = mock()
    private val stringUtils: StringUtils = mock()
    private val metadataManager: MetadataManager = mock()
    private val rxBus = RxBus()

    @Before
    override fun setUp() {
        super.setUp()

        whenever(environmentSettings.bitcoinCashNetworkParameters).thenReturn(
                BitcoinCashMainNetParams.get()
        )

        subject = BchDataManager(
                payloadDataManager,
                bchDataStore,
                metadataUtils,
                environmentSettings,
                blockExplorer,
                stringUtils,
                metadataManager,
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
        whenever(payloadDataManager.mnemonic).thenReturn(split("all all all all all all all all all all all all"))

        val metadata: Metadata = mock()
        whenever(metadataUtils.getMetadataNode(any(), any()))
                .thenReturn(metadata)
        whenever(metadata.metadata).thenReturn(null)//json is null = no metadata entry

        // create 1 account
        val account: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val accounts = mutableListOf(account)
        whenever(payloadDataManager.accounts).thenReturn(accounts)

        whenever(stringUtils.getString(any())).thenReturn("label")

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
        whenever(payloadDataManager.mnemonic).thenReturn(split("all all all all all all all all all all all all"))

        val metadata: Metadata = mock()
        whenever(metadataUtils.getMetadataNode(any(), any()))
                .thenReturn(metadata)
        whenever(metadata.metadata).thenReturn("{\"some\": \"data\"}")

        // 1 account
        val accounts: List<Account> = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(payloadDataManager.accounts).thenReturn(accounts)
        whenever(accounts.size).thenReturn(1)

        whenever(stringUtils.getString(any())).thenReturn("label")

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

        val metadata: Metadata = mock()
        whenever(metadataUtils.getMetadataNode(any(), any()))
                .thenReturn(metadata)
        whenever(metadata.metadata).thenReturn(null)//json is null = no metadata entry

        whenever(stringUtils.getString(any())).thenReturn("label")

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

        val metadata: Metadata = mock()
        whenever(metadataUtils.getMetadataNode(any(), any()))
                .thenReturn(metadata)
        whenever(metadata.metadata).thenReturn("{\"some\": \"data\"}")

        // 1 account
        val accounts: List<Account> = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(payloadDataManager.accounts).thenReturn(accounts)
        whenever(accounts.size).thenReturn(1)

        whenever(stringUtils.getString(any())).thenReturn("label")

        // Act
        val testObserver = subject.initBchWallet(key, "Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun `correctBtcOffsetIfNeed btc equal to bch account size`() {
        // Arrange
        val btcAccount: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val btcAccounts = mutableListOf(btcAccount)
        whenever(payloadDataManager.accounts).thenReturn(btcAccounts)

        val bchAccount: GenericMetadataAccount = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val bchAccounts = mutableListOf(bchAccount)
        whenever(bchDataStore.bchMetadata?.accounts).thenReturn(bchAccounts)

        // Act
        val needsSync = subject.correctBtcOffsetIfNeed("label")

        // Assert
        assertFalse(needsSync)
        verify(payloadDataManager).accounts
        verify(bchDataStore.bchMetadata)!!.accounts
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(bchDataStore.bchMetadata)
    }

    @Test
    @Throws(Exception::class)
    fun `correctBtcOffsetIfNeed btc more than bch account size`() {
        // Arrange
        val btcAccount: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val btcAccounts = mutableListOf(btcAccount, btcAccount)
        whenever(payloadDataManager.accounts).thenReturn(btcAccounts)

        val bchAccount: GenericMetadataAccount = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val bchAccounts = mutableListOf(bchAccount)
        whenever(bchDataStore.bchMetadata?.accounts).thenReturn(bchAccounts)

        // Act
        val needsSync = subject.correctBtcOffsetIfNeed("label")

        // Assert
        assertFalse(needsSync)
        verify(payloadDataManager).accounts
        verify(bchDataStore.bchMetadata)!!.accounts
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(bchDataStore.bchMetadata)
    }

    @Test
    @Throws(Exception::class)
    fun `correctBtcOffsetIfNeed btc 1 less than bch account size`() {
        // Arrange
        val btcAccountsNeeded = 1
        val mockCallCount = 1

        val btcAccount: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val btcAccounts = mutableListOf(btcAccount)
        whenever(payloadDataManager.accounts).thenReturn(btcAccounts)

        val bchAccount: GenericMetadataAccount = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val bchAccounts = mutableListOf(bchAccount, bchAccount)
        whenever(bchDataStore.bchMetadata?.accounts).thenReturn(bchAccounts)


        val mockWallet: Wallet = mock()
        val mockHdWallet: HDWallet = mock()
        whenever(btcAccount.xpub).thenReturn("xpub 2")
        whenever(mockHdWallet.addAccount(any())).thenReturn(btcAccount)
        whenever(mockWallet.hdWallets).thenReturn(mutableListOf(mockHdWallet))
        whenever(payloadDataManager.wallet).thenReturn(mockWallet)

        // Act
        val needsSync = subject.correctBtcOffsetIfNeed("label")

        // Assert
        assertTrue(needsSync)
        verify(payloadDataManager).accounts
        verify(bchDataStore.bchMetadata, times(btcAccountsNeeded + mockCallCount))!!.accounts

        verify(payloadDataManager, times(btcAccountsNeeded)).wallet
        verify(mockHdWallet, times(btcAccountsNeeded)).addAccount("label 2")
        verify(bchDataStore.bchMetadata, times(btcAccountsNeeded + mockCallCount))!!.accounts

        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(bchDataStore.bchMetadata)
    }

    @Test
    @Throws(Exception::class)
    fun `correctBtcOffsetIfNeed btc 5 less than bch account size`() {
        // Arrange
        val btcAccountsNeeded = 5
        val mockCallCount = 1

        val btcAccount: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val btcAccounts = mutableListOf(btcAccount)
        whenever(payloadDataManager.accounts).thenReturn(btcAccounts)

        val bchAccount: GenericMetadataAccount = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val bchAccounts = mutableListOf(bchAccount, bchAccount, bchAccount, bchAccount, bchAccount, bchAccount)
        whenever(bchDataStore.bchMetadata?.accounts).thenReturn(bchAccounts)


        val mockWallet: Wallet = mock()
        val mockHdWallet: HDWallet = mock()
        whenever(btcAccount.xpub).thenReturn("xpub 2")
        whenever(mockHdWallet.addAccount(any())).thenReturn(btcAccount)

        whenever(mockWallet.hdWallets).thenReturn(mutableListOf(mockHdWallet))
        whenever(payloadDataManager.wallet).thenReturn(mockWallet)

        // Act
        val needsSync = subject.correctBtcOffsetIfNeed("label")

        // Assert
        assertTrue(needsSync)
        verify(payloadDataManager).accounts
        verify(bchDataStore.bchMetadata, times(btcAccountsNeeded + mockCallCount))!!.accounts

        verify(payloadDataManager, times(btcAccountsNeeded)).wallet
        verify(mockHdWallet, times(btcAccountsNeeded)).addAccount(any())
        verify(bchDataStore.bchMetadata, times(btcAccountsNeeded + mockCallCount))!!.accounts

        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(bchDataStore.bchMetadata)
    }

    private fun split(words: String): List<String> {
        return words.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
    }
}