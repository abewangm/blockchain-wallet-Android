package piuk.blockchain.android.ui.exchange

import piuk.blockchain.android.ui.base.BasePresenter
import javax.inject.Inject

class ShapeShiftPresenter @Inject constructor(
        // TODO: Pass dependencies here
) : BasePresenter<ShapeShiftView>() {

    override fun onViewReady() {
        // TODO: Load shapeshift data

        // 1. Show loading on subscribe
        // 2. If no trades found, show empty, redirect to new trade
        // 3. If trades found, map and display data + link for new trade
        // 4. Display error state + retry if necessary
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