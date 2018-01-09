package piuk.blockchain.android.ui.dashboard.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.android.synthetic.main.item_pie_chart.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.dashboard.PieChartsState
import piuk.blockchain.android.util.extensions.gone
import piuk.blockchain.android.util.extensions.inflate
import piuk.blockchain.android.util.extensions.invisible
import piuk.blockchain.android.util.extensions.visible
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import uk.co.chrisjenx.calligraphy.TypefaceUtils
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


class PieChartDelegate<in T>(private val context: Context) : AdapterDelegate<T> {

    private var viewHolder: PieChartViewHolder? = null
    private var fiatSymbol: String? = null

    private val typefaceRegular by unsafeLazy {
        TypefaceUtils.load(context.assets, "fonts/Montserrat-Regular.ttf")
    }

    override fun isForViewType(items: List<T>, position: Int): Boolean
            = items[position] is PieChartsState

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder
            = PieChartViewHolder(parent.inflate(R.layout.item_pie_chart))

    override fun onBindViewHolder(
            items: List<T>,
            position: Int,
            holder: RecyclerView.ViewHolder,
            payloads: List<*>
    ) {

        fiatSymbol = "$" // TODO: Remove this
        viewHolder = holder as PieChartViewHolder

        viewHolder?.chart?.apply {
            setDrawCenterText(true)
            setCenterTextTypeface(typefaceRegular)
            setCenterTextColor(ContextCompat.getColor(context, R.color.primary_gray_dark))
            setCenterTextSize(16f)
            centerText = "$22,866.48"

            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 70f

            animateY(1000, Easing.EasingOption.EaseInOutQuad)
            isRotationEnabled = false
            legend.isEnabled = false
            description.isEnabled = false

            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            marker = ValueMarker(context, R.layout.item_pie_chart_marker)
        }

    }

    internal fun updateChartState(pieChartsState: PieChartsState) = when (pieChartsState) {
        is PieChartsState.Data -> renderData(pieChartsState)
        is PieChartsState.Error -> renderError()
        is PieChartsState.Loading -> renderLoading()
    }

    private fun renderLoading() {
        viewHolder?.apply {
            progressBar.visible()
            chart.invisible()
        }
    }

    private fun renderError() {
        viewHolder?.apply {
            progressBar.gone()
            chart.apply {
                visible()
                data = null
                invalidate()
            }
        }

        ToastCustom.makeText(
                context,
                context.getText(R.string.dashboard_charts_balance_error),
                ToastCustom.LENGTH_SHORT,
                ToastCustom.TYPE_ERROR
        )
    }

    private fun renderData(data: PieChartsState.Data) {
        val entries = ArrayList<PieEntry>()

        (0 until 5).forEach { entries.add(PieEntry((Math.random() * 400 + 400 / 5).toFloat())) }

        val dataSet = PieDataSet(entries, context.getString(R.string.dashboard_balances))

        dataSet.setDrawIcons(false)

        dataSet.sliceSpace = 0f
        dataSet.selectionShift = 5f

        val colors = listOf(
                ContextCompat.getColor(context, R.color.color_bitcoin),
                ContextCompat.getColor(context, R.color.color_ether),
                ContextCompat.getColor(context, R.color.color_bitcoin_cash)
        )

        dataSet.colors = colors

        val chartData = PieData(dataSet)
        chartData.setDrawValues(false)
        viewHolder?.chart?.apply {
            this.data = chartData
            highlightValues(null)
            invalidate()

            textview_value_bitcoin.text = getFormattedFiatAmount(data.fiatSymbol, data.bitcoinAmount * data.bitcoinExchangeRate)
        }
    }

    private fun getFormattedFiatAmount(fiatSymbol: String, value: BigDecimal): String =
            "$fiatSymbol${NumberFormat.getNumberInstance(Locale.getDefault())
                    .apply { maximumFractionDigits = 2 }
                    .format(value)}"

    private inner class ValueMarker(
            context: Context,
            layoutResource: Int
    ) : MarkerView(context, layoutResource) {

        private val coin = findViewById<TextView>(R.id.textview_marker_coin)
        private val price = findViewById<TextView>(R.id.textview_marker_price)

        private var mpPointF: MPPointF? = null

        @SuppressLint("SimpleDateFormat", "SetTextI18n")
        override fun refreshContent(e: Entry, highlight: Highlight) {
            // TODO: Change this
            coin.text = SimpleDateFormat("E, MMM dd, HH:mm").format(Date(e.x.toLong() * 1000))
            price.text = getFormattedFiatAmount(fiatSymbol!!, e.y.toBigDecimal())

            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF {
            if (mpPointF == null) {
                // Center the marker horizontally and vertically
                mpPointF = MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
            }

            return mpPointF!!
        }
    }

    private class PieChartViewHolder internal constructor(
            itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        internal var chart: PieChart = itemView.pie_chart
        internal val progressBar: ProgressBar = itemView.progress_bar
        // Bitcoin
        internal var bitcoinValue: TextView = itemView.textview_value_bitcoin
        internal var bitcoinAmount: TextView = itemView.textview_amount_bitcoin
        // Ether
        internal var etherValue: TextView = itemView.textview_value_ether
        internal var etherAmount: TextView = itemView.textview_amount_ether
        // Bitcoin Cash
        internal var bitcoinCashValue: TextView = itemView.textview_value_bitcoin_cash
        internal var bitcoinCashAmount: TextView = itemView.textview_amount_bitcoin_cash

    }

}