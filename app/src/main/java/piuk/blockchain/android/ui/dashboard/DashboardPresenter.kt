package piuk.blockchain.android.ui.dashboard

import android.support.annotation.DrawableRes
import android.support.annotation.VisibleForTesting
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.currency.CryptoCurrencies
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
        private val prefsUtil: PrefsUtil,
        private val exchangeRateFactory: ExchangeRateFactory,
        private val ethDataManager: EthDataManager,
        private val bchDataManager: BchDataManager,
        private val payloadDataManager: PayloadDataManager,
        private val transactionListDataManager: TransactionListDataManager,
        private val stringUtils: StringUtils,
        private val appUtil: AppUtil,
        private val buyDataManager: BuyDataManager,
        private val rxBus: RxBus,
        private val swipeToReceiveHelper: SwipeToReceiveHelper,
        private val walletOptionsDataManager: WalletOptionsDataManager
) : BasePresenter<DashboardView>() {

    private val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(getBtcUnitType()) }
    private val displayList by unsafeLazy {
        mutableListOf<Any>(
                stringUtils.getString(R.string.dashboard_balances),
                PieChartsState.Loading,
                stringUtils.getString(R.string.dashboard_price_charts),
                AssetPriceCardState.Loading(CryptoCurrencies.BTC),
                AssetPriceCardState.Loading(CryptoCurrencies.ETHER),
                AssetPriceCardState.Loading(CryptoCurrencies.BCH)
        )
    }
    private val metadataObservable by unsafeLazy { rxBus.register(MetadataEvent::class.java) }
    @Suppress("MemberVisibilityCanBePrivate")
    @VisibleForTesting var btcBalance: Long = 0L
    @Suppress("MemberVisibilityCanBePrivate")
    @VisibleForTesting var bchBalance: Long = 0L
    @Suppress("MemberVisibilityCanBePrivate")
    @VisibleForTesting var ethBalance: BigInteger = BigInteger.ZERO

    override fun onViewReady() {
        view.notifyItemAdded(displayList, 0)
        updatePrices()

        // Triggers various updates to the page once all metadata is loaded
        metadataObservable.flatMap { getOnboardingStatusObservable() }
                .doOnNext { swipeToReceiveHelper.storeEthAddress() }
                .doOnNext { updateAllBalances() }
                .doOnNext { checkLatestAnnouncement() }
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(
                        { /* No-op */ },
                        { Timber.e(it) }
                )
    }

    override fun onViewDestroyed() {
        rxBus.unregister(MetadataEvent::class.java, metadataObservable)
        super.onViewDestroyed()
    }

    private fun updatePrices() {
        exchangeRateFactory.updateTickers()
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .doOnError { Timber.e(it) }
                .subscribe(
                        {
                            val list = listOf(
                                    AssetPriceCardState.Data(
                                            getBtcPriceString(),
                                            CryptoCurrencies.BTC,
                                            R.drawable.vector_bitcoin
                                    ),
                                    AssetPriceCardState.Data(
                                            getEthPriceString(),
                                            CryptoCurrencies.ETHER,
                                            R.drawable.vector_eth
                                    ),
                                    AssetPriceCardState.Data(
                                            getBchPriceString(),
                                            CryptoCurrencies.BCH,
                                            R.drawable.vector_bitcoin_cash
                                    )
                            )

                            handleAssetPriceUpdate(list)
                        },
                        {
                            val list = listOf(
                                    AssetPriceCardState.Error(CryptoCurrencies.BTC),
                                    AssetPriceCardState.Error(CryptoCurrencies.ETHER),
                                    AssetPriceCardState.Error(CryptoCurrencies.BCH)
                            )

                            handleAssetPriceUpdate(list)
                        }
                )
    }

    private fun handleAssetPriceUpdate(list: List<AssetPriceCardState>) {
        displayList.removeAll { it is AssetPriceCardState }
        displayList.addAll(list)

        val firstPosition = displayList.indexOfFirst { it is AssetPriceCardState }

        val positions = listOf(
                firstPosition,
                firstPosition + 1,
                firstPosition + 2
        )

        view.notifyItemUpdated(displayList, positions)
    }

    private fun updateAllBalances() {
        ethDataManager.fetchEthAddress()
                .flatMapCompletable { ethAddressResponse ->
                    payloadDataManager.updateAllBalances()
                            .andThen(
                                    payloadDataManager.updateAllTransactions()
                                            .doOnError { Timber.e(it) }
                                            .onErrorComplete()
                            )
                            .andThen(
                                    bchDataManager.updateAllBalances()
                                            .doOnError { Timber.e(it) }
                                            .onErrorComplete()
                            )
                            .doOnComplete {
                                btcBalance =
                                        transactionListDataManager.getBtcBalance(ItemAccount().apply {
                                            type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
                                        })

                                bchBalance =
                                        transactionListDataManager.getBchBalance(ItemAccount().apply {
                                            type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
                                        })
                                ethBalance = ethAddressResponse.getTotalBalance()

                                val btcFiat =
                                        exchangeRateFactory.getLastBtcPrice(getFiatCurrency()) * (btcBalance / 1e8)
                                val bchFiat =
                                        exchangeRateFactory.getLastBchPrice(getFiatCurrency()) * (bchBalance / 1e8)
                                val ethFiat =
                                        BigDecimal(
                                                exchangeRateFactory.getLastEthPrice(
                                                        getFiatCurrency()
                                                )
                                        ).multiply(
                                                Convert.fromWei(
                                                        BigDecimal(ethBalance),
                                                        Convert.Unit.ETHER
                                                )
                                        )

                                val totalDouble = btcFiat.plus(ethFiat.toDouble()).plus(bchFiat)
                                val totalString = getFormattedCurrencyString(totalDouble)

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
                                                bitcoinCashAmountString = getBchBalanceString(
                                                        bchBalance
                                                ),
                                                // Total
                                                totalValueString = totalString
                                        )
                                )
                            }
                }
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .subscribe(
                        { storeSwipeToReceiveAddresses() },
                        { Timber.e(it) }
                )
    }

    private fun showAnnouncement() {
        // Don't add the announcement if there already is one
        if (displayList.none { it is AnnouncementData }) {
            // In the future, the announcement data may be parsed from an endpoint. For now, here is fine
            val announcementData = AnnouncementData(
                    title = R.string.bitcoin_cash,
                    description = R.string.onboarding_bitcoin_cash_description,
                    link = R.string.onboarding_cta,
                    image = R.drawable.vector_bch_onboarding,
                    emoji = "\uD83C\uDF89",
                    closeFunction = { dismissAnnouncement() },
                    linkFunction = { view.startBitcoinCashReceive() }
            )

            displayList.add(0, announcementData)
            view.notifyItemAdded(displayList, 0)
            view.scrollToTop()
        }
    }

    private fun dismissAnnouncement() {
        prefsUtil.setValue(BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, true)
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
                    .doOnNext { view.scrollToTop() }
                    .doOnError { Timber.e(it) }
        }
    }

    private fun checkLatestAnnouncement() {
        // If user hasn't completed onboarding, ignore announcements
        if (isOnboardingComplete() && !prefsUtil.getValue(
                    BITCOIN_CASH_ANNOUNCEMENT_DISMISSED,
                    false
            )) {
            prefsUtil.setValue(BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, true)

            walletOptionsDataManager.showShapeshift(
                    payloadDataManager.wallet.guid,
                    payloadDataManager.wallet.sharedKey
            )
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

    private fun storeSwipeToReceiveAddresses() {
        bchDataManager.getWalletTransactions(50, 0)
                .flatMapCompletable { getSwipeToReceiveCompletable() }
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .subscribe(
                        { view.startWebsocketService() },
                        { Timber.e(it) }
                )
    }

    private fun getSwipeToReceiveCompletable(): Completable =
            // Defer to background thread as deriving addresses is quite processor intensive
            Completable.fromCallable {
                swipeToReceiveHelper.updateAndStoreBitcoinAddresses()
                swipeToReceiveHelper.updateAndStoreBitcoinCashAddresses()
            }.subscribeOn(Schedulers.computation())
                    // Ignore failure
                    .onErrorComplete()

    ///////////////////////////////////////////////////////////////////////////
    // Units
    ///////////////////////////////////////////////////////////////////////////

    private fun getFormattedPriceString(): String {
        val lastPrice = getLastBtcPrice(getFiatCurrency())
        val fiatSymbol = monetaryUtil.getCurrencySymbol(getFiatCurrency(), view.locale)
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
        val fiatBalance = getLastBtcPrice(strFiat) * (btcBalance / 1e8)

        return getFormattedCurrencyString(fiatBalance)
    }

    private fun getBchBalanceString(bchBalance: Long): String {
        var balance = monetaryUtil.getDisplayAmountWithFormatting(bchBalance)
        // Replace 0.0 with 0 to match web
        if (balance == "0.0") balance = "0"

        return "$balance ${getBchDisplayUnits()}"
    }

    private fun getBchFiatString(bchBalance: Long): String {
        val strFiat = getFiatCurrency()
        val fiatBalance = getLastBchPrice(strFiat) * (bchBalance / 1e8)

        return getFormattedCurrencyString(fiatBalance)
    }

    private fun getEthBalanceString(ethBalance: BigInteger): String {
        val number = DecimalFormat.getInstance().apply { maximumFractionDigits = 8 }
                .run { format(Convert.fromWei(BigDecimal(ethBalance), Convert.Unit.ETHER)) }

        return "$number ETH"
    }

    private fun getEthFiatString(ethBalance: BigInteger): String {
        val strFiat = getFiatCurrency()
        val fiatBalance = BigDecimal.valueOf(getLastEthPrice(strFiat))
                .multiply(Convert.fromWei(BigDecimal(ethBalance), Convert.Unit.ETHER))

        return getFormattedCurrencyString(fiatBalance.toDouble())
    }

    private fun getBtcPriceString(): String =
            getLastBtcPrice(getFiatCurrency()).run { getFormattedCurrencyString(this) }

    private fun getEthPriceString(): String =
            getLastEthPrice(getFiatCurrency()).run { getFormattedCurrencyString(this) }

    private fun getBchPriceString(): String =
            getLastBchPrice(getFiatCurrency()).run { getFormattedCurrencyString(this) }

    private fun getFormattedCurrencyString(price: Double) =
            "${getCurrencySymbol()}${monetaryUtil.getFiatFormat(getFiatCurrency()).format(price)}"

    private fun getCurrencySymbol() = monetaryUtil.getCurrencySymbol(getFiatCurrency(), view.locale)

    private fun getBtcDisplayUnits() = monetaryUtil.getBtcUnits()[getBtcUnitType()]

    private fun getBchDisplayUnits() = monetaryUtil.getBchUnits()[getBtcUnitType()]

    private fun getBtcUnitType() =
            prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)

    private fun getFiatCurrency() =
            prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

    private fun getLastBtcPrice(fiat: String) = exchangeRateFactory.getLastBtcPrice(fiat)

    private fun getLastEthPrice(fiat: String) = exchangeRateFactory.getLastEthPrice(fiat)

    private fun getLastBchPrice(fiat: String) = exchangeRateFactory.getLastBchPrice(fiat)

    companion object {

        @VisibleForTesting const val BITCOIN_CASH_ANNOUNCEMENT_DISMISSED =
                "BITCOIN_CASH_ANNOUNCEMENT_DISMISSED"

    }
}


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

    object Loading : PieChartsState()
    object Error : PieChartsState()

}

sealed class AssetPriceCardState(val currency: CryptoCurrencies) {

    data class Data(
            val priceString: String,
            val cryptoCurrency: CryptoCurrencies,
            @DrawableRes val icon: Int
    ) : AssetPriceCardState(cryptoCurrency)

    class Loading(val cryptoCurrency: CryptoCurrencies) : AssetPriceCardState(cryptoCurrency)
    class Error(val cryptoCurrency: CryptoCurrencies) : AssetPriceCardState(cryptoCurrency)

}