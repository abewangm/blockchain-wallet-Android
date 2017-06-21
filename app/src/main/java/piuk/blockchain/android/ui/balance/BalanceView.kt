package piuk.blockchain.android.ui.balance

import android.support.annotation.StringRes
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.UiState
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.MonetaryUtil

interface BalanceView : View {

    fun getIfContactsEnabled(): Boolean

    fun onTransactionsUpdated(displayObjects: List<Any>)

    fun onTotalBalanceUpdated(balance: String)

    fun onExchangeRateUpdated(exchangeRate: Double, isBtc: Boolean)

    fun showProgressDialog()

    fun dismissProgressDialog()

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun onAccountsUpdated(
            accounts: List<ItemAccount>,
            lastPrice: Double,
            fiat: String,
            monetaryUtil: MonetaryUtil,
            isBtc: Boolean
    )

    fun setUiState(@UiState.UiStateDef uiState: Int)

    fun onViewTypeChanged(isBtc: Boolean)

}