package piuk.blockchain.android.ui.receive

import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.ethereum.EthereumAccount
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.AddressBook
import info.blockchain.wallet.payload.data.LegacyAddress
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.ethereum.models.CombinedEthModel
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import java.math.BigInteger

class WalletAccountHelperTest {

    private lateinit var subject: WalletAccountHelper
    private val payloadManager: PayloadManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val stringUtils: StringUtils = mock()
    private val prefsUtil: PrefsUtil = mock()
    private val exchangeRateFactory: ExchangeRateFactory = mock()
    private val currencyState: CurrencyState = mock()
    private val ethDataManager: EthDataManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)

    @Before
    fun setUp() {
        subject = WalletAccountHelper(
                payloadManager,
                stringUtils,
                prefsUtil,
                exchangeRateFactory,
                currencyState,
                ethDataManager
        )
    }

    @Test
    @Throws(Exception::class)
    fun `getAccountItems should return one Account and one LegacyAddress`() {
        // Arrange
        val label = "LABEL"
        val xPub = "X_PUB"
        val address = "ADDRESS"
        val account = Account().apply {
            this.label = label
            this.xpub = xPub
        }
        val legacyAddress = LegacyAddress().apply {
            this.label = null
            this.address = address
        }
        whenever(payloadManager.payload.hdWallets[0].accounts).thenReturn(listOf(account))
        whenever(payloadManager.payload.legacyAddressList).thenReturn(mutableListOf(legacyAddress))
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("GBP")
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        // Act
        val result = subject.getAccountItems()
        // Assert
        verify(payloadManager, atLeastOnce()).payload
        verify(prefsUtil).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verifyNoMoreInteractions(prefsUtil)
        result.size `should be` 2
        result[0].accountObject `should equal` account
        result[1].accountObject `should equal` legacyAddress
    }

    @Test
    @Throws(Exception::class)
    fun `getHdAccounts should return single Account`() {
        // Arrange
        val label = "LABEL"
        val xPub = "X_PUB"
        val archivedAccount = Account().apply { isArchived = true }
        val account = Account().apply {
            this.label = label
            this.xpub = xPub
        }
        whenever(payloadManager.payload.hdWallets[0].accounts)
                .thenReturn(mutableListOf(archivedAccount, account))
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("GBP")
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        // Act
        val result = subject.getAccountItems()
        // Assert
        verify(payloadManager, atLeastOnce()).payload
        verify(prefsUtil).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verifyNoMoreInteractions(prefsUtil)
        result.size `should equal` 1
        result[0].accountObject `should be` account
    }

    @Test
    @Throws(Exception::class)
    fun `getAccountItems when currency is ETH should return one account`() {
        // Arrange
        val ethAccount: EthereumAccount = mock()
        val combinedEthModel: CombinedEthModel = mock()
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.ETHER)
        whenever(ethDataManager.getEthWallet()?.account).thenReturn(ethAccount)
        whenever(ethDataManager.getEthResponseModel()).thenReturn(combinedEthModel)
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(1234567890L))
        // Act
        val result = subject.getAccountItems()
        // Assert
        verify(ethDataManager, atLeastOnce()).getEthWallet()
        result.size `should be` 1
        result[0].accountObject `should equal` ethAccount
    }

    @Test
    @Throws(Exception::class)
    fun `getLegacyAddresses should return single LegacyAddress`() {
        // Arrange
        val address = "ADDRESS"
        val archivedAddress = LegacyAddress().apply { tag = LegacyAddress.ARCHIVED_ADDRESS }
        val legacyAddress = LegacyAddress().apply {
            this.label = null
            this.address = address
        }
        whenever(payloadManager.payload.legacyAddressList)
                .thenReturn(mutableListOf(archivedAddress, legacyAddress))
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("GBP")
        // Act
        val result = subject.getLegacyAddresses()
        // Assert
        verify(payloadManager, atLeastOnce()).payload
        verify(prefsUtil).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verifyNoMoreInteractions(prefsUtil)
        result.size `should equal` 1
        result[0].accountObject `should be` legacyAddress
    }

    @Test
    @Throws(Exception::class)
    fun `getAddressBookEntries should return single item`() {
        // Arrange
        val addressBook = AddressBook()
        whenever(payloadManager.payload.addressBook).thenReturn(listOf(addressBook))
        // Act
        val result = subject.getAddressBookEntries()
        // Assert
        result.size `should equal` 1
    }

    @Test
    @Throws(Exception::class)
    fun `getAddressBookEntries should return empty list`() {
        // Arrange
        whenever(payloadManager.payload.addressBook)
                .thenReturn(null)
        // Act
        val result = subject.getAddressBookEntries()
        // Assert
        result.size `should equal` 0
    }

    @Test
    @Throws(Exception::class)
    fun `getDefaultAccount should return ETH account`() {
        // Arrange
        val ethAccount: EthereumAccount = mock()
        val combinedEthModel: CombinedEthModel = mock()
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.ETHER)
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.ETHER)
        whenever(ethDataManager.getEthWallet()?.account).thenReturn(ethAccount)
        whenever(ethDataManager.getEthResponseModel()).thenReturn(combinedEthModel)
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(1234567890L))
        // Act
        val result = subject.getDefaultAccount()
        // Assert
        verify(ethDataManager, atLeastOnce()).getEthWallet()
        result.accountObject `should equal` ethAccount
    }

    @Test
    @Throws(Exception::class)
    fun `getDefaultAccount should return BTC account`() {
        // Arrange
        val btcAccount: Account = mock()
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        whenever(payloadManager.payload.hdWallets[0].defaultAccountIdx).thenReturn(0)
        whenever(payloadManager.payload.hdWallets[0].accounts[0]).thenReturn(btcAccount)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("GBP")
        // Act
        val result = subject.getDefaultAccount()
        // Assert
        verify(payloadManager, atLeastOnce()).payload
        verify(prefsUtil).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verifyNoMoreInteractions(prefsUtil)
        result.accountObject `should equal` btcAccount
    }

}