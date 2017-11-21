package piuk.blockchain.android.data.shapeshift

import info.blockchain.wallet.shapeshift.ShapeShiftTrades
import info.blockchain.wallet.shapeshift.data.Trade
import piuk.blockchain.android.util.annotations.Mockable
import java.util.ArrayList

/**
 * A simple data store class to cache ShapeShift trade data
 */
@Mockable
class ShapeShiftDataStore {

    var data: ShapeShiftTrades? = null

}