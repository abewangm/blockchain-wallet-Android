package piuk.blockchain.android.ui.send

import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.View

interface SendView : View {

    fun setSendingAddress(get: ItemAccount)

}
