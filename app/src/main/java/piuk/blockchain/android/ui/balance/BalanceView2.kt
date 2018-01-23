//package piuk.blockchain.android.ui.balance
//
//import android.support.annotation.StringRes
//import piuk.blockchain.android.data.contacts.models.ContactTransactionDisplayModel
//import piuk.blockchain.android.data.currency.CryptoCurrencies
//import piuk.blockchain.android.ui.account.ItemAccount
//import piuk.blockchain.android.ui.base.UiState
//import piuk.blockchain.android.ui.base.View
//import piuk.blockchain.android.ui.customviews.ToastCustom
//import piuk.blockchain.android.util.MonetaryUtil
//
//interface BalanceView2 : View {
//
//    val isContactsEnabled: Boolean
//
//    val shouldShowBuy: Boolean
//
//    fun onTransactionsUpdated(displayObjects: List<Any>)
//
//    fun onTotalBalanceUpdated(balance: String)
//
//    fun onExchangeRateUpdated(btcExchangeRate: Double, ethExchangeRate: Double, isBtc: Boolean, txNoteMap: MutableMap<String, String>)
//
//    fun showProgressDialog()
//
//    fun dismissProgressDialog()
//
//    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)
//
//    fun onAccountsUpdated(
//            accounts: List<ItemAccount>,
//            lastBtcPrice: Double,
//            fiat: String,
//            monetaryUtil: MonetaryUtil,
//            isBtc: Boolean
//    )
//
//    fun setUiState(@UiState.UiStateDef uiState: Int)
//
//    fun onViewTypeChanged(isBtc: Boolean, btcFormat: Int)
//
//    fun startBuyActivity()
//
//    fun startReceiveFragment()
//
//    fun updateSelectedCurrency(cryptoCurrency: CryptoCurrencies)
//
//    fun hideAccountSpinner()
//
//    fun showAccountSpinner()
//
//    fun updateAccountList(accountList: List<ItemAccount>)
//}