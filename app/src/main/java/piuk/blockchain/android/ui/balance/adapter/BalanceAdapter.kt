package piuk.blockchain.android.ui.balance.adapter

import android.app.Activity
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.ListDelegationAdapter


class BalanceAdapter(
        activity: Activity,
        items: List<Any>
) : ListDelegationAdapter<List<Any>>(AdapterDelegatesManager(), items) {

    init {
        delegatesManager.addAdapterDelegate(HeaderDelegate())
        delegatesManager.addAdapterDelegate(TransactionSummaryDelegate())
        delegatesManager.addAdapterDelegate(FctxDelegate(activity))
    }

}