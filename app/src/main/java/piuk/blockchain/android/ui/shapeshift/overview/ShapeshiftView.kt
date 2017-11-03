package piuk.blockchain.android.ui.shapeshift.overview

import piuk.blockchain.android.ui.base.View

interface ShapeShiftView : View {

    fun onStateUpdated(shapeshiftState: ShapeShiftState)

}