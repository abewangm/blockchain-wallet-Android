package piuk.blockchain.android.ui.dashboard.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.android.synthetic.main.item_asset_price_card.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.AssetPriceCardState
import piuk.blockchain.android.util.extensions.gone
import piuk.blockchain.android.util.extensions.inflate
import piuk.blockchain.android.util.extensions.visible

class AssetPriceCardDelegate<in T>(
        private val context: Context
) : AdapterDelegate<T> {

    private var viewHolder: AssetPriceCardViewHolder? = null

    override fun isForViewType(items: List<T>, position: Int): Boolean =
            items[position] is AssetPriceCardState

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
            AssetPriceCardViewHolder(parent.inflate(R.layout.item_asset_price_card))

    override fun onBindViewHolder(
            items: List<T>,
            position: Int,
            holder: RecyclerView.ViewHolder,
            payloads: List<*>
    ) {
        viewHolder = holder as AssetPriceCardViewHolder
        val item = items[position] as AssetPriceCardState

        viewHolder?.run {
            currency.text = context.getString(R.string.dashboard_price, item.currency.unit)
        }

        updateChartState(item)
    }

    private fun updateChartState(state: AssetPriceCardState) {
        when (state) {
            is AssetPriceCardState.Data -> renderData(state)
            is AssetPriceCardState.Loading -> renderLoading()
            is AssetPriceCardState.Error -> renderError()
        }
    }

    private fun renderData(data: AssetPriceCardState.Data) {
        viewHolder?.run {
            progressBar.gone()
            error.gone()
            price.text = data.priceString
        }
    }

    private fun renderLoading() {
        viewHolder?.run {
            progressBar.visible()
            error.gone()
        }
    }

    private fun renderError() {
        viewHolder?.run {
            progressBar.gone()
            error.visible()
        }
    }

    private class AssetPriceCardViewHolder internal constructor(
            itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        internal var price: TextView = itemView.textview_price
        internal var currency: TextView = itemView.textview_currency
        internal var progressBar: ProgressBar = itemView.progress_bar
        internal var error: TextView = itemView.textview_error
        internal var button: LinearLayout = itemView.button_see_charts

    }


}