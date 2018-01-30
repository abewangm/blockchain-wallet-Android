package piuk.blockchain.android.ui.balance

import com.nhaarman.mockito_kotlin.*
import io.reactivex.Completable
import io.reactivex.Observable
import org.amshove.kluent.`should equal to`
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.data.access.AccessState
import piuk.blockchain.android.data.access.AuthEvent
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.exchange.BuyDataManager
import piuk.blockchain.android.data.notifications.models.NotificationPayload
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.UiState
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils

@Suppress("IllegalIdentifier")
class BalancePresenterTest {

    private lateinit var subject: BalancePresenter
    private var view: BalanceView = mock()
    private var exchangeRateFactory: ExchangeRateFactory = mock()
    private var transactionListDataManager: TransactionListDataManager = mock()
    private var swipeToReceiveHelper: SwipeToReceiveHelper = mock()
    private var payloadDataManager: PayloadDataManager = mock()
    private var buyDataManager: BuyDataManager = mock()
    private var stringUtils: StringUtils = mock()
    private var prefsUtil: PrefsUtil = mock()
    private var accessState: AccessState = mock()
    private var currencyState: CurrencyState = mock()
    private var rxBus: RxBus = mock()
    private var ethDataManager: EthDataManager = mock()
    private var shapeShiftDataManager: ShapeShiftDataManager = mock()
    private val bchDataManager: BchDataManager = mock()
    private val walletAccountHelper: WalletAccountHelper = mock()

    @Before
    fun setUp() {

        subject = BalancePresenter(
                exchangeRateFactory,
                transactionListDataManager,
                ethDataManager,
                swipeToReceiveHelper,
                payloadDataManager,
                buyDataManager,
                stringUtils,
                prefsUtil,
                rxBus,
                currencyState,
                shapeShiftDataManager,
                bchDataManager,
                walletAccountHelper
        )
        subject.initView(view)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReady() {

        // Arrange
        val account: ItemAccount = mock()
        whenever(walletAccountHelper.getAccountItemsForOverview()).thenReturn(mutableListOf(account))
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)

        whenever(rxBus.register(NotificationPayload::class.java)).thenReturn(Observable.empty())
        whenever(rxBus.register(AuthEvent::class.java)).thenReturn(Observable.empty())

        // Act
        subject.onViewReady()

        // Assert
        verify(view).setupAccountsAdapter(mutableListOf(account))
        verify(view).setupTxFeedAdapter(true)
    }

    @Test
    @Throws(Exception::class)
    fun onViewDestroyed() {
        // Arrange
        val notificationObservable = Observable.just(NotificationPayload(emptyMap()))
        val authEventObservable = Observable.just(AuthEvent.LOGOUT)
        subject.notificationObservable = notificationObservable
        subject.authEventObservable = authEventObservable
        // Act
        subject.onViewDestroyed()
        // Assert
        verify(rxBus).unregister(NotificationPayload::class.java, notificationObservable)
        verify(rxBus).unregister(AuthEvent::class.java, authEventObservable)
    }

    @Test
    @Throws(Exception::class)
    fun onResume() {
        // Arrange
//        whenever(view.getCurrentAccountPosition()).thenReturn(0)
//        val account: ItemAccount = mock()
//        whenever(walletAccountHelper.getAccountItemsForOverview()).thenReturn(mutableListOf(account))
//        whenever(account.displayBalance).thenReturn("0.052 BTC")
//
//        whenever(exchangeRateFactory.updateTickers()).thenReturn(Observable.empty())
//        whenever(bchDataManager.refreshMetadataCompletable()).thenReturn(Completable.complete())
//        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.empty())
//        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
//        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
//        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
//        whenever(transactionListDataManager.fetchTransactions(any(), any(), any()))
//                .thenReturn(Observable.empty())
//
//        // Act
//        subject.onResume()
//
//        // Assert
//        verify(view).setUiState(UiState.LOADING)
//
//        //Tx
//        verify(swipeToReceiveHelper).updateAndStoreBitcoinAddresses()
//        verify(swipeToReceiveHelper).updateAndStoreBitcoinCashAddresses()
//
//        verify(view).updateBalanceHeader("0.052 BTC")
//        verify(view).updateAccountsDataSet(mutableListOf(account))
//        verify(view).generateLauncherShortcuts()
    }

    @Test
    @Throws(Exception::class)
    fun areLauncherShortcutsEnabled() {
        // Arrange
        whenever(prefsUtil.getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true))
                .thenReturn(false)
        // Act
        val result = subject.areLauncherShortcutsEnabled()
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true)
        verifyNoMoreInteractions(prefsUtil)
        result `should equal to` false
    }

    @Test
    @Throws(Exception::class)
    fun onRefreshRequested() {
        // Arrange

        //getCurrentAccount()
        whenever(view.getCurrentAccountPosition()).thenReturn(0)
        val account: ItemAccount = mock()
        whenever(walletAccountHelper.getAccountItemsForOverview()).thenReturn(mutableListOf(account))
        whenever(account.displayBalance).thenReturn("0.052 BTC")

        whenever(exchangeRateFactory.updateTickers()).thenReturn(Observable.empty())
        whenever(bchDataManager.refreshMetadataCompletable()).thenReturn(Completable.complete())
        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.empty())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(transactionListDataManager.fetchTransactions(any(), any(), any()))
                .thenReturn(Observable.empty())

        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)

        // Act
        subject.onRefreshRequested()

        // Assert
        verify(view).setUiState(UiState.LOADING)
        verify(view, times(2)).updateSelectedCurrency(CryptoCurrencies.BTC)
        verify(view, times(2)).updateBalanceHeader("0.052 BTC")
        verify(view, times(2)).updateAccountsDataSet(mutableListOf(account))
        verify(view).generateLauncherShortcuts()
        verify(view).updateTransactionValueType(true)

    }
}
