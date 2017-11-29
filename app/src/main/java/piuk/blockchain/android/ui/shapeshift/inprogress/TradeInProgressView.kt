package piuk.blockchain.android.ui.shapeshift.inprogress

import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.shapeshift.models.TradeProgressUiState

interface TradeInProgressView : View {

    val depositAddress: String

    fun updateUi(uiState: TradeProgressUiState)

}