package piuk.blockchain.android.data.shapeshift

import info.blockchain.wallet.shapeshift.ShapeShiftApi
import info.blockchain.wallet.shapeshift.ShapeShiftPairs
import info.blockchain.wallet.shapeshift.ShapeShiftTrades
import info.blockchain.wallet.shapeshift.data.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import piuk.blockchain.android.data.metadata.MetadataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxPinning
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.shapeshift.datastore.ShapeShiftDataStore
import piuk.blockchain.android.data.stores.Either
import piuk.blockchain.android.data.stores.Optional
import piuk.blockchain.android.ui.shapeshift.models.CoinPairings
import piuk.blockchain.android.util.annotations.Mockable
import piuk.blockchain.android.util.annotations.WebRequest

@Mockable
class ShapeShiftDataManager(
        private val shapeShiftApi: ShapeShiftApi,
        private val shapeShiftDataStore: ShapeShiftDataStore,
        private val metadataManager: MetadataManager,
        rxBus: RxBus
) {

    private val rxPinning = RxPinning(rxBus)

    /**
     * Must be called to initialize the ShapeShift trade metadata information.
     *
     * @param metadataNode The metadata node for SS, obtained through
     * [piuk.blockchain.android.data.payload.PayloadDataManager.getMetadataNodeFactory]
     * @return A [Completable] object
     */
    fun initShapeshiftTradeData(): Completable =
            rxPinning.call {
                fetchOrCreateShapeShiftTradeData()
                        .flatMapCompletable {
                            shapeShiftDataStore.tradeData = it.first
                            if (it.second) {
                                save()
                            } else {
                                Completable.complete()
                            }
                        }
                        .compose(RxUtil.applySchedulersToCompletable())
            }

    /**
     * Returns the US state that the user has selected and stored in metadata, contained within an
     * [Optional] wrapper. If [Optional.None], no State has been set. Will throw an
     * [IllegalArgumentException] if [ShapeShiftTrades] has not been initialized.
     *
     * @return An [Observable] containing an [Optional]
     */
    fun getState(): Observable<Optional<State>> {
        shapeShiftDataStore.tradeData?.run {
            return when (usState) {
                null -> Observable.just(Optional.None)
                else -> Observable.just(Optional.Some(usState))
            }
        }

        throw IllegalStateException("ShapeShiftTrades not initialized")
    }

    /**
     * Sets the user's selected State and saves it to metadata. Can be null to clear the saved
     * State. Will throw an [IllegalArgumentException] if [ShapeShiftTrades] has not been
     * initialized.
     *
     * @return A [Completable] object
     */
    fun setState(state: State?): Completable {
        shapeShiftDataStore.tradeData?.run {
            usState = state
            return rxPinning.call { Completable.fromCallable { save() } }
                    .compose(RxUtil.applySchedulersToCompletable())
        }

        throw IllegalStateException("ShapeShiftTrades not initialized")
    }

    /**
     * Returns a list of [Trade] objects previously fetched from metadata. Note that this does
     * not refresh the list. Will throw an [IllegalArgumentException] if [ShapeShiftTrades] has
     * not been initialized.
     *
     * @return An [Observable] wrapping a list of [Trade] objects
     */
    fun getTradesList(): Observable<List<Trade>> {
        shapeShiftDataStore.tradeData?.run { return Observable.just(trades) }

        throw IllegalStateException("ShapeShiftTrades not initialized")
    }

    /**
     * Returns a [Trade] object if found in the current list of [Trade] objects pulled from
     * metadata. Will throw an [IllegalArgumentException] if [ShapeShiftTrades] has not been
     * initialized.
     *
     * @param depositAddress The deposit address of the [Trade] you wish to find
     * @return A [Single] wrapping a [Trade]
     */
    fun findTrade(depositAddress: String): Single<Trade> {
        shapeShiftDataStore.tradeData?.run {
            val foundTrade = trades.firstOrNull { it.quote.deposit == depositAddress }
            return if (foundTrade == null) {
                Single.error(Throwable("Trade not found"))
            } else {
                Single.just(foundTrade)
            }
        }

        throw IllegalStateException("ShapeShiftTrades not initialized")
    }

    /**
     * Adds a new [Trade] object to the list of Trades and then saves it to the metadata service.
     * Will revert the status of the Trades list if the call fails. Will throw an
     * [IllegalArgumentException] if [ShapeShiftTrades] has not been initialized.
     *
     * @param trade The [Trade] object to be added to the list of Trades
     * @return A [Completable] object
     */
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

    /**
     * For development purposes only! Clears all [Trade] objects from the user's metadata and
     * stores an empty list instead. Will throw an [IllegalArgumentException] if [ShapeShiftTrades]
     * has not been initialized.
     *
     * @return A [Completable] object
     */
    fun clearAllTrades(): Completable {
        shapeShiftDataStore.tradeData?.run {
            trades?.clear()
            return rxPinning.call { Completable.fromCallable { save() } }
                    .compose(RxUtil.applySchedulersToCompletable())
        }

        throw IllegalStateException("ShapeShiftTrades not initialized")
    }

    /**
     * Takes a [Trade] object, replaces the current version of it stored in metadata and then saves
     * it. Will return an error if the [Trade] is not found. Will throw an
     * [IllegalArgumentException] if [ShapeShiftTrades] has not been initialized.
     *
     * @param trade The [Trade] object to be updated
     * @return A [Completable] object
     */
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

    /**
     * Gets the [TradeStatusResponse] for a given [Trade] deposit address. Note that this won't
     * return an invalid [TradeStatusResponse] if the server returned an error response: it will
     * fail instead.
     *
     * @param depositAddress The [Trade] deposit address
     * @return An [Observable] wrapping a [TradeStatusResponse] object.
     */
    fun getTradeStatus(depositAddress: String): Observable<TradeStatusResponse> =
            rxPinning.call<TradeStatusResponse> {
                shapeShiftApi.getTradeStatus(depositAddress)
                        .flatMap {
                            if (it.error != null && it.status == null) {
                                Observable.error(Throwable(it.error))
                            } else {
                                Observable.just(it)
                            }
                        }
            }.compose(RxUtil.applySchedulersToObservable())

    /**
     * Gets the [TradeStatusResponse] for a given [Trade] deposit address and returns it along with the original trade.
     * Note that this won't
     * return an invalid [TradeStatusResponse] if the server returned an error response.
     *
     * @param tradeMetadata The [Trade] data stored in kv-store
     * @return An [Observable] wrapping a [Pair<Trade, TradeStatusResponse>] object.
     */
    fun getTradeStatusPair(tradeMetadata: Trade): Observable<TradeStatusPair> =
            rxPinning.call<TradeStatusPair> {
                shapeShiftApi.getTradeStatus(tradeMetadata.quote.deposit)
                        .map { TradeStatusPair(tradeMetadata, it) }
            }.compose(RxUtil.applySchedulersToObservable())

    /**
     * Gets the current approximate [MarketInfo] for a given [CoinPairings] object.
     *
     * @param coinPairings A [CoinPairings] wrapper object for a [ShapeShiftPairs]
     * @return An [Observable] wrapping the most recent [MarketInfo]
     */
    fun getRate(coinPairings: CoinPairings): Observable<MarketInfo> =
            rxPinning.call<MarketInfo> { shapeShiftApi.getRate(coinPairings.pairCode) }
                    .compose(RxUtil.applySchedulersToObservable())

    /**
     * Returns an [Either] where the left object is an error String, or a valid [Quote] object for
     * the given [QuoteRequest].
     *
     * @param quoteRequest A valid [QuoteRequest] object
     * @return An [Observable] wrapping an [Either]
     */
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

    /**
     * Returns an [Either] where the left object is an error String, or a valid [Quote] object for
     * the given [QuoteRequest]. This returns only an approximate quote.
     *
     * @param quoteRequest A valid [QuoteRequest] object
     * @return An [Observable] wrapping an [Either]
     */
    fun getApproximateQuote(quoteRequest: QuoteRequest): Observable<Either<String, Quote>> =
            rxPinning.call<Either<String, Quote>> {
                shapeShiftApi.getApproximateQuote(quoteRequest).map {
                    when {
                        it.error != null -> Either.Left<String>(it.error)
                        else -> Either.Right<Quote>(it.wrapper)
                    }
                }
            }.compose(RxUtil.applySchedulersToObservable())

    /**
     * Fetches the current trade metadata from the web, or else creates a new metadata entry
     * containing an empty list of [Trade] objects.
     *
     * @param metadataHDNode
     * @return A [ShapeShiftTrades] object wrapping trades functionality
     * @throws Exception Can throw various exceptions if the key is incorrect, the server is down
     * etc
     */
    @WebRequest
    @Throws(Exception::class)
    private fun fetchOrCreateShapeShiftTradeData() =
            metadataManager.fetchMetadata(ShapeShiftTrades.METADATA_TYPE_EXTERNAL)
                    .compose(RxUtil.applySchedulersToObservable())
                    .map { optional ->

                        val json = optional.orNull()
                        var shapeShiftData = ShapeShiftTrades.load(json)
                        var needsSave = false

                        if (shapeShiftData == null) {
                            shapeShiftData = ShapeShiftTrades()
                            needsSave = true
                        }

                        Pair(shapeShiftData, needsSave)
                    }

    fun save(): Completable {
        shapeShiftDataStore.tradeData.run {
            metadataManager.saveToMetadata(shapeShiftDataStore.tradeData!!.toJson(), ShapeShiftTrades.METADATA_TYPE_EXTERNAL)
        }

        throw IllegalStateException("ShapeShiftTrades not initialized")
    }

    data class TradeStatusPair(val tradeMetadata: Trade, val tradeStatusResponse: TradeStatusResponse)
}