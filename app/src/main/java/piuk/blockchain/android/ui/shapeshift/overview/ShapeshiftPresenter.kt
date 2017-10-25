package piuk.blockchain.android.ui.shapeshift.overview

import io.reactivex.Observable
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver
import piuk.blockchain.android.ui.base.BasePresenter
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ShapeShiftPresenter @Inject constructor(
        // TODO: Pass dependencies here
) : BasePresenter<ShapeShiftView>() {

    override fun onViewReady() {
        // 1. Show loading on subscribe
        // 2. If no trades found, show empty, redirect to new trade
        // 3. If trades found, map and display data + link for new trade
        // 4. Display error state + retry if necessary

        // TODO: Load shapeshift data
        Observable.timer(1500, TimeUnit.MILLISECONDS)
                .doOnSubscribe { view.onStateUpdated(ShapeShiftState.Loading) }
                .doOnComplete { view.onStateUpdated(ShapeShiftState.Empty) }
                .subscribe { IgnorableDefaultObserver<Any>() }
    }

    internal fun onRetryPressed() {
        onViewReady()
    }

}

sealed class ShapeShiftState {

    class Data(
            // TODO: Pass necessary data here
    ) : ShapeShiftState()

    object Empty : ShapeShiftState()
    object Error : ShapeShiftState()
    object Loading : ShapeShiftState()

}