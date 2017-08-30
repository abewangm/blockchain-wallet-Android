package piuk.blockchain.android.ui.send

import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import java.util.ArrayList
import javax.inject.Inject

class SendPresenter @Inject constructor(
        private val walletAccountHelper: WalletAccountHelper,
        private val payloadDataManager: PayloadDataManager
) : BasePresenter<SendView>() {

    override fun onViewReady() {
    }

    fun onContinue() {
    }

    fun onBitcoinChosen() {
    }

    fun onEtherChosen() {
    }

    fun clearReceivingAddress() {
    }

    fun clearContact() {
    }

    internal fun getAddressList(): List<ItemAccount> {
        val result = ArrayList<ItemAccount>()
        result.addAll(walletAccountHelper.getAccountItems())
        return result
    }

    fun selectDefaultSendingAccount() {
        view.setSendingAddress(getAddressList().get(getDefaultAccount()))
    }

    fun selectSendingAccount(accountPosition: Int) {

        val list = getAddressList()

        if(accountPosition >= 0) {
            view.setSendingAddress(list.get(accountPosition))
        } else {
            selectDefaultSendingAccount()
        }
    }

    internal fun getDefaultAccount(): Int {
        return getListIndexFromAccountIndex(payloadDataManager.defaultAccountIndex)
    }

    private fun getListIndexFromAccountIndex(accountIndex: Int): Int {
        return payloadDataManager.getPositionOfAccountFromActiveList(accountIndex)
    }
}