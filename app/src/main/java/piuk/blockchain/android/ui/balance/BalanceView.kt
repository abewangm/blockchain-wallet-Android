package piuk.blockchain.android.ui.balance

import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.UiState
import piuk.blockchain.android.ui.base.View

interface BalanceView : View {

    fun setupAccountsAdapter(accountsList: List<ItemAccount>)

    fun setupTxFeedAdapter(isCrypto: Boolean)
//                            btcExchangeRate: Double,
//                           ethExchangeRate: Double,
//                           isBtc: Boolean,
//                           txNoteMap: MutableMap<String, String>)

    fun updateTransactionDataSet(isCrypto: Boolean, displayObjects: List<Any>)

    fun updateBalanceHeader(balance: String)

    fun updateAccountsDataSet(accountsList: List<ItemAccount>)

    fun updateSelectedCurrency(cryptoCurrency: CryptoCurrencies)

    fun selectDefaultAccount()

    fun setUiState(@UiState.UiStateDef uiState: Int)


//    val isContactsEnabled: Boolean
//
//    val shouldShowBuy: Boolean
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
//    fun startBuyActivity()
//
//    fun startReceiveFragment()
//
//
//    fun hideAccountSpinner()
//
//    fun showAccountSpinner()
//
//    fun updateAccountList(accountList: List<ItemAccount>)

    fun getCurrentAccountPosition(): Int?

    fun updateTransactionValueType(showCrypto: Boolean)


}