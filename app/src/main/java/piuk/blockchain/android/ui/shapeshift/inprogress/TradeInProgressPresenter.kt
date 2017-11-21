package piuk.blockchain.android.ui.shapeshift.inprogress

import piuk.blockchain.android.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.util.annotations.Mockable
import javax.inject.Inject

@Mockable
class TradeInProgressPresenter @Inject constructor(
        private val shapeShiftDataManager: ShapeShiftDataManager
): BasePresenter<TradeInProgressView>() {


    override fun onViewReady() {

    }

}