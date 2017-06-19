package piuk.blockchain.android.ui.balance

import android.support.annotation.StringRes
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.customviews.ToastCustom

interface BalanceView : View {

    fun getIfContactsEnabled(): Boolean

    fun onTransactionsUpdated(displayObjects: List<Any>)

    fun onExchangeRateUpdated()

    fun showProgressDialog()

    fun dismissProgressDialog()

    fun setShowRefreshing(showRefreshing: Boolean)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

}