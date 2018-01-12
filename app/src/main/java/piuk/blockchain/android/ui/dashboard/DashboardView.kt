package piuk.blockchain.android.ui.dashboard

import android.support.annotation.StringRes
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.customviews.ToastCustom
import java.util.*

interface DashboardView : View {

    val shouldShowBuy: Boolean

    val locale: Locale

    fun updatePieChartState(chartsState: PieChartsState)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun notifyItemAdded(displayItems: MutableList<Any>, position: Int)

    fun notifyItemRemoved(displayItems: MutableList<Any>, position: Int)

    fun notifyItemUpdated(displayItems: MutableList<Any>, position: Int)

    fun startBuyActivity()

    fun startBitcoinCashReceive()

    fun scrollToTop()
}