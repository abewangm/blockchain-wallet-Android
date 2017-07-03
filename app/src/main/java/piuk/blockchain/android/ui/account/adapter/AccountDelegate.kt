package piuk.blockchain.android.ui.account.adapter

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.item_accounts_row.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.account.AccountItem
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.extensions.gone
import piuk.blockchain.android.util.extensions.inflate
import piuk.blockchain.android.util.extensions.visible

class AccountDelegate<in T>(
        val listener: AccountHeadersListener
) : AdapterDelegate<T> {

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
            AccountViewHolder(parent.inflate(R.layout.item_accounts_row))

    override fun onBindViewHolder(
            items: List<T>,
            position: Int,
            holder: RecyclerView.ViewHolder,
            payloads: List<*>
    ) {
        val accountViewHolder = holder as AccountViewHolder

        val accountItem = items[position] as AccountItem

        // Normal account view
        accountViewHolder.itemView.setOnClickListener {
            listener.onAccountClicked(accountItem.correctedPosition)
        }

        accountViewHolder.title.text = accountItem.label

        if (!accountItem.address.isNullOrEmpty()) {
            accountViewHolder.address.apply {
                visible()
                text = accountItem.address
            }
        } else {
            accountViewHolder.address.gone()
        }

        if (accountItem.isArchived) {
            accountViewHolder.amount.apply {
                setText(R.string.archived_label)
                setTextColor(
                        ContextCompat.getColor(
                                accountViewHolder.itemView.context,
                                R.color.product_gray_transferred
                        ))
            }
        } else {
            accountViewHolder.amount.apply {
                text = accountItem.amount
                setTextColor(
                        ContextCompat.getColor(
                                accountViewHolder.itemView.context,
                                R.color.product_green_medium
                        ))
            }
        }

        if (accountItem.isWatchOnly) {
            accountViewHolder.tag.apply {
                setText(R.string.watch_only)
                setTextColor(
                        ContextCompat.getColor(
                                accountViewHolder.itemView.context,
                                R.color.product_red_medium
                        ))
            }
        }

        if (accountItem.isDefault) {
            accountViewHolder.tag.apply {
                setText(R.string.default_label)
                setTextColor(
                        ContextCompat.getColor(
                                accountViewHolder.itemView.context,
                                R.color.product_gray_transferred
                        ))
            }
        }

        if (!accountItem.isWatchOnly && !accountItem.isDefault) {
            accountViewHolder.tag.gone()
        } else {
            accountViewHolder.tag.visible()
        }
    }

    override fun isForViewType(items: List<T>, position: Int): Boolean {
        if (items[position] is AccountItem) {
            return (items[position] as AccountItem).type == AccountItem.TYPE_ACCOUNT
        } else {
            return false
        }
    }

    private class AccountViewHolder internal constructor(
            itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        internal var title: TextView = itemView.my_account_row_label
        internal var address: TextView = itemView.my_account_row_address
        internal var amount: TextView = itemView.my_account_row_amount
        internal var tag: TextView = itemView.my_account_row_tag
    }

}