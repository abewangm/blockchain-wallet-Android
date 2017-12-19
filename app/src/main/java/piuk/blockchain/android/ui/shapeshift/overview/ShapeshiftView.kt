package piuk.blockchain.android.ui.shapeshift.overview

import info.blockchain.wallet.shapeshift.data.Trade
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse
import piuk.blockchain.android.ui.base.View

interface ShapeShiftView : View {

    fun onStateUpdated(shapeshiftState: ShapeShiftState)

    fun onTradeUpdate(trade: Trade, tradeResponse: TradeStatusResponse)

    fun onExchangeRateUpdated(btcExchangeRate: Double, ethExchangeRate: Double, isBtc: Boolean)

    fun onViewTypeChanged(isBtc: Boolean, btcFormat: Int)

    fun showStateSelection()
}