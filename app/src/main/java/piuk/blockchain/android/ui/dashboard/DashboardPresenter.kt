package piuk.blockchain.android.ui.dashboard

import android.support.annotation.VisibleForTesting
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Observable
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.charts.ChartsDataManager
import piuk.blockchain.android.data.charts.TimeSpan
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.exchange.BuyDataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.balance.AnnouncementData
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.dashboard.models.OnboardingModel
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.android.ui.onboarding.OnboardingPagerContent
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.*
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat
import javax.inject.Inject

class DashboardPresenter @Inject constructor(
        private val chartsDataManager: ChartsDataManager,
        private val prefsUtil: PrefsUtil,
        private val exchangeRateFactory: ExchangeRateFactory,
        private val ethDataManager: EthDataManager,
        private val payloadDataManager: PayloadDataManager,
        private val transactionListDataManager: TransactionListDataManager,
        private val stringUtils: StringUtils,
        private val appUtil: AppUtil,
        private val buyDataManager: BuyDataManager,
        private val rxBus: RxBus,
        private val swipeToReceiveHelper: SwipeToReceiveHelper,
        private val currencyState: CurrencyState
) : BasePresenter<DashboardView>() {

    private val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(getBtcUnitType()) }
    private lateinit var cryptoCurrency: CryptoCurrencies
    private val displayList = mutableListOf<Any>(ChartDisplayable())
    private val metadataObservable by unsafeLazy { rxBus.register(MetadataEvent::class.java) }
    private var timeSpan = TimeSpan.MONTH
    @VisibleForTesting var btcBalance = 0L
    @VisibleForTesting var ethBalance = BigInteger.ZERO

    override fun onViewReady() {
        cryptoCurrency = currencyState.cryptoCurrency
        view.notifyItemAdded(displayList, 0)

        // Triggers various updates to the page once all metadata is loaded
        metadataObservable.flatMap { getOnboardingStatusObservable() }
                .doOnNext { swipeToReceiveHelper.storeEthAddress() }
                .doOnNext { updateAllBalances() }
                .doOnNext { checkLatestAnnouncement() }
                .subscribe(
                        { /* No-op */ },
                        { Timber.e(it) }
                )
    }

    override fun onViewDestroyed() {
        rxBus.unregister(MetadataEvent::class.java, metadataObservable)
        super.onViewDestroyed()
    }

    internal fun updateSelectedCurrency(cryptoCurrency: CryptoCurrencies) {
        this.cryptoCurrency = cryptoCurrency
        currencyState.cryptoCurrency = cryptoCurrency
        updateCryptoPrice()
        updateChartsData(timeSpan)
    }

    internal fun onResume() {
        cryptoCurrency = currencyState.cryptoCurrency
        updateChartsData(timeSpan)
        updateAllBalances()
        updatePrices()
    }

    fun getCurrentCryptoCurrency(): Int {
        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> return 0
            CryptoCurrencies.ETHER -> return 1
            else -> return 1
        }
    }

    internal fun invertViewType() {
        currencyState.toggleDisplayingCrypto()
        updateCryptoBalances()
    }

    private fun updatePrices() {
        exchangeRateFactory.updateTickers()
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .subscribe(
                        { updateCryptoPrice() },
                        { Timber.e(it) }
                )
    }

    private fun updateCryptoPrice() {
        view.updateCryptoCurrencyPrice(
                if (cryptoCurrency == CryptoCurrencies.BTC)
                    getBtcString() else getEthString()
        )
    }

    private fun updateChartsData(timeSpan: TimeSpan) {
        this.timeSpan = timeSpan
        compositeDisposable.clear()

        view.updateChartState(ChartsState.TimeSpanUpdated(timeSpan))

        when (timeSpan) {
            TimeSpan.ALL_TIME -> chartsDataManager.getAllTimePrice(cryptoCurrency, getFiatCurrency())
            TimeSpan.YEAR -> chartsDataManager.getYearPrice(cryptoCurrency, getFiatCurrency())
            TimeSpan.MONTH -> chartsDataManager.getMonthPrice(cryptoCurrency, getFiatCurrency())
            TimeSpan.WEEK -> chartsDataManager.getWeekPrice(cryptoCurrency, getFiatCurrency())
            TimeSpan.DAY -> chartsDataManager.getDayPrice(cryptoCurrency, getFiatCurrency())
        }.compose(RxUtil.addObservableToCompositeDisposable(this))
                .toList()
                .doOnSubscribe { view.updateChartState(ChartsState.Loading) }
                .doOnSuccess { view.updateChartState(getChartsData(it)) }
                .doOnError { view.updateChartState(ChartsState.Error) }
                .subscribe(
                        {
                            view.updateDashboardSelectedCurrency(cryptoCurrency)
                        },
                        { Timber.e(it) }
                )
    }

    private fun getChartsData(list: List<PriceDatum>) = ChartsState.Data(
            data = list,
            fiatSymbol = getCurrencySymbol(),
            getChartAllTime = { updateChartsData(TimeSpan.ALL_TIME) },
            getChartYear = { updateChartsData(TimeSpan.YEAR) },
            getChartMonth = { updateChartsData(TimeSpan.MONTH) },
            getChartWeek = { updateChartsData(TimeSpan.WEEK) },
            getChartDay = { updateChartsData(TimeSpan.DAY) }
    )

    private fun updateAllBalances() {
        ethDataManager.fetchEthAddress()
                .flatMapCompletable { ethAddressResponse ->
                    payloadDataManager.updateAllBalances()
                            .doOnComplete {
                                btcBalance = transactionListDataManager.getBtcBalance(ItemAccount().apply {
                                    type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
                                })
                                ethBalance = ethAddressResponse.getTotalBalance()
                                updateCryptoBalances()

                                val btcFiat = exchangeRateFactory.getLastBtcPrice(getFiatCurrency()) * (btcBalance / 1e8)
                                val ethFiat = BigDecimal(exchangeRateFactory.getLastEthPrice(getFiatCurrency()))
                                        .multiply(Convert.fromWei(BigDecimal(ethBalance), Convert.Unit.ETHER))

                                val totalDouble = btcFiat.plus(ethFiat.toDouble())

                                val totalString = "${getCurrencySymbol()}${monetaryUtil.getFiatFormat(getFiatCurrency()).format(totalDouble)}"
                                view.updateTotalBalance(totalString)
                            }
                }.subscribe(
                { /* No-op*/ },
                { Timber.e(it) }
        )
    }

    private fun updateCryptoBalances() {
        view.updateBtcBalance(getBtcBalanceString(
                currencyState.isDisplayingCryptoCurrency,
                btcBalance
        ))
        view.updateEthBalance(getEthBalanceString(
                currencyState.isDisplayingCryptoCurrency,
                ethBalance
        ))
    }

    private fun showAnnouncement() {
        // Don't add the announcement if there already is one
        if (displayList.none { it is AnnouncementData }) {
            // In the future, the announcement data may be parsed from an endpoint. For now, here is fine
            val announcementData = AnnouncementData(
                    title = R.string.onboarding_ether_title,
                    description = R.string.onboarding_ether_description,
                    link = R.string.onboarding_ether_cta,
                    image = R.drawable.vector_eth_offset,
                    emoji = "",
                    closeFunction = { dismissAnnouncement() },
                    linkFunction = { view.startReceiveFragment() }
            )

            displayList.add(0, announcementData)
            view.notifyItemAdded(displayList, 0)
        }
    }

    private fun dismissAnnouncement() {
        prefsUtil.setValue(ETH_ANNOUNCEMENT_DISMISSED, true)
        if (displayList.any { it is AnnouncementData }) {
            displayList.removeAll { it is AnnouncementData }
            view.notifyItemRemoved(displayList, 0)
        }
    }

    private fun getOnboardingStatusObservable(): Observable<Boolean> {
        return if (isOnboardingComplete()) {
            Observable.just(false)
        } else
            buyDataManager.canBuy
                    .compose(RxUtil.addObservableToCompositeDisposable(this))
                    .doOnNext { displayList.add(0, getOnboardingPages(it)) }
                    .doOnNext { view.notifyItemAdded(displayList, 0) }
                    .doOnError { Timber.e(it) }
    }

    private fun checkLatestAnnouncement() {
        // If user hasn't completed onboarding, ignore announcements
        if (isOnboardingComplete()) {
            if (!prefsUtil.getValue(ETH_ANNOUNCEMENT_DISMISSED, false)) {
                prefsUtil.setValue(ETH_ANNOUNCEMENT_DISMISSED, true)
                showAnnouncement()
            }
        }
    }

    private fun getOnboardingPages(isBuyAllowed: Boolean): OnboardingModel {
        val pages = mutableListOf<OnboardingPagerContent>()
        if (isBuyAllowed) {
            // Buy bitcoin prompt
            pages.add(
                    OnboardingPagerContent(
                            stringUtils.getString(R.string.onboarding_current_price),
                            getFormattedPriceString(),
                            stringUtils.getString(R.string.onboarding_buy_content),
                            stringUtils.getString(R.string.onboarding_buy_bitcoin),
                            MainActivity.ACTION_BUY,
                            R.color.primary_blue_accent,
                            R.drawable.vector_buy_offset
                    )
            )
        }
        // Receive bitcoin
        pages.add(
                OnboardingPagerContent(
                        stringUtils.getString(R.string.onboarding_receive_bitcoin),
                        "",
                        stringUtils.getString(R.string.onboarding_receive_content),
                        stringUtils.getString(R.string.receive_bitcoin),
                        MainActivity.ACTION_RECEIVE,
                        R.color.secondary_teal_medium,
                        R.drawable.vector_receive_offset
                )
        )
        // QR Codes
        pages.add(
                OnboardingPagerContent(
                        stringUtils.getString(R.string.onboarding_qr_codes),
                        "",
                        stringUtils.getString(R.string.onboarding_qr_codes_content),
                        stringUtils.getString(R.string.onboarding_scan_address),
                        MainActivity.ACTION_SEND,
                        R.color.primary_navy_medium,
                        R.drawable.vector_qr_offset
                )
        )

        return OnboardingModel(
                pages,
                // TODO: These are neat and clever, but make things pretty hard to test. Replace with callbacks.
                dismissOnboarding = {
                    displayList.removeAll { it is OnboardingModel }
                    view.notifyItemRemoved(displayList, 0)
                },
                onboardingComplete = { setOnboardingComplete(true) },
                onboardingNotComplete = { setOnboardingComplete(false) }
        )
    }

    private fun isOnboardingComplete() =
            // If wallet isn't newly created, don't show onboarding
            prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false) || !appUtil.isNewlyCreated

    private fun setOnboardingComplete(completed: Boolean) {
        prefsUtil.setValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, completed)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Units
    ///////////////////////////////////////////////////////////////////////////

    private fun getFormattedPriceString(): String {
        val lastPrice = getLastBtcPrice(getFiatCurrency())
        val fiatSymbol = exchangeRateFactory.getSymbol(getFiatCurrency())
        val format = DecimalFormat().apply { minimumFractionDigits = 2 }

        return stringUtils.getFormattedString(
                R.string.current_price_btc,
                "$fiatSymbol${format.format(lastPrice)}"
        )
    }

    private fun getBtcBalanceString(isBtc: Boolean, btcBalance: Long): String {
        val strFiat = getFiatCurrency()
        val fiatBalance = exchangeRateFactory.getLastBtcPrice(strFiat) * (btcBalance / 1e8)

        return if (isBtc) {
            "${monetaryUtil.getDisplayAmountWithFormatting(btcBalance)} ${getBtcDisplayUnits()}"
        } else {
            "${monetaryUtil.getFiatFormat(strFiat).format(fiatBalance)} $strFiat"
        }
    }

    private fun getEthBalanceString(isEth: Boolean, ethBalance: BigInteger): String {
        val strFiat = getFiatCurrency()
        val fiatBalance = BigDecimal.valueOf(exchangeRateFactory.getLastEthPrice(strFiat))
                .multiply(Convert.fromWei(BigDecimal(ethBalance), Convert.Unit.ETHER))
        val number = DecimalFormat.getInstance().apply { maximumFractionDigits = 8 }
                .run { format(Convert.fromWei(BigDecimal(ethBalance), Convert.Unit.ETHER)) }

        return if (isEth) {
            "$number ETH"
        } else {
            "${monetaryUtil.getFiatFormat(strFiat).format(fiatBalance.toDouble())} $strFiat"
        }
    }

    private fun getCurrencySymbol() = exchangeRateFactory.getSymbol(getFiatCurrency())

    private fun getBtcString(): String {
        val lastBtcPrice = getLastBtcPrice(getFiatCurrency())
        return "${getCurrencySymbol()}${monetaryUtil.getFiatFormat(getFiatCurrency()).format(lastBtcPrice)}"
    }

    private fun getEthString(): String {
        val lastEthPrice = getLastEthPrice(getFiatCurrency())
        return "${getCurrencySymbol()}${monetaryUtil.getFiatFormat(getFiatCurrency()).format(lastEthPrice)}"
    }

    private fun getBtcDisplayUnits() = monetaryUtil.getBtcUnits()[getBtcUnitType()]

    private fun getBtcUnitType() =
            prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)

    private fun getFiatCurrency() =
            prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

    private fun getLastBtcPrice(fiat: String) = exchangeRateFactory.getLastBtcPrice(fiat)

    private fun getLastEthPrice(fiat: String) = exchangeRateFactory.getLastEthPrice(fiat)

    companion object {

        @VisibleForTesting const val ETH_ANNOUNCEMENT_DISMISSED = "ETH_ANNOUNCEMENT_DISMISSED"

    }

}

sealed class ChartsState {

    data class Data(
            val data: List<PriceDatum>,
            val fiatSymbol: String,
            val getChartAllTime: () -> Unit,
            val getChartYear: () -> Unit,
            val getChartMonth: () -> Unit,
            val getChartWeek: () -> Unit,
            val getChartDay: () -> Unit
    ) : ChartsState()

    object Error : ChartsState()
    object Loading : ChartsState()
    class TimeSpanUpdated(val timeSpan: TimeSpan) : ChartsState()

}

class ChartDisplayable