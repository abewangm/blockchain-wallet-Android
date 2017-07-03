package piuk.blockchain.android.ui.account.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.item_accounts_row_header.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.extensions.inflate

class CreateWalletDelegate<in T>(
        val listener: AccountHeadersListener
) : AdapterDelegate<List<T>> {

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
            HeaderViewHolder(parent.inflate(R.layout.item_accounts_row_header))

    override fun onBindViewHolder(
            items: List<T>,
            position: Int,
            holder: RecyclerView.ViewHolder,
            payloads: List<*>
    ) {
        val headerViewHolder = holder as HeaderViewHolder
        headerViewHolder.header.setText(R.string.wallets)
        headerViewHolder.itemView.setOnClickListener { listener.onCreateNewClicked() }
    }

    override fun isForViewType(items: List<T>, position: Int): Boolean {
        if (items[position] is AccountItem) {
            return (items[position] as AccountItem).type == AccountItem.TYPE_CREATE_NEW_WALLET_BUTTON
        } else {
            return false
        }
    }

    private class HeaderViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var header: TextView = itemView.header_name
    }

}