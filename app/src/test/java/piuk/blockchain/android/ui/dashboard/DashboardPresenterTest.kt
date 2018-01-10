package piuk.blockchain.android.ui.dashboard

import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Completable
import io.reactivex.Observable
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.data.charts.ChartsDataManager
import piuk.blockchain.android.data.charts.models.ChartDatumDto
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.ethereum.models.CombinedEthModel
import piuk.blockchain.android.data.exchange.BuyDataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.*
import java.math.BigInteger

class DashboardPresenterTest {

    private lateinit var subject: DashboardPresenter
    private val chartsDataManager: ChartsDataManager = mock()
    private val prefsUtil: PrefsUtil = mock()
    private val exchangeRateFactory: ExchangeRateFactory = mock()
    private val ethDataManager: EthDataManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val transactionListDataManager: TransactionListDataManager = mock()
    private val stringUtils: StringUtils = mock()
    private val appUtil: AppUtil = mock()
    private val buyDataManager: BuyDataManager = mock()
    private val rxBus: RxBus = mock()
    private val swipeToReceiveHelper: SwipeToReceiveHelper = mock()
    private val view: DashboardView = mock()
    private val walletOptionsDataManager: WalletOptionsDataManager = mock()

    @Before
    fun setUp() {

        subject = DashboardPresenter(
                chartsDataManager,
                prefsUtil,
                exchangeRateFactory,
                ethDataManager,
                payloadDataManager,
                transactionListDataManager,
                stringUtils,
                appUtil,
                buyDataManager,
                rxBus,
                swipeToReceiveHelper,
                walletOptionsDataManager
        )

        subject.initView(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady onboarding complete, no announcement`() {
        // Arrange
//        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
//        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false))
                .thenReturn(true)
        whenever(appUtil.isNewlyCreated).thenReturn(false)
        val combinedEthModel: CombinedEthModel = mock()
        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.just(combinedEthModel))
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        val btcBalance = 21_000_000_000L
        whenever(transactionListDataManager.getBtcBalance(any())).thenReturn(btcBalance)
        val ethBalance = 22_000_000_000L
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(ethBalance))
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(0)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastEthPrice("USD")).thenReturn(3.0)
        whenever(exchangeRateFactory.getSymbol("USD")).thenReturn("$")
        whenever(prefsUtil.getValue(DashboardPresenter.SHAPESHIFT_ANNOUNCEMENT_DISMISSED, false))
                .thenReturn(true)
        // Act
        subject.onViewReady()
        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        // TODO: Fix me
//        verify(view).updateBtcBalance("210.0 BTC")
//        verify(view).updateEthBalance("0.00000002 ETH")
//        verify(view).updateTotalBalance("\$420.00")
        verifyNoMoreInteractions(view)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, atLeastOnce()).getValue(DashboardPresenter.SHAPESHIFT_ANNOUNCEMENT_DISMISSED, false)
        verifyNoMoreInteractions(prefsUtil)
        verify(swipeToReceiveHelper).storeEthAddress()
        verifyNoMoreInteractions(swipeToReceiveHelper)
        verify(ethDataManager).fetchEthAddress()
        verifyNoMoreInteractions(ethDataManager)
        verify(payloadDataManager).updateAllBalances()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).getBtcBalance(any())
        verifyNoMoreInteractions(transactionListDataManager)
        verify(exchangeRateFactory, times(2)).getLastBtcPrice("USD")
        verify(exchangeRateFactory, times(2)).getLastEthPrice("USD")
        verify(exchangeRateFactory).getSymbol("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady onboarding not complete`() {
        // Arrange
//        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
//        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false))
                .thenReturn(false)
        whenever(appUtil.isNewlyCreated).thenReturn(true)
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(true))
        val combinedEthModel: CombinedEthModel = mock()
        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.just(combinedEthModel))
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        val btcBalance = 21_000_000_000L
        whenever(transactionListDataManager.getBtcBalance(any())).thenReturn(btcBalance)
        val ethBalance = 22_000_000_000L
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(ethBalance))
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(0)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastEthPrice("USD")).thenReturn(3.0)
        whenever(exchangeRateFactory.getSymbol("USD")).thenReturn("$")
        whenever(stringUtils.getString(any())).thenReturn("")
        whenever(stringUtils.getFormattedString(any(), any())).thenReturn("")
        // Act
        subject.onViewReady()
        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        // TODO: Fix me
//        verify(view).updateBtcBalance("210.0 BTC")
//        verify(view).updateEthBalance("0.00000002 ETH")
//        verify(view).updateTotalBalance("\$420.00")
        verifyNoMoreInteractions(view)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verifyNoMoreInteractions(prefsUtil)
        verify(appUtil, atLeastOnce()).isNewlyCreated
        verifyNoMoreInteractions(appUtil)
        verify(swipeToReceiveHelper).storeEthAddress()
        verifyNoMoreInteractions(swipeToReceiveHelper)
        verify(ethDataManager).fetchEthAddress()
        verifyNoMoreInteractions(ethDataManager)
        verify(payloadDataManager).updateAllBalances()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).getBtcBalance(any())
        verifyNoMoreInteractions(transactionListDataManager)
        verify(exchangeRateFactory, atLeastOnce()).getLastBtcPrice("USD")
        verify(exchangeRateFactory, times(2)).getLastEthPrice("USD")
        verify(exchangeRateFactory, atLeastOnce()).getSymbol("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady onboarding complete with announcement`() {
        // Arrange
//        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
//        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false))
                .thenReturn(true)
        whenever(appUtil.isNewlyCreated).thenReturn(false)
        val combinedEthModel: CombinedEthModel = mock()
        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.just(combinedEthModel))
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
//        val mockWallet: Wallet = mock()
//        whenever(payloadDataManager.wallet).thenReturn(mockWallet)
//        whenever(payloadDataManager.wallet.guid).thenReturn("")
//        whenever(payloadDataManager.wallet.sharedKey).thenReturn("")
        val btcBalance = 21_000_000_000L
        whenever(transactionListDataManager.getBtcBalance(any())).thenReturn(btcBalance)
        val ethBalance = 22_000_000_000L
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(ethBalance))
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(0)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastEthPrice("USD")).thenReturn(3.0)
        whenever(exchangeRateFactory.getSymbol("USD")).thenReturn("$")
        whenever(prefsUtil.getValue(DashboardPresenter.SHAPESHIFT_ANNOUNCEMENT_DISMISSED, false))
                .thenReturn(false)
        // Act
        subject.onViewReady()
        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        // TODO: Fix me
//        verify(view).updateBtcBalance("210.0 BTC")
//        verify(view).updateEthBalance("0.00000002 ETH")
//        verify(view).updateTotalBalance("\$420.00")
        verifyNoMoreInteractions(view)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, atLeastOnce()).getValue(DashboardPresenter.SHAPESHIFT_ANNOUNCEMENT_DISMISSED, false)
        verify(prefsUtil, atLeastOnce()).setValue(DashboardPresenter.SHAPESHIFT_ANNOUNCEMENT_DISMISSED, true)
        verifyNoMoreInteractions(prefsUtil)
        verify(swipeToReceiveHelper).storeEthAddress()
        verifyNoMoreInteractions(swipeToReceiveHelper)
        verify(ethDataManager).fetchEthAddress()
        verifyNoMoreInteractions(ethDataManager)
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).wallet
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).getBtcBalance(any())
        verifyNoMoreInteractions(transactionListDataManager)
        verify(exchangeRateFactory, times(2)).getLastBtcPrice("USD")
        verify(exchangeRateFactory, times(2)).getLastEthPrice("USD")
        verify(exchangeRateFactory).getSymbol("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
    }

    @Test
    @Throws(Exception::class)
    fun onViewDestroyed() {
        // Arrange

        // Act
        subject.onViewDestroyed()
        // Assert
        verify(rxBus).unregister(eq(MetadataEvent::class.java), anyOrNull())
    }

    @Test
    @Throws(Exception::class)
    fun onResume() {
        // Arrange
//        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
//        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(0)
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(3.0)
        whenever(exchangeRateFactory.getSymbol("USD")).thenReturn("$")
        whenever(chartsDataManager.getMonthPrice(CryptoCurrencies.BTC, "USD"))
                .thenReturn(Observable.just(mock(ChartDatumDto::class)))
        whenever(exchangeRateFactory.updateTickers())
                .thenReturn(Observable.just(mapOf("" to mock(PriceDatum::class))))
        val combinedEthModel: CombinedEthModel = mock()
        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.just(combinedEthModel))
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        val btcBalance = 21_000_000_000L
        whenever(transactionListDataManager.getBtcBalance(any())).thenReturn(btcBalance)
        val ethBalance = 22_000_000_000L
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(ethBalance))
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(0)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastEthPrice("USD")).thenReturn(3.0)
        whenever(exchangeRateFactory.getSymbol("USD")).thenReturn("$")
        // Act
        subject.onResume()
        // Assert
        verify(view, times(3)).updateChartState(any())
        verify(view).updateCryptoCurrencyPrice(any())
        // TODO: Fix me
//        verify(view).updateBtcBalance("210.0 BTC")
//        verify(view).updateEthBalance("0.00000002 ETH")
//        verify(view).updateTotalBalance("\$420.00")
        verify(view).updateDashboardSelectedCurrency(any())
        verifyNoMoreInteractions(view)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verifyNoMoreInteractions(prefsUtil)
        verify(ethDataManager).fetchEthAddress()
        verifyNoMoreInteractions(ethDataManager)
        verify(payloadDataManager).updateAllBalances()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).getBtcBalance(any())
        verifyNoMoreInteractions(transactionListDataManager)
        verify(exchangeRateFactory, atLeastOnce()).getLastBtcPrice("USD")
        verify(exchangeRateFactory, times(2)).getLastEthPrice("USD")
        verify(exchangeRateFactory).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getSymbol("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
    }

}