package piuk.blockchain.android.ui.shapeshift.inprogress

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import info.blockchain.wallet.shapeshift.data.Trade
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.util.annotations.Mockable
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Mockable
class TradeInProgressPresenter @Inject constructor(
        private val shapeShiftDataManager: ShapeShiftDataManager
) : BasePresenter<TradeInProgressView>() {

    override fun onViewReady() {
        // Set initial state
        onNoDeposit()

        // Poll for results
        Observable.interval(5, TimeUnit.SECONDS, Schedulers.io())
                .flatMap { shapeShiftDataManager.getTradeStatus(view.depositAddress) }
                .doOnNext { handleState(it.status) }
                .takeUntil { isInFinalState(it.status) }
                .compose(RxUtil.applySchedulersToObservable())
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(
                        {
                            // TODO: Update Metadata Entries when exchange is complete
                        },
                        {
                            Timber.e(it)
                        },
                        {
                            Timber.d("On Complete")
                        }
                )
    }

    private fun handleState(status: Trade.STATUS) {
        when (status) {
            Trade.STATUS.NO_DEPOSITS -> onNoDeposit()
            Trade.STATUS.RECEIVED -> onReceived()
            Trade.STATUS.COMPLETE -> onComplete()
            Trade.STATUS.FAILED, Trade.STATUS.RESOLVED -> onFailed()
        }
    }

    private fun onNoDeposit() {
        val state = TradeProgressUiState(
                R.string.shapeshift_sending_title,
                R.string.shapeshift_in_progress_explanation,
                R.drawable.shapeshift_progress_airplane,
                true,
                1
        )
        view.updateUi(state)
    }

    private fun onReceived() {
        val state = TradeProgressUiState(
                R.string.shapeshift_in_progress_title,
                R.string.shapeshift_in_progress_explanation,
                R.drawable.shapeshift_progress_exchange,
                true,
                2
        )
        view.updateUi(state)
    }

    private fun onComplete() {
        val state = TradeProgressUiState(
                R.string.shapeshift_complete_title,
                R.string.shapeshift_in_progress_explanation,
                R.drawable.shapeshift_progress_complete,
                true,
                3
        )
        view.updateUi(state)
    }

    private fun onFailed() {
        val state = TradeProgressUiState(
                R.string.shapeshift_failed_title,
                R.string.shapeshift_failed_explanation,
                R.drawable.shapeshift_progress_failed,
                false,
                0
        )
        view.updateUi(state)
    }

    private fun isInFinalState(status: Trade.STATUS) = when (status) {
        Trade.STATUS.NO_DEPOSITS, Trade.STATUS.RECEIVED -> false
        Trade.STATUS.COMPLETE, Trade.STATUS.FAILED, Trade.STATUS.RESOLVED -> true
    }

}

data class TradeProgressUiState(
        @StringRes val title: Int,
        @StringRes val message: Int,
        @DrawableRes val icon: Int,
        val showSteps: Boolean,
        val stepNumber: Int
)