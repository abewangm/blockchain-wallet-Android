package piuk.blockchain.android.ui.receive

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.BlockchainFramework
import info.blockchain.wallet.FrameworkInterface
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.ethereum.data.EthAddressResponse
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Completable
import io.reactivex.Observable
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.apache.commons.lang3.NotImplementedException
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.bitcoinj.params.BitcoinMainNetParams
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.api.EnvironmentSettings
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager
import piuk.blockchain.android.data.ethereum.EthDataStore
import piuk.blockchain.android.data.ethereum.models.CombinedEthModel
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import retrofit2.Retrofit
import java.util.*

class ReceivePresenterTest {

    private lateinit var subject: ReceivePresenter
    private val payloadDataManager: PayloadDataManager = mock(defaultAnswer = RETURNS_DEEP_STUBS)
    private val prefsUtil: PrefsUtil = mock()
    private val qrCodeDataManager: QrCodeDataManager = mock()
    private val exchangeRateFactory: ExchangeRateFactory = mock()
    private val walletAccountHelper: WalletAccountHelper = mock()
    private val activity: ReceiveView = mock()
    private val ethDataStore: EthDataStore = mock()
    private val bchDataManager: BchDataManager = mock()
    private val environmentSettings: EnvironmentSettings = mock()
    private val currencyState: CurrencyState = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        initFramework()

        subject = ReceivePresenter(
                prefsUtil,
                qrCodeDataManager,
                walletAccountHelper,
                payloadDataManager,
                exchangeRateFactory,
                ethDataStore,
                bchDataManager,
                environmentSettings,
                currencyState
        )
        subject.initView(activity)
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady hide contacts introduction`() {
        // Arrange
        whenever(activity.isContactsEnabled).thenReturn(true)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_CONTACTS_INTRODUCTION_COMPLETE, false))
                .thenReturn(true)
        // Act
        subject.onViewReady()
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_CONTACTS_INTRODUCTION_COMPLETE, false)
        verifyNoMoreInteractions(prefsUtil)
        verify(activity).isContactsEnabled
        verify(activity).hideContactsIntroduction()
        verifyNoMoreInteractions(activity)
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady show contacts introduction`() {
        // Arrange
        whenever(activity.isContactsEnabled).thenReturn(true)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_CONTACTS_INTRODUCTION_COMPLETE, false))
                .thenReturn(false)
        // Act
        subject.onViewReady()
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_CONTACTS_INTRODUCTION_COMPLETE, false)
        verifyNoMoreInteractions(prefsUtil)
        verify(activity).isContactsEnabled
        verify(activity).showContactsIntroduction()
        verifyNoMoreInteractions(activity)
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady don't show contacts`() {
        // Arrange
        whenever(activity.isContactsEnabled).thenReturn(false)
        // Act
        subject.onViewReady()
        // Assert
        verifyZeroInteractions(prefsUtil)
        verify(activity).isContactsEnabled
        verify(activity).hideContactsIntroduction()
        verifyNoMoreInteractions(activity)
    }

    @Test
    @Throws(Exception::class)
    fun onSendToContactClicked() {
        // Arrange

        // Act
        subject.onSendToContactClicked()
        // Assert
        verify(activity).startContactSelectionActivity()
        verifyZeroInteractions(activity)
    }

    @Test
    @Throws(Exception::class)
    fun isValidAmount() {
        // Arrange
        val amount = "-1"
        // Act
        val result = subject.isValidAmount(amount)
        // Assert
        result `should be` false
    }

    @Test
    @Throws(Exception::class)
    fun shouldShowDropdown() {
        // Arrange
        whenever(walletAccountHelper.getAccountItems()).thenReturn(listOf(mock(), mock()))
        whenever(walletAccountHelper.getAddressBookEntries()).thenReturn(listOf(mock(), mock()))
        // Act
        val result = subject.shouldShowDropdown()
        // Assert
        verify(walletAccountHelper).getAccountItems()
        verify(walletAccountHelper).getAddressBookEntries()
        verifyNoMoreInteractions(walletAccountHelper)
        result `should be` true
    }

    @Test
    @Throws(Exception::class)
    fun `onLegacyAddressSelected no label`() {
        // Arrange
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        val legacyAddress = LegacyAddress().apply { this.address = address }
        whenever(activity.getBtcAmount()).thenReturn("0")
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
                .thenReturn(Observable.empty())
        // Act
        subject.onLegacyAddressSelected(legacyAddress)
        // Assert
        verify(activity).getBtcAmount()
        verify(activity).updateReceiveAddress(address)
        verify(activity).updateReceiveLabel(address)
        verify(activity).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        subject.selectedAccount `should be` null
        subject.selectedAddress `should be` address
        subject.selectedBchAccount `should be` null
    }

    @Test
    @Throws(Exception::class)
    fun `onLegacyAddressSelected with label`() {
        // Arrange
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        val label = "LABEL"
        val legacyAddress = LegacyAddress().apply {
            this.address = address
            this.label = label
        }
        whenever(activity.getBtcAmount()).thenReturn("0")
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
                .thenReturn(Observable.empty())
        // Act
        subject.onLegacyAddressSelected(legacyAddress)
        // Assert
        verify(activity).getBtcAmount()
        verify(activity).updateReceiveAddress(address)
        verify(activity).updateReceiveLabel(label)
        verify(activity).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        subject.selectedAccount `should be` null
        subject.selectedAddress `should be` address
        subject.selectedBchAccount `should be` null
    }

    @Test
    @Throws(Exception::class)
    fun `onLegacyAddressSelected BCH with no label`() {
        // Arrange
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        val bech32Address = "bitcoincash:qpna9wa3akewwj4umm0asx6jnt70hrdxpycrd7gy6u"
        val bech32Display = "qpna9wa3akewwj4umm0asx6jnt70hrdxpycrd7gy6u"
        val legacyAddress = LegacyAddress().apply {
            this.address = address
        }
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
                .thenReturn(Observable.empty())
        whenever(environmentSettings.bitcoinNetworkParameters).thenReturn(BitcoinMainNetParams.get())
        // Act
        subject.onLegacyBchAddressSelected(legacyAddress)
        // Assert
        verify(activity).updateReceiveAddress(bech32Display)
        verify(activity).updateReceiveLabel(bech32Display)
        verify(activity).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(eq(bech32Address), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        subject.selectedAccount `should be` null
        subject.selectedAddress!! `should equal to` bech32Address
        subject.selectedBchAccount `should be` null
    }

    @Test
    @Throws(Exception::class)
    fun `onLegacyAddressSelected BCH with label`() {
        // Arrange
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        val bech32Address = "bitcoincash:qpna9wa3akewwj4umm0asx6jnt70hrdxpycrd7gy6u"
        val bech32Display = "qpna9wa3akewwj4umm0asx6jnt70hrdxpycrd7gy6u"
        val label = "BCH LABEL"
        val legacyAddress = LegacyAddress().apply {
            this.address = address
            this.label = label
        }
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
                .thenReturn(Observable.empty())
        whenever(environmentSettings.bitcoinNetworkParameters).thenReturn(BitcoinMainNetParams.get())
        // Act
        subject.onLegacyBchAddressSelected(legacyAddress)
        // Assert
        verify(activity).updateReceiveAddress(bech32Display)
        verify(activity).updateReceiveLabel(label)
        verify(activity).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(eq(bech32Address), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        subject.selectedAccount `should be` null
        subject.selectedAddress `should equal` bech32Address
        subject.selectedBchAccount `should be` null
    }

    @Test
    @Throws(Exception::class)
    fun `onAccountSelected success`() {
        // Arrange
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        val label = "LABEL"
        val account = Account().apply { this.label = label }
        whenever(activity.getBtcAmount()).thenReturn("0")
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
                .thenReturn(Observable.empty())
        whenever(payloadDataManager.getNextReceiveAddress(account))
                .thenReturn(Observable.just(address))
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        // Act
        subject.onAccountSelected(account)
        // Assert
        verify(activity).setSelectedCurrency(CryptoCurrencies.BTC)
        verify(activity).getBtcAmount()
        verify(activity).updateReceiveAddress(address)
        verify(activity).updateReceiveLabel(label)
        verify(activity, times(2)).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(payloadDataManager).getNextReceiveAddress(account)
        verify(payloadDataManager).updateAllTransactions()
        verifyNoMoreInteractions(payloadDataManager)
        verify(currencyState).cryptoCurrency = CryptoCurrencies.BTC
        verify(currencyState).cryptoCurrency
        verifyNoMoreInteractions(currencyState)
        subject.selectedAccount `should be` account
        subject.selectedAddress `should be` address
        subject.selectedBchAccount `should be` null
    }

    @Test
    @Throws(Exception::class)
    fun `onAccountSelected address derivation failure`() {
        // Arrange
        val label = "LABEL"
        val account = Account().apply { this.label = label }
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        whenever(payloadDataManager.getNextReceiveAddress(account))
                .thenReturn(Observable.error { Throwable() })
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        // Act
        subject.onAccountSelected(account)
        // Assert
        verify(activity).setSelectedCurrency(CryptoCurrencies.BTC)
        verify(activity).showQrLoading()
        verify(activity).updateReceiveLabel(label)
        verify(activity).showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
        verifyNoMoreInteractions(activity)
        verify(payloadDataManager).updateAllTransactions()
        verify(payloadDataManager).getNextReceiveAddress(account)
        verifyNoMoreInteractions(payloadDataManager)
        verify(currencyState).cryptoCurrency = CryptoCurrencies.BTC
        verify(currencyState).cryptoCurrency
        verifyNoMoreInteractions(currencyState)
        subject.selectedAccount `should be` account
        subject.selectedAddress `should be` null
        subject.selectedBchAccount `should be` null
    }

    @Test
    @Throws(Exception::class)
    fun onEthSelected() {
        // Arrange
        val ethAccount = "0x879dBFdE84B0239feB355f55F81fb29f898C778C"
        val combinedEthModel: CombinedEthModel = mock()
        val ethResponse: EthAddressResponse = mock()
        whenever(ethDataStore.ethAddressResponse).thenReturn(combinedEthModel)
        whenever(combinedEthModel.getAddressResponse()).thenReturn(ethResponse)
        whenever(ethResponse.account).thenReturn(ethAccount)
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
                .thenReturn(Observable.empty())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.ETHER)
        // Act
        subject.onEthSelected()
        // Assert
        verify(activity).setSelectedCurrency(CryptoCurrencies.ETHER)
        verify(activity).updateReceiveAddress(ethAccount)
        verify(activity).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(currencyState).cryptoCurrency = CryptoCurrencies.ETHER
        verify(currencyState).cryptoCurrency
        verifyNoMoreInteractions(currencyState)
        subject.selectedAccount `should be` null
        subject.selectedAddress `should be` ethAccount
        subject.selectedBchAccount `should be` null
    }

    @Test
    @Throws(Exception::class)
    fun `onBchAccountSelected success`() {
        // Arrange
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        val bech32Address = "bitcoincash:qpna9wa3akewwj4umm0asx6jnt70hrdxpycrd7gy6u"
        val bech32Display = "qpna9wa3akewwj4umm0asx6jnt70hrdxpycrd7gy6u"
        val xPub = "X_PUB"
        val label = "LABEL"
        val account = GenericMetadataAccount().apply {
            this.label = label
            this.xpub = xPub
        }
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
                .thenReturn(Observable.empty())
        whenever(bchDataManager.updateAllBalances())
                .thenReturn(Completable.complete())
        whenever(bchDataManager.getActiveAccounts())
                .thenReturn(listOf(account))
        whenever(bchDataManager.getNextReceiveAddress(0))
                .thenReturn(Observable.just(address))
        whenever(bchDataManager.getWalletTransactions(50, 0))
                .thenReturn(Observable.just(emptyList()))
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BCH)
        // Act
        subject.onBchAccountSelected(account)
        // Assert
        verify(activity).setSelectedCurrency(CryptoCurrencies.BCH)
        verify(activity).updateReceiveAddress(bech32Display)
        verify(activity).updateReceiveLabel(label)
        verify(activity, times(2)).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(eq(bech32Address), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(bchDataManager).updateAllBalances()
        verify(bchDataManager).getActiveAccounts()
        verify(bchDataManager).getNextReceiveAddress(0)
        verify(bchDataManager).getWalletTransactions(50, 0)
        verifyNoMoreInteractions(payloadDataManager)
        verify(currencyState).cryptoCurrency = CryptoCurrencies.BCH
        verify(currencyState).cryptoCurrency
        verifyNoMoreInteractions(currencyState)
        subject.selectedAccount `should be` null
        subject.selectedAddress `should equal` bech32Address
        subject.selectedBchAccount `should be` account
    }

    @Test
    @Throws(Exception::class)
    fun `onSelectBchDefault success`() {
        // Arrange
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        val bech32Address = "bitcoincash:qpna9wa3akewwj4umm0asx6jnt70hrdxpycrd7gy6u"
        val bech32Display = "qpna9wa3akewwj4umm0asx6jnt70hrdxpycrd7gy6u"
        val xPub = "X_PUB"
        val label = "LABEL"
        val account = GenericMetadataAccount().apply {
            this.label = label
            this.xpub = xPub
        }
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
                .thenReturn(Observable.empty())
        whenever(bchDataManager.getDefaultGenericMetadataAccount()).thenReturn(account)
        whenever(bchDataManager.updateAllBalances())
                .thenReturn(Completable.complete())
        whenever(bchDataManager.getActiveAccounts())
                .thenReturn(listOf(account))
        whenever(bchDataManager.getNextReceiveAddress(0))
                .thenReturn(Observable.just(address))
        whenever(bchDataManager.getWalletTransactions(50, 0))
                .thenReturn(Observable.just(emptyList()))
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BCH)
        // Act
        subject.onSelectBchDefault()
        // Assert
        verify(activity).setSelectedCurrency(CryptoCurrencies.BCH)
        verify(activity).updateReceiveAddress(bech32Display)
        verify(activity).updateReceiveLabel(label)
        verify(activity, times(2)).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(eq(bech32Address), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(bchDataManager).getDefaultGenericMetadataAccount()
        verify(bchDataManager).updateAllBalances()
        verify(bchDataManager).getActiveAccounts()
        verify(bchDataManager).getNextReceiveAddress(0)
        verify(bchDataManager).getWalletTransactions(50, 0)
        verifyNoMoreInteractions(payloadDataManager)
        verify(currencyState).cryptoCurrency = CryptoCurrencies.BCH
        verify(currencyState).cryptoCurrency
        verifyNoMoreInteractions(currencyState)
        subject.selectedAccount `should be` null
        subject.selectedAddress `should equal` bech32Address
        subject.selectedBchAccount `should be` account
    }

    @Test
    @Throws(Exception::class)
    fun `onSelectDefault account valid account position`() {
        val accountPosition = 2
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        val label = "LABEL"
        val account = Account().apply { this.label = label }
        whenever(payloadDataManager.getAccount(accountPosition))
                .thenReturn(account)
        whenever(activity.getBtcAmount()).thenReturn("0")
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
                .thenReturn(Observable.empty())
        whenever(payloadDataManager.getNextReceiveAddress(account))
                .thenReturn(Observable.just(address))
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        // Act
        subject.onSelectDefault(accountPosition)
        // Assert
        verify(activity).setSelectedCurrency(CryptoCurrencies.BTC)
        verify(activity).getBtcAmount()
        verify(activity).updateReceiveAddress(address)
        verify(activity).updateReceiveLabel(label)
        verify(activity, times(2)).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(payloadDataManager).getNextReceiveAddress(account)
        verify(payloadDataManager).getAccount(accountPosition)
        verify(payloadDataManager).updateAllTransactions()
        verifyNoMoreInteractions(payloadDataManager)
        verify(currencyState).cryptoCurrency = CryptoCurrencies.BTC
        verify(currencyState).cryptoCurrency
        verifyNoMoreInteractions(currencyState)
        subject.selectedAccount `should be` account
        subject.selectedAddress `should be` address
        subject.selectedBchAccount `should be` null
    }

    @Test
    @Throws(Exception::class)
    fun `onSelectDefault account invalid account position`() {
        val accountPosition = -1
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        val label = "LABEL"
        val account = Account().apply { this.label = label }
        whenever(payloadDataManager.defaultAccount)
                .thenReturn(account)
        whenever(activity.getBtcAmount()).thenReturn("0")
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
                .thenReturn(Observable.empty())
        whenever(payloadDataManager.getNextReceiveAddress(account))
                .thenReturn(Observable.just(address))
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        // Act
        subject.onSelectDefault(accountPosition)
        // Assert
        verify(activity).setSelectedCurrency(CryptoCurrencies.BTC)
        verify(activity).getBtcAmount()
        verify(activity).updateReceiveAddress(address)
        verify(activity).updateReceiveLabel(label)
        verify(activity, times(2)).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(payloadDataManager).getNextReceiveAddress(account)
        verify(payloadDataManager).updateAllTransactions()
        verify(payloadDataManager).defaultAccount
        verifyNoMoreInteractions(payloadDataManager)
        verify(currencyState).cryptoCurrency = CryptoCurrencies.BTC
        verify(currencyState).cryptoCurrency
        verifyNoMoreInteractions(currencyState)
        subject.selectedAccount `should be` account
        subject.selectedAddress `should be` address
        subject.selectedBchAccount `should be` null
    }

    @Test
    @Throws(Exception::class)
    fun onBitcoinAmountChanged() {
        // Arrange
        val amount = "2100000000000000"
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        subject.selectedAddress = address
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
                .thenReturn(Observable.empty())
        // Act
        subject.onBitcoinAmountChanged(amount)
        // Assert
        verify(activity).showQrLoading()
        verify(activity).showToast(R.string.invalid_amount, ToastCustom.TYPE_ERROR)
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
    }

    @Test
    @Throws(Exception::class)
    fun `getSelectedAccountPosition ETH`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        val xPub = "X_PUB"
        val account = Account().apply { xpub = xPub }
        subject.selectedAccount = account
        whenever(payloadDataManager.accounts).thenReturn(listOf(account))
        whenever(payloadDataManager.getPositionOfAccountInActiveList(0))
                .thenReturn(10)
        // Act
        val result = subject.getSelectedAccountPosition()
        // Assert
        result `should equal to` 10
    }

    @Test
    @Throws(Exception::class)
    fun `getSelectedAccountPosition BTC`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.ETHER)
        // Act
        val result = subject.getSelectedAccountPosition()
        // Assert
        result `should equal to` -1
    }

    @Test
    @Throws(Exception::class)
    fun setWarnWatchOnlySpend() {
        // Arrange

        // Act
        subject.setWarnWatchOnlySpend(true)
        // Assert
        verify(prefsUtil).setValue(ReceivePresenter.KEY_WARN_WATCH_ONLY_SPEND, true)
    }

    @Test
    @Throws(Exception::class)
    fun clearSelectedContactId() {
        // Arrange
        val contactId = "1337"
        subject.selectedContactId = contactId
        // Act
        subject.clearSelectedContactId()
        // Assert
        subject.selectedContactId `should be` null
    }

    @Test
    @Throws(Exception::class)
    fun getConfirmationDetails() {
        // Arrange
        val label = "LABEL"
        val xPub = "X_PUB"
        val account = Account().apply {
            this.label = label
            xpub = xPub
        }
        val contactName = "CONTACT_NAME"
        val accountPosition = 10
        subject.selectedAccount = account
        whenever(payloadDataManager.accounts).thenReturn(listOf(account))
        whenever(payloadDataManager.getPositionOfAccountInActiveList(0))
                .thenReturn(10)
        subject.selectedAccount = account
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        whenever(payloadDataManager.wallet.hdWallets[0].accounts.indexOf(account))
                .thenReturn(accountPosition)
        whenever(activity.getContactName())
                .thenReturn(contactName)
        whenever(payloadDataManager.getAccount(accountPosition))
                .thenReturn(account)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("GBP")
        whenever(exchangeRateFactory.getLastBtcPrice("GBP"))
                .thenReturn(3426.00)
        whenever(activity.getBtcAmount()).thenReturn("1.0")
        whenever(activity.locale).thenReturn(Locale.UK)
        // Act
        val result = subject.getConfirmationDetails()
        // Assert
        verify(activity).getContactName()
        verify(activity).getBtcAmount()
        verify(activity).locale
        verifyNoMoreInteractions(activity)
        verify(prefsUtil, times(2)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastBtcPrice("GBP")
        verifyNoMoreInteractions(exchangeRateFactory)
        result.fromLabel `should equal to` label
        result.toLabel `should equal to` contactName
        result.cryptoAmount `should equal to` "1.0"
        result.cryptoUnit `should equal to` "BTC"
        result.fiatUnit `should equal to` "GBP"
        result.fiatAmount `should equal to` "3,426.00"
        result.fiatSymbol `should equal to` "£"
    }

    @Test
    @Throws(Exception::class)
    fun `onShowBottomSheetSelected btc`() {
        // Arrange
        subject.selectedAddress = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        whenever(activity.getBtcAmount()).thenReturn("0")
        // Act
        subject.onShowBottomSheetSelected()
        // Assert
        verify(activity).getBtcAmount()
        verify(activity).showBottomSheet(anyString())
        verifyNoMoreInteractions(activity)
    }

    @Test
    @Throws(Exception::class)
    fun `onShowBottomSheetSelected eth`() {
        // Arrange
        subject.selectedAddress = "0x879dBFdE84B0239feB355f55F81fb29f898C778C"
        // Act
        subject.onShowBottomSheetSelected()
        // Assert
        verify(activity).showBottomSheet(anyString())
        verifyNoMoreInteractions(activity)
    }

    @Test(expected = IllegalStateException::class)
    @Throws(Exception::class)
    fun `onShowBottomSheetSelected unknown`() {
        // Arrange
        subject.selectedAddress = "I am not a valid address"
        // Act
        subject.onShowBottomSheetSelected()
        // Assert
        verifyZeroInteractions(activity)
    }

    @Test
    @Throws(Exception::class)
    fun updateFiatTextField() {
        // Arrange
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(0)
        whenever(
                prefsUtil.getValue(
                        PrefsUtil.KEY_SELECTED_FIAT,
                        PrefsUtil.DEFAULT_CURRENCY
                )
        ).thenReturn("GBP")
        whenever(exchangeRateFactory.getLastBtcPrice("GBP")).thenReturn(2.0)
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        // Act
        subject.updateFiatTextField("1.0")
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verify(prefsUtil, times(2)).getValue(
                PrefsUtil.KEY_SELECTED_FIAT,
                PrefsUtil.DEFAULT_CURRENCY
        )
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastBtcPrice("GBP")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(currencyState, times(2)).cryptoCurrency
        verifyNoMoreInteractions(currencyState)
        verify(activity).updateFiatTextField("2.00")
        verifyNoMoreInteractions(activity)
    }

    @Test
    @Throws(Exception::class)
    fun updateBtcTextField() {
        // Arrange
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(0)
        whenever(
                prefsUtil.getValue(
                        PrefsUtil.KEY_SELECTED_FIAT,
                        PrefsUtil.DEFAULT_CURRENCY
                )
        ).thenReturn("GBP")
        whenever(exchangeRateFactory.getLastBtcPrice("GBP")).thenReturn(2.0)
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        // Act
        subject.updateBtcTextField("1.0")
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastBtcPrice("GBP")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(currencyState).cryptoCurrency
        verifyNoMoreInteractions(currencyState)
        verify(activity).updateBtcTextField("0.5")
        verifyNoMoreInteractions(activity)
    }

    private fun initFramework() {
        BlockchainFramework.init(object : FrameworkInterface {
            override fun getRetrofitShapeShiftInstance(): Retrofit {
                throw NotImplementedException("Function should not be called")
            }

            override fun getDevice(): String {
                throw NotImplementedException("Function should not be called")
            }

            override fun getRetrofitExplorerInstance(): Retrofit {
                throw NotImplementedException("Function should not be called")
            }

            override fun getEnvironment(): Environment {
                throw NotImplementedException("Function should not be called")
            }

            override fun getRetrofitApiInstance(): Retrofit {
                throw NotImplementedException("Function should not be called")
            }

            override fun getApiCode(): String {
                throw NotImplementedException("Function should not be called")
            }

            override fun getAppVersion(): String {
                throw NotImplementedException("Function should not be called")
            }

            override fun getBitcoinParams(): NetworkParameters {
                return BitcoinMainNetParams.get()
            }

            override fun getBitcoinCashParams(): NetworkParameters {
                return BitcoinCashMainNetParams.get()
            }
        })
    }

}