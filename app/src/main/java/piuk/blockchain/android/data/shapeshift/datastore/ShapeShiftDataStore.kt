package piuk.blockchain.android.data.shapeshift.datastore

import info.blockchain.wallet.shapeshift.ShapeShiftTrades
import piuk.blockchain.android.util.annotations.Mockable

/**
 * A simple class for persisting ShapeShift Trade data.
 */
@Mockable
class ShapeShiftDataStore {

    var tradeData: ShapeShiftTrades? = null

}