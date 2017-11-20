package piuk.blockchain.android.ui.shapeshift.confirmation

import android.support.annotation.StringRes
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.shapeshift.models.ShapeShiftData

interface ShapeShiftConfirmationView : View {

    val shapeShiftData: ShapeShiftData

    fun showToast(message: Int, toastType: String)

    fun finishPage()

    fun showProgressDialog(@StringRes message: Int)

    fun dismissProgressDialog()

    fun setButtonState(enabled: Boolean)

    fun updateCounter(timeRemaining: String)

    fun updateDeposit(label: String, amount: String)

    fun updateReceive(label: String, amount: String)

    fun updateExchangeRate(exchangeRate: String)

    fun updateTransactionFee(displayString: String)

    fun updateNetworkFee(displayString: String)

    fun showSecondPasswordDialog()

}