package piuk.blockchain.android.data.datamanagers

import com.nhaarman.mockito_kotlin.*
import info.blockchain.api.data.Balance
import info.blockchain.wallet.metadata.MetadataNodeFactory
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payload.data.Wallet
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.Completable
import io.reactivex.Observable
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.amshove.kluent.`should equal`
import org.amshove.kluent.mock
import org.amshove.kluent.shouldEqual
import org.bitcoinj.core.ECKey
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.services.PayloadService
import java.math.BigInteger

class PayloadDataManagerTest : RxTest() {

    private lateinit var subject: PayloadDataManager
    private val mockPayloadService: PayloadService = mock()
    private val mockPayloadManager: PayloadManager = mock(defaultAnswer = RETURNS_DEEP_STUBS)
    private val rxBus = RxBus()

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        subject = PayloadDataManager(mockPayloadService, mockPayloadManager, rxBus)
    }

    @Test
    @Throws(Exception::class)
    fun initializeFromPayload() {
        // Arrange
        val payload = "{}"
        val password = "PASSWORD"
        whenever(mockPayloadService.initializeFromPayload(payload, password))
                .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.initializeFromPayload(payload, password).test()
        // Assert
        verify(mockPayloadService).initializeFromPayload(payload, password)
        verifyNoMoreInteractions(mockPayloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun restoreHdWallet() {
        // Arrange
        val mnemonic = "MNEMONIC"
        val walletName = "WALLET_NAME"
        val email = "EMAIL"
        val password = "PASSWORD"
        val mockWallet: Wallet = mock()
        whenever(mockPayloadService.restoreHdWallet(mnemonic, walletName, email, password))
                .thenReturn(Observable.just(mockWallet))
        // Act
        val testObserver = subject.restoreHdWallet(mnemonic, walletName, email, password).test()
        // Assert
        verify(mockPayloadService).restoreHdWallet(mnemonic, walletName, email, password)
        verifyNoMoreInteractions(mockPayloadService)
        testObserver.assertComplete()
        testObserver.assertValue(mockWallet)
    }

    @Test
    @Throws(Exception::class)
    fun createHdWallet() {
        // Arrange
        val password = "PASSWORD"
        val email = "EMAIL"
        val walletName = "WALLET_NAME"
        val mockWallet: Wallet = mock()
        whenever(mockPayloadService.createHdWallet(password, walletName, email))
                .thenReturn(Observable.just(mockWallet))
        // Act
        val testObserver = subject.createHdWallet(password, walletName, email).test()
        // Assert
        verify(mockPayloadService).createHdWallet(password, walletName, email)
        verifyNoMoreInteractions(mockPayloadService)
        testObserver.assertComplete()
        testObserver.assertValue(mockWallet)
    }

    @Test
    @Throws(Exception::class)
    fun initializeAndDecrypt() {
        // Arrange
        val sharedKey = "SHARED_KEY"
        val guid = "GUID"
        val password = "PASSWORD"
        whenever(mockPayloadService.initializeAndDecrypt(sharedKey, guid, password))
                .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.initializeAndDecrypt(sharedKey, guid, password).test()
        // Assert
        verify(mockPayloadService).initializeAndDecrypt(sharedKey, guid, password)
        verifyNoMoreInteractions(mockPayloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun handleQrCode() {
        // Arrange
        val data = "DATA"
        whenever(mockPayloadService.handleQrCode(data)).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.handleQrCode(data).test()
        // Assert
        verify(mockPayloadService).handleQrCode(data)
        verifyNoMoreInteractions(mockPayloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun upgradeV2toV3() {
        // Arrange
        val secondPassword = "SECOND_PASSWORD"
        val defaultAccountName = "DEFAULT_ACCOUNT_NAME"
        whenever(mockPayloadService.upgradeV2toV3(secondPassword, defaultAccountName))
                .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.upgradeV2toV3(secondPassword, defaultAccountName).test()
        // Assert
        verify(mockPayloadService).upgradeV2toV3(secondPassword, defaultAccountName)
        verifyNoMoreInteractions(mockPayloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun syncPayloadWithServer() {
        // Arrange
        whenever(mockPayloadService.syncPayloadWithServer()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.syncPayloadWithServer().test()
        // Assert
        verify(mockPayloadService).syncPayloadWithServer()
        verifyNoMoreInteractions(mockPayloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun syncPayloadAndPublicKeys() {
        // Arrange
        whenever(mockPayloadService.syncPayloadAndPublicKeys()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.syncPayloadAndPublicKeys().test()
        // Assert
        verify(mockPayloadService).syncPayloadAndPublicKeys()
        verifyNoMoreInteractions(mockPayloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun updateAllTransactions() {
        // Arrange
        whenever(mockPayloadService.updateAllTransactions()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.updateAllTransactions().test()
        // Assert
        verify(mockPayloadService).updateAllTransactions()
        verifyNoMoreInteractions(mockPayloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun updateAllBalances() {
        // Arrange
        whenever(mockPayloadService.updateAllBalances()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.updateAllBalances().test()
        // Assert
        verify(mockPayloadService).updateAllBalances()
        verifyNoMoreInteractions(mockPayloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun updateTransactionNotes() {
        // Arrange
        val txHash = "TX_HASH"
        val note = "note"
        whenever(mockPayloadService.updateTransactionNotes(txHash, note))
                .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.updateTransactionNotes(txHash, note).test()
        // Assert
        verify(mockPayloadService).updateTransactionNotes(txHash, note)
        verifyNoMoreInteractions(mockPayloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun getBalanceOfAddresses() {
        // Arrange
        val address = "ADDRESS"
        val hashMap: LinkedHashMap<String, Balance> = LinkedHashMap(mapOf(Pair(address, Balance())))
        whenever(mockPayloadService.getBalanceOfAddresses(listOf(address)))
                .thenReturn(Observable.just(hashMap))
        // Act
        val testObserver = subject.getBalanceOfAddresses(listOf(address)).test()
        // Assert
        verify(mockPayloadService).getBalanceOfAddresses(listOf(address))
        verifyNoMoreInteractions(mockPayloadService)
        testObserver.assertComplete()
        testObserver.assertValue(hashMap)
    }

    @Test
    @Throws(Exception::class)
    fun addressToLabel() {
        // Arrange
        val address = "ADDRESS"
        val label = "label"
        whenever(mockPayloadManager.getLabelFromAddress(address)).thenReturn(label)
        // Act
        val result = subject.addressToLabel(address)
        // Assert
        verify(mockPayloadManager).getLabelFromAddress(address)
        verifyNoMoreInteractions(mockPayloadManager)
        result shouldEqual label
    }

    @Test
    @Throws(Exception::class)
    fun `getNextReceiveAddress based on account index`() {
        // Arrange
        val index = 0
        val mockAccount: Account = mock()
        val accounts = listOf(mockAccount)
        val address = "ADDRESS"
        whenever(mockPayloadManager.payload.hdWallets.first().accounts).thenReturn(accounts)
        whenever(mockPayloadManager.getNextReceiveAddress(mockAccount)).thenReturn(address)
        // Act
        val testObserver = subject.getNextReceiveAddress(index).test()
        testScheduler.triggerActions()
        // Assert
        verify(mockPayloadManager).getNextReceiveAddress(mockAccount)
        testObserver.assertComplete()
        testObserver.assertValue(address)
    }

    @Test
    @Throws(Exception::class)
    fun `getNextReceiveAddress from account`() {
        // Arrange
        val mockAccount: Account = mock()
        val address = "ADDRESS"
        whenever(mockPayloadManager.getNextReceiveAddress(mockAccount)).thenReturn(address)
        // Act
        val testObserver = subject.getNextReceiveAddress(mockAccount).test()
        testScheduler.triggerActions()
        // Assert
        verify(mockPayloadManager).getNextReceiveAddress(mockAccount)
        testObserver.assertComplete()
        testObserver.assertValue(address)
    }

    @Test
    @Throws(Exception::class)
    fun getNextReceiveAddressAndReserve() {
        // Arrange
        val accountIndex = 0
        val addressLabel = "ADDRESS_LABEL"
        val address = "ADDRESS"
        val mockAccount: Account = mock()
        val accounts = listOf(mockAccount)
        whenever(mockPayloadManager.payload.hdWallets[0].accounts).thenReturn(accounts)
        whenever(mockPayloadManager.getNextReceiveAddressAndReserve(mockAccount, addressLabel))
                .thenReturn(address)
        // Act
        val testObserver = subject.getNextReceiveAddressAndReserve(accountIndex, addressLabel).test()
        testScheduler.triggerActions()
        // Assert
        verify(mockPayloadManager).getNextReceiveAddressAndReserve(mockAccount, addressLabel)
        testObserver.assertComplete()
        testObserver.assertValue(address)
    }

    @Test
    @Throws(Exception::class)
    fun `getNextChangeAddress based on account index`() {
        // Arrange
        val index = 0
        val mockAccount: Account = mock()
        val accounts = listOf(mockAccount)
        val address = "ADDRESS"
        whenever(mockPayloadManager.payload.hdWallets[0].accounts).thenReturn(accounts)
        whenever(mockPayloadManager.getNextChangeAddress(mockAccount)).thenReturn(address)
        // Act
        val testObserver = subject.getNextChangeAddress(index).test()
        testScheduler.triggerActions()
        // Assert
        verify(mockPayloadManager).getNextChangeAddress(mockAccount)
        testObserver.assertComplete()
        testObserver.assertValue(address)
    }

    @Test
    @Throws(Exception::class)
    fun `getNextChangeAddress from account`() {
        // Arrange
        val mockAccount: Account = mock()
        val address = "ADDRESS"
        whenever(mockPayloadManager.getNextChangeAddress(mockAccount)).thenReturn(address)
        // Act
        val testObserver = subject.getNextChangeAddress(mockAccount).test()
        testScheduler.triggerActions()
        // Assert
        verify(mockPayloadManager).getNextChangeAddress(mockAccount)
        testObserver.assertComplete()
        testObserver.assertValue(address)
    }

    @Test
    @Throws(Exception::class)
    fun getAddressECKey() {
        // Arrange
        val mockLegacyAddress: LegacyAddress = mock()
        val secondPassword = "SECOND_PASSWORD"
        val mockEcKey: ECKey = mock()
        whenever(mockPayloadManager.getAddressECKey(mockLegacyAddress, secondPassword))
                .thenReturn(mockEcKey)
        // Act
        val result = subject.getAddressECKey(mockLegacyAddress, secondPassword)
        // Assert
        verify(mockPayloadManager).getAddressECKey(mockLegacyAddress, secondPassword)
        verifyNoMoreInteractions(mockPayloadManager)
        result shouldEqual mockEcKey
    }

    @Test
    @Throws(Exception::class)
    fun `getAccounts returns list of accounts`() {
        // Arrange
        val mockAccount: Account = mock()
        val accounts = listOf(mockAccount)
        whenever(mockPayloadManager.payload.hdWallets.first().accounts)
                .thenReturn(accounts)
        // Act
        val result = subject.accounts
        // Assert
        verify(mockPayloadManager, atLeastOnce()).payload
        result shouldEqual accounts
    }

    @Test
    @Throws(Exception::class)
    fun `getAccounts returns empty list`() {
        // Arrange
        whenever(mockPayloadManager.payload).thenReturn(null)
        // Act
        val result = subject.accounts
        // Assert
        verify(mockPayloadManager).payload
        result shouldEqual emptyList()
    }

    @Test
    @Throws(Exception::class)
    fun `getLegacyAddresses returns list of legacy addresses`() {
        // Arrange
        val mockLegacyAddress: LegacyAddress = mock()
        val addresses = listOf(mockLegacyAddress)
        whenever(mockPayloadManager.payload.legacyAddressList).thenReturn(addresses)
        // Act
        val result = subject.legacyAddresses
        // Assert
        verify(mockPayloadManager, atLeastOnce()).payload
        result shouldEqual addresses
    }

    @Test
    @Throws(Exception::class)
    fun `getLegacyAddresses returns empty list`() {
        // Arrange
        whenever(mockPayloadManager.payload).thenReturn(null)
        // Act
        val result = subject.legacyAddresses
        // Assert
        verify(mockPayloadManager).payload
        result shouldEqual emptyList()
    }

    @Test
    @Throws(Exception::class)
    fun getAddressBalance() {
        // Arrange
        val address = "ADDRESS"
        val balance = BigInteger.TEN
        whenever(mockPayloadManager.getAddressBalance(address))
                .thenReturn(balance)
        // Act
        val result = subject.getAddressBalance(address)
        // Assert
        verify(mockPayloadManager).getAddressBalance(address)
        verifyNoMoreInteractions(mockPayloadManager)
        result shouldEqual balance
    }

    @Test
    @Throws(Exception::class)
    fun getReceiveAddressAtPosition() {
        // Arrange
        val mockAccount: Account = mock()
        val position = 1337
        val address = "ADDRESS"
        whenever(mockPayloadManager.getReceiveAddressAtPosition(mockAccount, position))
                .thenReturn(address)
        // Act
        val result = subject.getReceiveAddressAtPosition(mockAccount, position)
        // Assert
        verify(mockPayloadManager).getReceiveAddressAtPosition(mockAccount, position)
        verifyNoMoreInteractions(mockPayloadManager)
        result shouldEqual address
    }

    @Test
    @Throws(Exception::class)
    fun getReceiveAddressAtArbitraryPosition() {
        // Arrange
        val mockAccount: Account = mock()
        val position = 1337
        val address = "ADDRESS"
        whenever(mockPayloadManager.getReceiveAddressAtArbitraryPosition(mockAccount, position))
                .thenReturn(address)
        // Act
        val result = subject.getReceiveAddressAtArbitraryPosition(mockAccount, position)
        // Assert
        verify(mockPayloadManager).getReceiveAddressAtArbitraryPosition(mockAccount, position)
        verifyNoMoreInteractions(mockPayloadManager)
        result shouldEqual address
    }

    @Test
    @Throws(Exception::class)
    fun subtractAmountFromAddressBalance() {
        // Arrange
        val address = "ADDRESS"
        val amount = 1_000_000L
        // Act
        subject.subtractAmountFromAddressBalance(address, amount)
        // Assert
        verify(mockPayloadManager).subtractAmountFromAddressBalance(address, BigInteger.valueOf(amount))
        verifyNoMoreInteractions(mockPayloadManager)
    }

    @Test
    @Throws(Exception::class)
    fun incrementReceiveAddress() {
        // Arrange
        val mockAccount: Account = mock()
        // Act
        subject.incrementReceiveAddress(mockAccount)
        // Assert
        verify(mockPayloadManager).incrementNextReceiveAddress(mockAccount)
        verifyNoMoreInteractions(mockPayloadManager)
    }

    @Test
    @Throws(Exception::class)
    fun incrementChangeAddress() {
        // Arrange
        val mockAccount: Account = mock()
        // Act
        subject.incrementChangeAddress(mockAccount)
        // Assert
        verify(mockPayloadManager).incrementNextChangeAddress(mockAccount)
        verifyNoMoreInteractions(mockPayloadManager)
    }

    @Test
    @Throws(Exception::class)
    fun getXpubFromAddress() {
        // Arrange
        val xPub = "X_PUB"
        val address = "ADDRESS"
        whenever(mockPayloadManager.getXpubFromAddress(address))
                .thenReturn(xPub)
        // Act
        val result = subject.getXpubFromAddress(address)
        // Assert
        verify(mockPayloadManager).getXpubFromAddress(address)
        verifyNoMoreInteractions(mockPayloadManager)
        result shouldEqual xPub
    }

    @Test
    @Throws(Exception::class)
    fun getXpubFromIndex() {
        // Arrange
        val xPub = "X_PUB"
        val index = 42
        whenever(mockPayloadManager.getXpubFromAccountIndex(index))
                .thenReturn(xPub)
        // Act
        val result = subject.getXpubFromIndex(index)
        // Assert
        verify(mockPayloadManager).getXpubFromAccountIndex(index)
        verifyNoMoreInteractions(mockPayloadManager)
        result shouldEqual xPub
    }

    @Test
    @Throws(Exception::class)
    fun isOwnHDAddress() {
        // Arrange
        val address = "ADDRESS"
        whenever(mockPayloadManager.isOwnHDAddress(address)).thenReturn(true)
        // Act
        val result = subject.isOwnHDAddress(address)
        // Assert
        result shouldEqual true
    }

    @Test
    @Throws(Exception::class)
    fun loadNodes() {
        // Arrange
        whenever(mockPayloadService.loadNodes()).thenReturn(Observable.just(true))
        // Act
        val testObserver = subject.loadNodes().test()
        // Assert
        verify(mockPayloadService).loadNodes()
        verifyNoMoreInteractions(mockPayloadService)
        testObserver.assertComplete()
        testObserver.assertValue(true)
    }

    @Test
    @Throws(Exception::class)
    fun generateNodes() {
        // Arrange
        val secondPassword = "SECOND_PASSWORD"
        whenever(mockPayloadService.generateNodes(secondPassword))
                .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.generateNodes(secondPassword).test()
        // Assert
        verify(mockPayloadService).generateNodes(secondPassword)
        verifyNoMoreInteractions(mockPayloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun getMetadataNodeFactory() {
        // Arrange
        val mockNodeFactory: MetadataNodeFactory = mock()
        whenever(mockPayloadManager.metadataNodeFactory).thenReturn(mockNodeFactory)
        // Act
        val testObserver = subject.metadataNodeFactory.test()
        // Assert
        verify(mockPayloadManager).metadataNodeFactory
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
        testObserver.assertValue(mockNodeFactory)
    }

    @Test
    @Throws(Exception::class)
    fun registerMdid() {
        // Arrange
        val responseBody = ResponseBody.create(MediaType.parse("application/json"), "{}")
        whenever(mockPayloadService.registerMdid()).thenReturn(Observable.just(responseBody))
        // Act
        val testObserver = subject.registerMdid().test()
        // Assert
        verify(mockPayloadService).registerMdid()
        verifyNoMoreInteractions(mockPayloadService)
        testObserver.assertComplete()
        testObserver.assertValue(responseBody)
    }

    @Test
    @Throws(Exception::class)
    fun unregisterMdid() {
        // Arrange
        val responseBody = ResponseBody.create(MediaType.parse("application/json"), "{}")
        whenever(mockPayloadService.unregisterMdid()).thenReturn(Observable.just(responseBody))
        // Act
        val testObserver = subject.unregisterMdid().test()
        // Assert
        verify(mockPayloadService).unregisterMdid()
        verifyNoMoreInteractions(mockPayloadService)
        testObserver.assertComplete()
        testObserver.assertValue(responseBody)
    }

    @Test
    @Throws(Exception::class)
    fun `getWallet returns wallet`() {
        // Arrange
        val mockWallet: Wallet = mock()
        whenever(mockPayloadManager.payload).thenReturn(mockWallet)
        // Act
        val result = subject.wallet
        // Assert
        verify(mockPayloadManager).payload
        verifyNoMoreInteractions(mockPayloadManager)
        result shouldEqual mockWallet
    }

    @Test
    @Throws(Exception::class)
    fun `getWallet returns null`() {
        // Arrange
        whenever(mockPayloadManager.payload).thenReturn(null)
        // Act
        val result = subject.wallet
        // Assert
        verify(mockPayloadManager).payload
        verifyNoMoreInteractions(mockPayloadManager)
        result shouldEqual null
    }

    @Test
    @Throws(Exception::class)
    fun getDefaultAccountIndex() {
        // Arrange
        val index = 42
        whenever(mockPayloadManager.payload.hdWallets.first().defaultAccountIdx).thenReturn(index)
        // Act
        val result = subject.defaultAccountIndex
        // Assert
        verify(mockPayloadManager, atLeastOnce()).payload
        result shouldEqual index
    }

    @Test
    @Throws(Exception::class)
    fun getDefaultAccount() {
        // Arrange
        val index = 42
        val mockAccount: Account = mock()
        whenever(mockPayloadManager.payload.hdWallets.first().defaultAccountIdx)
                .thenReturn(index)
        whenever(mockPayloadManager.payload.hdWallets.first().getAccount(index))
                .thenReturn(mockAccount)
        // Act
        val result = subject.defaultAccount
        // Assert
        verify(mockPayloadManager, atLeastOnce()).payload
        result shouldEqual mockAccount
    }

    @Test
    @Throws(Exception::class)
    fun getAccount() {
        // Arrange
        val index = 42
        val mockAccount: Account = mock()
        whenever(mockPayloadManager.payload.hdWallets.first().getAccount(index))
                .thenReturn(mockAccount)
        // Act
        val result = subject.getAccount(index)
        // Assert
        verify(mockPayloadManager, atLeastOnce()).payload
        result shouldEqual mockAccount
    }

    @Test
    fun getTransactionNotes() {
        // Arrange
        val txHash = "TX_HASH"
        val note = "NOTES"
        val map = mapOf(txHash to note)
        whenever(mockPayloadManager.payload.txNotes).thenReturn(map)
        // Act
        val result = subject.getTransactionNotes(txHash)
        // Assert
        verify(mockPayloadManager, atLeastOnce()).payload
        result `should equal` note
    }

    @Test
    @Throws(Exception::class)
    fun getHDKeysForSigning() {
        // Arrange
        val mockAccount: Account = mock()
        val mockOutputs: SpendableUnspentOutputs = mock()
        val mockEcKey: ECKey = mock()
        whenever(mockPayloadManager.payload.hdWallets.first().getHDKeysForSigning(mockAccount, mockOutputs))
                .thenReturn(listOf(mockEcKey))
        // Act
        val result = subject.getHDKeysForSigning(mockAccount, mockOutputs)
        // Assert
        verify(mockPayloadManager, atLeastOnce()).payload
        result shouldEqual listOf(mockEcKey)
    }

    @Test
    @Throws(Exception::class)
    fun getPayloadChecksum() {
        // Arrange
        val checkSum = "CHECKSUM"
        whenever(mockPayloadManager.payloadChecksum).thenReturn(checkSum)
        // Act
        val result = subject.payloadChecksum
        // Assert
        verify(mockPayloadManager).payloadChecksum
        verifyNoMoreInteractions(mockPayloadManager)
        result shouldEqual checkSum
    }

    @Test
    @Throws(Exception::class)
    fun getTempPassword() {
        // Arrange
        val tempPassword = "TEMP_PASSWORD"
        whenever(mockPayloadManager.tempPassword).thenReturn(tempPassword)
        // Act
        val result = subject.tempPassword
        // Assert
        verify(mockPayloadManager).tempPassword
        verifyNoMoreInteractions(mockPayloadManager)
        result shouldEqual tempPassword
    }

    @Test
    @Throws(Exception::class)
    fun setTempPassword() {
        // Arrange
        val tempPassword = "TEMP_PASSWORD"
        // Act
        subject.tempPassword = tempPassword
        // Assert
        verify(mockPayloadManager).tempPassword = tempPassword
        verifyNoMoreInteractions(mockPayloadManager)
    }

    @Test
    @Throws(Exception::class)
    fun getImportedAddressesBalance() {
        // Arrange
        val balance = BigInteger.TEN
        whenever(mockPayloadManager.importedAddressesBalance).thenReturn(balance)
        // Act
        val result = subject.importedAddressesBalance
        // Assert
        verify(mockPayloadManager).importedAddressesBalance
        verifyNoMoreInteractions(mockPayloadManager)
        result shouldEqual balance
    }

    @Test
    @Throws(Exception::class)
    fun isDoubleEncrypted() {
        // Arrange
        whenever(mockPayloadManager.payload.isDoubleEncryption).thenReturn(true)
        // Act
        val result = subject.isDoubleEncrypted
        // Assert
        result shouldEqual true
    }

    @Test
    @Throws(Exception::class)
    fun getPositionOfAccountFromActiveList() {
        // Arrange
        val index = 1
        val account0 = Account().apply { isArchived = true }
        val account1 = Account()
        val account2 = Account().apply { isArchived = true }
        val account3 = Account()
        whenever(mockPayloadManager.payload.hdWallets.first().accounts)
                .thenReturn(listOf(account0, account1, account2, account3))
        // Act
        val result = subject.getPositionOfAccountFromActiveList(index)
        // Assert
        result shouldEqual 3
    }

    @Test
    @Throws(Exception::class)
    fun getPositionOfAccountInActiveList() {
        // Arrange
        val index = 3
        val account0 = Account().apply { isArchived = true }
        val account1 = Account()
        val account2 = Account().apply { isArchived = true }
        val account3 = Account()
        whenever(mockPayloadManager.payload.hdWallets.first().accounts)
                .thenReturn(listOf(account0, account1, account2, account3))
        // Act
        val result = subject.getPositionOfAccountInActiveList(index)
        // Assert
        result shouldEqual 1
    }

}