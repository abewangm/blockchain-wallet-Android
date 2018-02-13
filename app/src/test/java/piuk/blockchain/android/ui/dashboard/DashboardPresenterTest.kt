package piuk.blockchain.android.ui.dashboard

import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Completable
import io.reactivex.Observable
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.data.bitcoincash.BchDataManager
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
import java.util.*

@Suppress("IllegalIdentifier")
class DashboardPresenterTest: RxTest(){

    private lateinit var subject: DashboardPresenter
    private val prefsUtil: PrefsUtil = mock()
    private val exchangeRateFactory: ExchangeRateFactory = mock()
    private val ethDataManager: EthDataManager = mock()
    private val bchDataManager: BchDataManager = mock()
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
    override fun setUp() {
        super.setUp()

        subject = DashboardPresenter(
                prefsUtil,
                exchangeRateFactory,
                ethDataManager,
                bchDataManager,
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

        whenever(view.locale).thenReturn(Locale.US)
        whenever(bchDataManager.getWalletTransactions(50, 0))
                .thenReturn(Observable.just(emptyList()))
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady onboarding complete, no announcement`() {
        // Arrange
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false))
                .thenReturn(true)
        whenever(appUtil.isNewlyCreated).thenReturn(false)
        val combinedEthModel: CombinedEthModel = mock()
        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.just(combinedEthModel))
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        val btcBalance = 21_000_000_000L
        whenever(transactionListDataManager.getBtcBalance(any())).thenReturn(btcBalance)
        val ethBalance = 22_000_000_000L
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(ethBalance))
        val bchBalance = 21_000_000_000L
        whenever(transactionListDataManager.getBchBalance(any())).thenReturn(bchBalance)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(0)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastEthPrice("USD")).thenReturn(3.0)
        whenever(exchangeRateFactory.updateTickers())
                .thenReturn(Observable.just(mapOf("" to PriceDatum())))
        whenever(prefsUtil.getValue(DashboardPresenter.BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, false))
                .thenReturn(true)
        whenever(stringUtils.getString(any())).thenReturn("")
        whenever(bchDataManager.updateAllBalances()).thenReturn(Completable.complete())
        // Act
        subject.onViewReady()
        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(view, atLeastOnce()).locale
        verify(view, atLeastOnce()).updatePieChartState(any())
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, atLeastOnce()).getValue(DashboardPresenter.BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, false)
        verifyNoMoreInteractions(prefsUtil)
        verify(ethDataManager).fetchEthAddress()
        verifyNoMoreInteractions(ethDataManager)
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).getBtcBalance(any())
        verify(transactionListDataManager).getBchBalance(any())
        verifyNoMoreInteractions(transactionListDataManager)
        verify(exchangeRateFactory, times(3)).getLastBtcPrice("USD")
        verify(exchangeRateFactory, times(3)).getLastEthPrice("USD")
        verify(exchangeRateFactory, times(3)).getLastBchPrice("USD")
        verify(exchangeRateFactory).updateTickers()
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(bchDataManager, atLeastOnce()).updateAllBalances()
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady onboarding not complete`() {
        // Arrange
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false))
                .thenReturn(false)
        whenever(appUtil.isNewlyCreated).thenReturn(true)
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(true))
        val combinedEthModel: CombinedEthModel = mock()
        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.just(combinedEthModel))
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        val btcBalance = 21_000_000_000L
        whenever(transactionListDataManager.getBtcBalance(any())).thenReturn(btcBalance)
        val ethBalance = 22_000_000_000L
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(ethBalance))
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(0)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastEthPrice("USD")).thenReturn(3.0)
        whenever(exchangeRateFactory.updateTickers())
                .thenReturn(Observable.just(mapOf("" to PriceDatum())))
        whenever(stringUtils.getString(any())).thenReturn("")
        whenever(stringUtils.getFormattedString(any(), any())).thenReturn("")
        whenever(bchDataManager.updateAllBalances()).thenReturn(Completable.complete())
        // Act
        subject.onViewReady()
        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(view, atLeastOnce()).locale
        verify(view, atLeastOnce()).updatePieChartState(any())
        verify(view, atLeastOnce()).scrollToTop()
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verifyNoMoreInteractions(prefsUtil)
        verify(appUtil, atLeastOnce()).isNewlyCreated
        verifyNoMoreInteractions(appUtil)
        verify(ethDataManager).fetchEthAddress()
        verifyNoMoreInteractions(ethDataManager)
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).getBtcBalance(any())
        verify(transactionListDataManager).getBchBalance(any())
        verifyNoMoreInteractions(transactionListDataManager)
        verify(exchangeRateFactory, times(4)).getLastBtcPrice("USD")
        verify(exchangeRateFactory, times(3)).getLastEthPrice("USD")
        verify(exchangeRateFactory, times(3)).getLastBchPrice("USD")
        verify(exchangeRateFactory).updateTickers()
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
        verify(bchDataManager).updateAllBalances()
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady onboarding complete with bch and Sfox announcement`() {
        // Arrange
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false))
                .thenReturn(true)
        whenever(appUtil.isNewlyCreated).thenReturn(false)
        val combinedEthModel: CombinedEthModel = mock()
        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.just(combinedEthModel))
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        val btcBalance = 21_000_000_000L
        whenever(transactionListDataManager.getBtcBalance(any())).thenReturn(btcBalance)
        val ethBalance = 22_000_000_000L
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(ethBalance))
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(0)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastEthPrice("USD")).thenReturn(3.0)
        whenever(exchangeRateFactory.updateTickers())
                .thenReturn(Observable.just(mapOf("" to PriceDatum())))
        whenever(prefsUtil.getValue(DashboardPresenter.BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, false))
                .thenReturn(false)
        whenever(prefsUtil.getValue(DashboardPresenter.SFOX_ANNOUNCEMENT_DISMISSED, false))
                .thenReturn(false)
        whenever(stringUtils.getString(any())).thenReturn("")
        whenever(bchDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(true))
        // Act
        subject.onViewReady()
        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(exchangeRateFactory).updateTickers()
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(view, atLeastOnce()).locale
        verify(view, atLeastOnce()).updatePieChartState(any())
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, atLeastOnce()).getValue(DashboardPresenter.BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, false)
        verify(prefsUtil, atLeastOnce()).setValue(DashboardPresenter.BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, true)
        verify(prefsUtil, atLeastOnce()).getValue(DashboardPresenter.SFOX_ANNOUNCEMENT_DISMISSED, false)
        verify(prefsUtil, atLeastOnce()).setValue(DashboardPresenter.SFOX_ANNOUNCEMENT_DISMISSED, true)
        verifyNoMoreInteractions(prefsUtil)
        verify(ethDataManager).fetchEthAddress()
        verifyNoMoreInteractions(ethDataManager)
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).getBtcBalance(any())
        verify(transactionListDataManager).getBchBalance(any())
        verifyNoMoreInteractions(transactionListDataManager)
        verify(exchangeRateFactory, times(3)).getLastBtcPrice("USD")
        verify(exchangeRateFactory, times(3)).getLastEthPrice("USD")
        verify(exchangeRateFactory, times(3)).getLastBchPrice("USD")
        verify(exchangeRateFactory).updateTickers()
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(bchDataManager, atLeastOnce()).updateAllBalances()
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady onboarding complete with bch but no Sfox announcement`() {
        // Arrange
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false))
                .thenReturn(true)
        whenever(appUtil.isNewlyCreated).thenReturn(false)
        val combinedEthModel: CombinedEthModel = mock()
        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.just(combinedEthModel))
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        val btcBalance = 21_000_000_000L
        whenever(transactionListDataManager.getBtcBalance(any())).thenReturn(btcBalance)
        val ethBalance = 22_000_000_000L
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(ethBalance))
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(0)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastEthPrice("USD")).thenReturn(3.0)
        whenever(exchangeRateFactory.updateTickers())
                .thenReturn(Observable.just(mapOf("" to PriceDatum())))
        whenever(prefsUtil.getValue(DashboardPresenter.BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, false))
                .thenReturn(false)
        whenever(stringUtils.getString(any())).thenReturn("")
        whenever(bchDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(false))
        // Act
        subject.onViewReady()
        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(exchangeRateFactory).updateTickers()
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(view, atLeastOnce()).locale
        verify(view, atLeastOnce()).updatePieChartState(any())
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, atLeastOnce()).getValue(DashboardPresenter.BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, false)
        verify(prefsUtil, atLeastOnce()).setValue(DashboardPresenter.BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, true)
        verifyNoMoreInteractions(prefsUtil)
        verify(ethDataManager).fetchEthAddress()
        verifyNoMoreInteractions(ethDataManager)
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).getBtcBalance(any())
        verify(transactionListDataManager).getBchBalance(any())
        verifyNoMoreInteractions(transactionListDataManager)
        verify(exchangeRateFactory, times(3)).getLastBtcPrice("USD")
        verify(exchangeRateFactory, times(3)).getLastEthPrice("USD")
        verify(exchangeRateFactory, times(3)).getLastBchPrice("USD")
        verify(exchangeRateFactory).updateTickers()
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(bchDataManager, atLeastOnce()).updateAllBalances()
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

}