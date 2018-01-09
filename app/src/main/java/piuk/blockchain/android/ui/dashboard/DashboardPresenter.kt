package piuk.blockchain.android.ui.dashboard

import android.support.annotation.VisibleForTesting
import io.reactivex.Observable
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.charts.ChartsDataManager
import piuk.blockchain.android.data.charts.TimeSpan
import piuk.blockchain.android.data.charts.models.ChartDatumDto
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.exchange.BuyDataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.walletoptions.WalletOptionsDataManager
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
        private val currencyState: CurrencyState,
        private val walletOptionsDataManager: WalletOptionsDataManager
) : BasePresenter<DashboardView>() {

    private val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(getBtcUnitType()) }
    private val displayList = mutableListOf<Any>(
            stringUtils.getString(R.string.dashboard_balances),
            PieChartsState.Loading,
            stringUtils.getString(R.string.dashboard_price_charts)
    )
    private val metadataObservable by unsafeLazy { rxBus.register(MetadataEvent::class.java) }
    private var timeSpan = TimeSpan.MONTH
    @VisibleForTesting var btcBalance: Long = 0L
    @VisibleForTesting var bchBalance: Long = 0L
    @VisibleForTesting var ethBalance: BigInteger = BigInteger.ZERO

    override fun onViewReady() {
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

    internal fun onResume() {
        updateChartsData(timeSpan)
        updateAllBalances()
        updatePrices()
    }

    private fun updatePrices() {
        exchangeRateFactory.updateTickers()
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(
                        {
                            //                            updateCryptoPrice()
                        },
                        { Timber.e(it) }
                )
    }

    private fun updateChartsData(timeSpan: TimeSpan) {
        this.timeSpan = timeSpan
        compositeDisposable.clear()

        view.updateChartState(ChartsState.TimeSpanUpdated(timeSpan))
//
//        when (timeSpan) {
//            TimeSpan.ALL_TIME -> chartsDataManager.getAllTimePrice(cryptoCurrency, getFiatCurrency())
//            TimeSpan.YEAR -> chartsDataManager.getYearPrice(cryptoCurrency, getFiatCurrency())
//            TimeSpan.MONTH -> chartsDataManager.getMonthPrice(cryptoCurrency, getFiatCurrency())
//            TimeSpan.WEEK -> chartsDataManager.getWeekPrice(cryptoCurrency, getFiatCurrency())
//            TimeSpan.DAY -> chartsDataManager.getDayPrice(cryptoCurrency, getFiatCurrency())
//        }.compose(RxUtil.addObservableToCompositeDisposable(this))
//                .toList()
//                .doOnSubscribe { view.updateChartState(ChartsState.Loading) }
//                .doOnSuccess { view.updateChartState(getChartsData(it)) }
//                .doOnError { view.updateChartState(ChartsState.Error) }
//                .subscribe(
//                        { view.updateDashboardSelectedCurrency(cryptoCurrency) },
//                        { Timber.e(it) }
//                )
    }

    private fun getChartsData(list: List<ChartDatumDto>) = ChartsState.Data(
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

                                bchBalance = transactionListDataManager.getBchBalance(ItemAccount().apply {
                                    type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
                                })
                                ethBalance = ethAddressResponse.getTotalBalance()
                                updateCryptoBalances()

                                val btcFiat = exchangeRateFactory.getLastBtcPrice(getFiatCurrency()) * (btcBalance / 1e8)
                                val bchFiat = exchangeRateFactory.getLastBchPrice(getFiatCurrency()) * (bchBalance / 1e8)
                                val ethFiat = BigDecimal(exchangeRateFactory.getLastEthPrice(getFiatCurrency()))
                                        .multiply(Convert.fromWei(BigDecimal(ethBalance), Convert.Unit.ETHER))

                                val totalDouble = btcFiat.plus(ethFiat.toDouble())

                                val totalString = "${getCurrencySymbol()}${monetaryUtil.getFiatFormat(getFiatCurrency()).format(totalDouble)}"

                                view.updatePieChartState(
                                        PieChartsState.Data(
                                                fiatSymbol = getCurrencySymbol(),
                                                // Amounts in Fiat
                                                bitcoinValue = BigDecimal.valueOf(btcFiat),
                                                etherValue = ethFiat,
                                                bitcoinCashValue = BigDecimal.valueOf(bchFiat),
                                                // Formatted fiat value Strings
                                                bitcoinValueString = getBtcFiatString(btcBalance),
                                                etherValueString = getEthFiatString(ethBalance),
                                                bitcoinCashValueString = getBchFiatString(bchBalance),
                                                // Formatted Amount Strings
                                                bitcoinAmountString = getBtcBalanceString(btcBalance),
                                                etherAmountString = getEthBalanceString(ethBalance),
                                                bitcoinCashAmountString = getBchBalanceString(bchBalance),
                                                // Total
                                                totalValueString = totalString
                                        )
                                )
                            }
                }
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .subscribe(
                        { /* No-op*/ },
                        { Timber.e(it) }
                )
    }

    private fun updateCryptoBalances() {

//        view.updateBtcBalance(getBtcBalanceString(
//                currencyState.isDisplayingCryptoCurrency,
//                btcBalance
//        ))
//        view.updateEthBalance(getEthBalanceString(
//                currencyState.isDisplayingCryptoCurrency,
//                ethBalance
//        ))
    }

    private fun showAnnouncement() {
        // Don't add the announcement if there already is one
        if (displayList.none { it is AnnouncementData }) {
            // In the future, the announcement data may be parsed from an endpoint. For now, here is fine
            val announcementData = AnnouncementData(
                    title = R.string.onboarding_shapeshift_title,
                    description = R.string.onboarding_shapeshift_description,
                    link = R.string.onboarding_shapeshift_cta,
                    image = R.drawable.vector_exchange_offset,
                    emoji = "\uD83C\uDF89",
                    closeFunction = { dismissAnnouncement() },
                    linkFunction = {
                        view.startShapeShiftActivity()
                    }
            )

            displayList.add(0, announcementData)
            view.notifyItemAdded(displayList, 0)
        }
    }

    private fun dismissAnnouncement() {
        prefsUtil.setValue(SHAPESHIFT_ANNOUNCEMENT_DISMISSED, true)
        if (displayList.any { it is AnnouncementData }) {
            displayList.removeAll { it is AnnouncementData }
            view.notifyItemRemoved(displayList, 0)
        }
    }

    private fun getOnboardingStatusObservable(): Observable<Boolean> {
        return if (isOnboardingComplete()) {
            Observable.just(false)
        } else {
            buyDataManager.canBuy
                    .compose(RxUtil.addObservableToCompositeDisposable(this))
                    .doOnNext { displayList.removeAll { it is OnboardingModel } }
                    .doOnNext { displayList.add(0, getOnboardingPages(it)) }
                    .doOnNext { view.notifyItemAdded(displayList, 0) }
                    .doOnError { Timber.e(it) }
        }
    }

    private fun checkLatestAnnouncement() {
        // If user hasn't completed onboarding, ignore announcements
        if (isOnboardingComplete() && !prefsUtil.getValue(SHAPESHIFT_ANNOUNCEMENT_DISMISSED, false)) {
            prefsUtil.setValue(SHAPESHIFT_ANNOUNCEMENT_DISMISSED, true)

            walletOptionsDataManager.showShapeshift(payloadDataManager.wallet.guid, payloadDataManager.wallet.sharedKey)
                    .compose(RxUtil.addObservableToCompositeDisposable(this))
                    .subscribe(
                            { if (it) showAnnouncement() },
                            { Timber.e(it) }
                    )
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

    private fun getBtcBalanceString(btcBalance: Long): String {
        var balance = monetaryUtil.getDisplayAmountWithFormatting(btcBalance)
        // Replace 0.0 with 0 to match web
        if (balance == "0.0") balance = "0"

        return "$balance ${getBtcDisplayUnits()}"
    }

    private fun getBtcFiatString(btcBalance: Long): String {
        val strFiat = getFiatCurrency()
        val fiatBalance = exchangeRateFactory.getLastBtcPrice(strFiat) * (btcBalance / 1e8)
        
        return "${monetaryUtil.getFiatFormat(strFiat).format(fiatBalance)} $strFiat"
    }

    private fun getBchBalanceString(bchBalance: Long): String {
        var balance = monetaryUtil.getDisplayAmountWithFormatting(bchBalance)
        // Replace 0.0 with 0 to match web
        if (balance == "0.0") balance = "0"

        return "$balance ${getBchDisplayUnits()}"
    }

    private fun getBchFiatString(bchBalance: Long): String {
        val strFiat = getFiatCurrency()
        val fiatBalance = exchangeRateFactory.getLastBchPrice(strFiat) * (bchBalance / 1e8)

        return "${monetaryUtil.getFiatFormat(strFiat).format(fiatBalance)} $strFiat"
    }

    private fun getEthBalanceString(ethBalance: BigInteger): String {
        val number = DecimalFormat.getInstance().apply { maximumFractionDigits = 8 }
                .run { format(Convert.fromWei(BigDecimal(ethBalance), Convert.Unit.ETHER)) }

        return "$number ETH"
    }

    private fun getEthFiatString(ethBalance: BigInteger): String {
        val strFiat = getFiatCurrency()
        val fiatBalance = BigDecimal.valueOf(exchangeRateFactory.getLastEthPrice(strFiat))
                .multiply(Convert.fromWei(BigDecimal(ethBalance), Convert.Unit.ETHER))

        return "${monetaryUtil.getFiatFormat(strFiat).format(fiatBalance.toDouble())} $strFiat"
    }

    private fun getBtcPriceString(): String {
        val lastBtcPrice = getLastBtcPrice(getFiatCurrency())
        return "${getCurrencySymbol()}${monetaryUtil.getFiatFormat(getFiatCurrency()).format(lastBtcPrice)}"
    }

    private fun getEthPriceString(): String {
        val lastEthPrice = getLastEthPrice(getFiatCurrency())
        return "${getCurrencySymbol()}${monetaryUtil.getFiatFormat(getFiatCurrency()).format(lastEthPrice)}"
    }

    private fun getCurrencySymbol() = monetaryUtil.getCurrencySymbol(getFiatCurrency(), view.locale)

    private fun getBtcDisplayUnits() = monetaryUtil.getBtcUnits()[getBtcUnitType()]

    private fun getBchDisplayUnits() = monetaryUtil.getBchUnits()[getBtcUnitType()]

    private fun getBtcUnitType() =
            prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)

    private fun getFiatCurrency() =
            prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

    private fun getLastBtcPrice(fiat: String) = exchangeRateFactory.getLastBtcPrice(fiat)

    private fun getLastEthPrice(fiat: String) = exchangeRateFactory.getLastEthPrice(fiat)

    companion object {

        @VisibleForTesting const val SHAPESHIFT_ANNOUNCEMENT_DISMISSED = "SHAPESHIFT_ANNOUNCEMENT_DISMISSED"

    }

}

sealed class ChartsState {

    data class Data(
            val data: List<ChartDatumDto>,
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

sealed class PieChartsState {

    data class Data(
            val fiatSymbol: String,
            // Amounts in Fiat
            val bitcoinValue: BigDecimal,
            val etherValue: BigDecimal,
            val bitcoinCashValue: BigDecimal,
            // Formatted fiat value Strings
            val bitcoinValueString: String,
            val etherValueString: String,
            val bitcoinCashValueString: String,
            // Formatted Amount Strings
            val bitcoinAmountString: String,
            val etherAmountString: String,
            val bitcoinCashAmountString: String,
            // Total String
            val totalValueString: String
    ) : PieChartsState()

    object Error : PieChartsState()
    object Loading : PieChartsState()
}