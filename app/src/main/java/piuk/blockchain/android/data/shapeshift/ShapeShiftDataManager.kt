package piuk.blockchain.android.data.shapeshift

import info.blockchain.wallet.shapeshift.ShapeShiftApi
import info.blockchain.wallet.shapeshift.ShapeShiftPairs
import info.blockchain.wallet.shapeshift.ShapeShiftTrades
import info.blockchain.wallet.shapeshift.data.*
import info.blockchain.wallet.util.MetadataUtil
import io.reactivex.Completable
import io.reactivex.Observable
import org.bitcoinj.crypto.DeterministicKey
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxPinning
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.shapeshift.datastore.ShapeShiftDataStore
import piuk.blockchain.android.data.stores.Either
import piuk.blockchain.android.util.annotations.Mockable
import piuk.blockchain.android.util.annotations.WebRequest

@Mockable
class ShapeShiftDataManager(
        private val shapeShiftApi: ShapeShiftApi,
        private val shapeShiftDataStore: ShapeShiftDataStore,
        rxBus: RxBus
) {

    private val rxPinning = RxPinning(rxBus)

    /**
     * Must be called to initialize the ShapeShift trade metadata information.
     *
     * @param masterKey The wallet's master key [info.blockchain.wallet.bip44.HDWallet.getMasterKey]
     * @return A [Completable] object
     */
    fun initialiseTrades(masterKey: DeterministicKey): Observable<ShapeShiftTrades> =
            rxPinning.call<ShapeShiftTrades> {
                Observable.fromCallable { fetchOrCreateMetadataNode(masterKey) }
                        .compose(RxUtil.applySchedulersToObservable())
            }

    fun getTradesList(): Observable<List<Trade>> {
        shapeShiftDataStore.tradeData?.run { return Observable.just(trades) }

        throw IllegalStateException("ShapeShiftTrades not initialized")
    }

    fun findTrade(withdrawalAddress: String): Observable<Trade> {
        shapeShiftDataStore.tradeData?.run {
            val foundTrade = trades.find { it.quote.withdrawal == withdrawalAddress }
            return if (foundTrade == null) {
                Observable.error(Throwable("Trade not found"))
            } else {
                Observable.just(foundTrade)
            }
        }

        throw IllegalStateException("ShapeShiftTrades not initialized")
    }

    fun addTradeToList(trade: Trade): Completable {
        shapeShiftDataStore.tradeData?.run {
            trades.add(trade)
            return rxPinning.call { Completable.fromCallable { save() } }
                    // Reset state on failure
                    .doOnError { trades.remove(trade) }
                    .compose(RxUtil.applySchedulersToCompletable())
        }

        throw IllegalStateException("ShapeShiftTrades not initialized")
    }

    fun updateTrade(trade: Trade): Completable {
        shapeShiftDataStore.tradeData?.run {
            val foundTrade = trades.find { it.quote.orderId == trade.quote.orderId }
            return if (foundTrade == null) {
                Completable.error(Throwable("Trade not found"))
            } else {
                trades.remove(foundTrade)
                trades.add(trade)
                rxPinning.call { Completable.fromCallable { save() } }
                        // Reset state on failure
                        .doOnError {
                            trades.remove(trade)
                            trades.add(foundTrade)
                        }
                        .compose(RxUtil.applySchedulersToCompletable())
            }
        }

        throw IllegalStateException("ShapeShiftTrades not initialized")
    }

    fun getTradeStatus(address: String): Observable<TradeStatusResponse> =
            rxPinning.call<TradeStatusResponse> { shapeShiftApi.getTradeStatus(address) }
                    .compose(RxUtil.applySchedulersToObservable())

    fun getRate(coinPairings: CoinPairings): Observable<MarketInfo> =
            rxPinning.call<MarketInfo> { shapeShiftApi.getRate(coinPairings.pairCode) }
                    .compose(RxUtil.applySchedulersToObservable())

    fun getQuote(quoteRequest: QuoteRequest): Observable<Either<String, Quote>> =
            rxPinning.call<Either<String, Quote>> {
                shapeShiftApi.getQuote(quoteRequest)
                        .map {
                            when {
                                it.error != null -> Either.Left<String>(it.error)
                                else -> Either.Right<Quote>(it.wrapper)
                            }
                        }
            }.compose(RxUtil.applySchedulersToObservable())

    fun getApproximateQuote(request: QuoteRequest): Observable<Either<String, Quote>> =
            rxPinning.call<Either<String, Quote>> {
                shapeShiftApi.getApproximateQuote(request).map {
                    when {
                        it.error != null -> Either.Left<String>(it.error)
                        else -> Either.Right<Quote>(it.wrapper)
                    }
                }
            }.compose(RxUtil.applySchedulersToObservable())

    /**
     * TODO: This is currently untestable, metadata needs a complete rethink as to how it works.
     *
     * Fetches the current trade metadata from the web, or else creates a new metadata entry
     * containing an empty list of [Trade] objects.
     *
     * @param masterKey The wallet master key, from [info.blockchain.wallet.bip44.HDWallet.getMasterKey]
     * @return A [ShapeShiftTrades] object wrapping trades functionality
     * @throws Exception Can throw various exceptions if the key is incorrect, the server is down
     * etc
     */
    @WebRequest
    @Throws(Exception::class)
    private fun fetchOrCreateMetadataNode(masterKey: DeterministicKey): ShapeShiftTrades {
        shapeShiftDataStore.tradeData = ShapeShiftTrades.load(MetadataUtil.deriveMetadataNode(masterKey))

        if (shapeShiftDataStore.tradeData == null) {
            shapeShiftDataStore.tradeData = ShapeShiftTrades(masterKey).apply { save() }
        }

        return shapeShiftDataStore.tradeData!!
    }

}

/**
 * For strict type checking and convenience.
 */
enum class CoinPairings(val pairCode: String) {
    BTC_TO_ETH(ShapeShiftPairs.BTC_ETH),
    ETH_TO_BTC(ShapeShiftPairs.ETH_BTC)
}