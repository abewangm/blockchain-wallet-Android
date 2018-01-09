package piuk.blockchain.android.ui.dashboard.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.graphics.PorterDuff
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.android.synthetic.main.item_chart.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.charts.TimeSpan
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.dashboard.ChartDisplayable
import piuk.blockchain.android.ui.dashboard.ChartsState
import piuk.blockchain.android.util.extensions.*
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import uk.co.chrisjenx.calligraphy.CalligraphyUtils
import uk.co.chrisjenx.calligraphy.TypefaceUtils
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ChartDelegate<in T>(
        private val context: Context
) : AdapterDelegate<T> {

    private var viewHolder: ChartViewHolder? = null
    private var fiatSymbol: String? = null

    private val typefaceRegular by unsafeLazy {
        TypefaceUtils.load(context.assets, "fonts/Montserrat-Regular.ttf")
    }
    private val typefaceLight by unsafeLazy {
        TypefaceUtils.load(context.assets, "fonts/Montserrat-Light.ttf")
    }
    private val buttonsList by unsafeLazy {
        listOf(
                viewHolder!!.day,
                viewHolder!!.week,
                viewHolder!!.month,
                viewHolder!!.year,
                viewHolder!!.allTime
        )
    }

    override fun isForViewType(items: List<T>, position: Int): Boolean =
            items[position] is ChartDisplayable

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
            ChartViewHolder(parent.inflate(R.layout.item_chart))

    override fun onBindViewHolder(
            items: List<T>,
            position: Int,
            holder: RecyclerView.ViewHolder,
            payloads: List<*>
    ) {
        viewHolder = holder as ChartViewHolder
        showTimeSpanSelected(TimeSpan.MONTH)
    }

    internal fun updateChartState(chartsState: ChartsState) = when (chartsState) {
        is ChartsState.Data -> showData(chartsState)
        is ChartsState.Loading -> showLoading()
        is ChartsState.Error -> showError()
        is ChartsState.TimeSpanUpdated -> showTimeSpanSelected(chartsState.timeSpan)
    }

    internal fun updateSelectedCurrency(cryptoCurrency: CryptoCurrencies) {
        viewHolder?.currency?.setText(
                if (cryptoCurrency == CryptoCurrencies.BTC) {
                    R.string.dashboard_bitcoin_price
                } else {
                    R.string.dashboard_ether_price
                }
        )
    }

    internal fun updateCurrencyPrice(price: String) {
        viewHolder?.let { it.price.text = price }
    }

    private fun showData(data: ChartsState.Data) {
        fiatSymbol = data.fiatSymbol
        configureChart()
        updatePercentChange(data)

        viewHolder?.apply {
            day.setOnClickListener { data.getChartDay() }
            week.setOnClickListener { data.getChartWeek() }
            month.setOnClickListener { data.getChartMonth() }
            year.setOnClickListener { data.getChartYear() }
            allTime.setOnClickListener { data.getChartAllTime() }

            progressBar.gone()
            chart.apply {
                visible()

                val entries = data.data.map { Entry(it.timestamp.toFloat(), it.price.toFloat()) }
                this.data = LineData(LineDataSet(entries, null).apply {
                    color = ContextCompat.getColor(context, R.color.primary_navy_medium)
                    lineWidth = 3f
                    mode = LineDataSet.Mode.LINEAR
                    setDrawValues(false)
                    circleRadius = 1.5f
                    setDrawCircleHole(false)
                    setCircleColor(ContextCompat.getColor(context, R.color.primary_navy_medium))
                    setDrawFilled(false)
                    isHighlightEnabled = true
                    setDrawHighlightIndicators(false)
                    marker = ValueMarker(context, R.layout.item_chart_marker)
                })

                animateX(500)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updatePercentChange(data: ChartsState.Data) {
        val first = data.data.first()
        val last = data.data.last()
        val difference = last.price - first.price
        val percentChange = (difference / first.price) * 100

        viewHolder?.apply {
            percentage.text = "${String.format("%.1f", percentChange)}%"
            when {
                percentChange < 0 -> updateArrow(arrow, 0f, R.color.product_red_medium)
                percentChange == 0.0 -> arrow.invisible()
                else -> updateArrow(arrow, 180f, R.color.product_green_medium)
            }
        }
    }

    private fun updateArrow(arrow: ImageView, rotation: Float, @ColorRes color: Int) {
        arrow.visible()
        arrow.rotation = rotation
        arrow.setColorFilter(
                ContextCompat.getColor(arrow.context, color),
                PorterDuff.Mode.SRC_ATOP
        )
    }

    private fun showLoading() {
        viewHolder?.let {
            it.progressBar.visible()
            it.chart.invisible()
        }
    }

    private fun showError() {
        viewHolder?.let {
            it.progressBar.gone()
            it.chart.apply {
                visible()
                data = null
                invalidate()
            }
        }

        context.toast(R.string.dashboard_charts_error, ToastCustom.TYPE_ERROR)
    }

    private fun showTimeSpanSelected(timeSpan: TimeSpan) {
        selectButton(timeSpan, viewHolder)
        setDateFormatter(timeSpan)
    }

    private fun selectButton(timeSpan: TimeSpan, viewHolder: ChartViewHolder?) {
        viewHolder?.let {
            when (timeSpan) {
                TimeSpan.ALL_TIME -> setTextViewSelected(viewHolder.allTime)
                TimeSpan.YEAR -> setTextViewSelected(viewHolder.year)
                TimeSpan.MONTH -> setTextViewSelected(viewHolder.month)
                TimeSpan.WEEK -> setTextViewSelected(viewHolder.week)
                TimeSpan.DAY -> setTextViewSelected(viewHolder.day)
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun setDateFormatter(timeSpan: TimeSpan) {
        val dateFormat = when (timeSpan) {
            TimeSpan.ALL_TIME -> SimpleDateFormat("yyyy")
            TimeSpan.YEAR -> SimpleDateFormat("MMM ''yy")
            TimeSpan.MONTH, TimeSpan.WEEK -> SimpleDateFormat("dd. MMM")
            TimeSpan.DAY -> SimpleDateFormat("H:00")
        }

        viewHolder?.let {
            it.chart.xAxis.setValueFormatter { fl, _ ->
                dateFormat.format(Date(fl.toLong() * 1000))
            }
        }
    }

    private fun setTextViewSelected(selected: TextView) {
        with(selected) {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            CalligraphyUtils.applyFontToTextView(this, typefaceRegular)
        }
        buttonsList.filterNot { it === selected }
                .map {
                    with(it) {
                        paintFlags = paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
                        CalligraphyUtils.applyFontToTextView(this, typefaceLight)
                    }
                }
    }

    @SuppressLint("SimpleDateFormat")
    private fun configureChart() {
        viewHolder?.chart?.apply {
            setDrawGridBackground(false)
            setDrawBorders(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            axisLeft.setDrawGridLines(false)
            axisLeft.setValueFormatter { fl, _ ->
                "$fiatSymbol${NumberFormat.getNumberInstance(Locale.getDefault())
                        .apply {
                            maximumFractionDigits = 0
                            roundingMode = RoundingMode.HALF_UP
                        }.format(fl)}"
            }
            axisLeft.typeface = typefaceLight
            axisLeft.textColor = ContextCompat.getColor(context, R.color.primary_gray_medium)
            axisRight.isEnabled = false
            xAxis.setDrawGridLines(false)
            xAxis.typeface = typefaceLight
            xAxis.textColor = ContextCompat.getColor(context, R.color.primary_gray_medium)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.isGranularityEnabled = true
            setExtraOffsets(8f, 0f, 0f, 10f)
            setNoDataTextColor(ContextCompat.getColor(context, R.color.primary_gray_medium))
        }
    }

    inner class ValueMarker(
            context: Context,
            layoutResource: Int
    ) : MarkerView(context, layoutResource) {

        private val date = findViewById<TextView>(R.id.textview_marker_date)
        private val price = findViewById<TextView>(R.id.textview_marker_price)

        private var mpPointF: MPPointF? = null

        @SuppressLint("SimpleDateFormat", "SetTextI18n")
        override fun refreshContent(e: Entry, highlight: Highlight) {
            date.text = SimpleDateFormat("E, MMM dd, HH:mm").format(Date(e.x.toLong() * 1000))
            price.text = "$fiatSymbol${NumberFormat.getNumberInstance(Locale.getDefault())
                    .apply { maximumFractionDigits = 2 }
                    .format(e.y)}"

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

    private class ChartViewHolder internal constructor(
            itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        internal var chart: LineChart = itemView.chart
        internal var day: TextView = itemView.textview_day
        internal var week: TextView = itemView.textview_week
        internal var month: TextView = itemView.textview_month
        internal var year: TextView = itemView.textview_year
        internal var allTime: TextView = itemView.textview_all_time
        internal var price: TextView = itemView.textview_price
        internal var percentage: TextView = itemView.textview_percentage
        internal var currency: TextView = itemView.textview_currency
        internal var progressBar: ProgressBar = itemView.progress_bar
        internal var arrow: ImageView = itemView.imageview_arrow

    }

}