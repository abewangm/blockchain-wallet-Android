package piuk.blockchain.android.ui.send

import piuk.blockchain.android.R
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import javax.inject.Inject

class SendPresenterNew @Inject constructor(
        private val walletAccountHelper: WalletAccountHelper,
        private val payloadDataManager: PayloadDataManager,
        private val currencyState: CurrencyState,
        private val ethDataManager: EthDataManager
) : BasePresenter<SendViewNew>() {

    override fun onViewReady() {
    }

    fun onContinue() {
    }

    internal fun resetAccountList() {
        getAddressList()
        selectDefaultSendingAccount()
    }

    fun onBitcoinChosen() {
        currencyState.cryptoCurrency = CryptoCurrencies.BTC
        resetAccountList()
    }

    fun onEtherChosen() {
        currencyState.cryptoCurrency = CryptoCurrencies.ETHER
        resetAccountList()
    }

    fun clearReceivingAddress() {
    }

    fun clearContact() {
    }

    internal fun getAddressList(): List<ItemAccount> {
        val list = walletAccountHelper.getAccountItems()

        if(list.size == 1) {
            view.hideReceivingDropdown()
            view.hideSendingFieldDropdown()
            setReceiveHint(list.size)
        } else {
            view.showSendingFieldDropdown()
            view.showReceivingDropdown()
            setReceiveHint(list.size)
        }

        return list
    }

    fun setReceiveHint(accountsCount: Int) {

        var hint: Int

        if(accountsCount > 1) {
            when (currencyState.cryptoCurrency) {
                CryptoCurrencies.BTC -> hint = R.string.to_field_helper
                else -> hint = R.string.eth_to_field_helper
            }
        } else {
            when (currencyState.cryptoCurrency) {
                CryptoCurrencies.BTC -> hint = R.string.to_field_helper_no_dropdown
                else -> hint = R.string.eth_to_field_helper_no_dropdown
            }
        }

        view.setReceivingHint(hint)
    }

    fun selectDefaultSendingAccount() {
        val accountItem = walletAccountHelper.getDefaultAccount()
        view.setSendingAddress(accountItem)
    }

    fun selectSendingBtcAccount(accountPosition: Int) {

        if(accountPosition >= 0) {
            view.setSendingAddress(getAddressList().get(accountPosition))
        } else {
            selectDefaultSendingAccount()
        }
    }

    internal fun getDefaultBtcAccount(): Int {
        return getListIndexFromAccountIndex(payloadDataManager.defaultAccountIndex)
    }

    internal fun getListIndexFromAccountIndex(accountIndex: Int): Int {
        return payloadDataManager.getPositionOfAccountFromActiveList(accountIndex)
    }
}