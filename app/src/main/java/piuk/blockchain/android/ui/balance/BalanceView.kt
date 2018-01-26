package piuk.blockchain.android.ui.balance

import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.UiState
import piuk.blockchain.android.ui.base.View

interface BalanceView : View {

    fun setupAccountsAdapter(accountsList: List<ItemAccount>)

    fun setupTxFeedAdapter(isCrypto: Boolean)

    fun updateTransactionDataSet(isCrypto: Boolean, displayObjects: List<Any>)

    fun updateBalanceHeader(balance: String)

    fun updateAccountsDataSet(accountsList: List<ItemAccount>)

    fun updateSelectedCurrency(cryptoCurrency: CryptoCurrencies)

    fun selectDefaultAccount()

    fun setUiState(@UiState.UiStateDef uiState: Int)

    fun getCurrentAccountPosition(): Int?

    fun updateTransactionValueType(showCrypto: Boolean)

    fun generateLauncherShortcuts()

    fun startReceiveFragmentBtc()

    fun startBuyActivity()
}