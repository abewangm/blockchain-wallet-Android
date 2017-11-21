package piuk.blockchain.android.ui.shapeshift.overview

import info.blockchain.wallet.shapeshift.data.Trade
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.util.annotations.Mockable
import timber.log.Timber
import javax.inject.Inject

@Mockable
class ShapeShiftPresenter @Inject constructor(
        private val shapeShiftDataManager: ShapeShiftDataManager,
        private val payloadDataManager: PayloadDataManager
) : BasePresenter<ShapeShiftView>() {

    override fun onViewReady() {
        // 1. Show loading on subscribe
        // 2. If no trades found, show empty, redirect to new trade
        // 3. If trades found, map and display data + link for new trade
        // 4. Display error state + retry if necessary

        // TODO: Load shapeshift data
        // TODO: Handle second password scenario
        shapeShiftDataManager.initialiseTrades(payloadDataManager.wallet.hdWallets[0].masterKey)
                .doOnError(Timber::e)
                .flatMap { shapeShiftDataManager.getTradesList() }
                .doOnSubscribe { view.onStateUpdated(ShapeShiftState.Loading) }
                // TODO: Remove me
                .doOnComplete { view.onStateUpdated(ShapeShiftState.Empty) }
                // TODO: Render data here
                .doOnNext { it.forEach { Timber.d(it.toJson()) } }
                .subscribe(
                        {
                            // Ignorable
                        },
                        {
                            Timber.e(it)
                            view.onStateUpdated(ShapeShiftState.Error)
                        }
                )
    }

    internal fun onRetryPressed() {
        onViewReady()
    }

}

sealed class ShapeShiftState {

    class Data(val trades: List<Trade>) : ShapeShiftState()
    object Empty : ShapeShiftState()
    object Error : ShapeShiftState()
    object Loading : ShapeShiftState()

}