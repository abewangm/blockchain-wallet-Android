package piuk.blockchain.android.ui.exchange.overview

import piuk.blockchain.android.ui.base.View

interface ShapeShiftView : View {

    fun onStateUpdated(shapeshiftState: ShapeShiftState)

}