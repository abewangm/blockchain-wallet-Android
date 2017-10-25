package piuk.blockchain.android.data.shapeshift

import info.blockchain.wallet.shapeshift.ShapeShiftApi
import info.blockchain.wallet.shapeshift.ShapeShiftPairs
import info.blockchain.wallet.shapeshift.data.MarketInfo
import info.blockchain.wallet.shapeshift.data.Quote
import info.blockchain.wallet.shapeshift.data.QuoteRequest
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse
import io.reactivex.Observable
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxPinning
import piuk.blockchain.android.data.rxjava.RxUtil

class ShapeShiftDataManager(private val shapeShiftApi: ShapeShiftApi, rxBus: RxBus) {

    private val rxPinning = RxPinning(rxBus)

    fun getRate(coinPairings: CoinPairings): Observable<MarketInfo> =
            rxPinning.call<MarketInfo> { shapeShiftApi.getRate(coinPairings.pairCode) }
                    .compose(RxUtil.applySchedulersToObservable())

    fun getQuote(quoteRequest: QuoteRequest): Observable<Quote> =
            rxPinning.call<Quote> { shapeShiftApi.getQuote(quoteRequest).map { it.wrapper } }
                    .compose(RxUtil.applySchedulersToObservable())

    fun getApproximateQuote(request: QuoteRequest): Observable<Quote> =
            rxPinning.call<Quote> { shapeShiftApi.getApproximateQuote(request).map { it.wrapper } }
                    .compose(RxUtil.applySchedulersToObservable())

    fun getTradeStatus(address: String): Observable<TradeStatusResponse> =
            rxPinning.call<TradeStatusResponse> { shapeShiftApi.getTradeStatus(address) }
                    .compose(RxUtil.applySchedulersToObservable())

}

/**
 * For strict type checking and convenience.
 */
enum class CoinPairings(val pairCode: String) {
    BTC_TO_ETH(ShapeShiftPairs.BTC_ETH),
    ETH_TO_BTC(ShapeShiftPairs.BTC_ETH)
}