package piuk.blockchain.android.ui.dashboard

import android.annotation.SuppressLint
import android.graphics.Paint
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import info.blockchain.api.data.Point
import kotlinx.android.synthetic.main.fragment_dashboard.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.charts.TimeSpan
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.extensions.*
import piuk.blockchain.android.util.helperfunctions.setOnTabSelectedListener
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import uk.co.chrisjenx.calligraphy.CalligraphyUtils
import uk.co.chrisjenx.calligraphy.TypefaceUtils
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


class DashboardFragment : BaseFragment<DashboardView, DashboardPresenter>(), DashboardView {

    @Inject lateinit var dashboardPresenter: DashboardPresenter
    private val typefaceRegular by unsafeLazy { TypefaceUtils.load(context.assets, "fonts/Montserrat-Regular.ttf") }
    private val typefaceLight by unsafeLazy { TypefaceUtils.load(context.assets, "fonts/Montserrat-Light.ttf") }

    init {
        Injector.INSTANCE.presenterComponent.inject(this)
    }

    override fun onCreateView(
            inflater: LayoutInflater?,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ) = container!!.inflate(R.layout.fragment_dashboard)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabs_dashboard.apply {
            addTab(tabs_dashboard.newTab().setText(R.string.bitcoin))
            addTab(tabs_dashboard.newTab().setText(R.string.ether))
            setOnTabSelectedListener {
                if (it == 0) {
                    textview_currency.setText(R.string.dashboard_bitcoin_price)
                    presenter.updateSelectedCurrency(CryptoCurrencies.BTC)
                } else {
                    textview_currency.setText(R.string.dashboard_ether_price)
                    presenter.updateSelectedCurrency(CryptoCurrencies.ETHER)
                }
            }
        }

        configureChart()

        textview_week.setOnClickListener { presenter.updateChartsData(TimeSpan.WEEK) }
        textview_month.setOnClickListener { presenter.updateChartsData(TimeSpan.MONTH) }
        textview_year.setOnClickListener { presenter.updateChartsData(TimeSpan.YEAR) }

        setTextViewSelected(textview_year, listOf(textview_week, textview_month))

        onViewReady()
    }

    override fun onResume() {
        super.onResume()
        presenter.updatePrices()
    }

    override fun updateChartState(chartsState: ChartsState) {
        return when (chartsState) {
            is ChartsState.Data -> showData(chartsState.data)
            is ChartsState.Loading -> showLoading()
            is ChartsState.Error -> showError()
            is ChartsState.SelectedTime -> selectButton(chartsState.timeSpan)
        }
    }

    override fun updateEthBalance(balance: String) {
        TODO("not implemented")
    }

    override fun updateBtcBalance(balance: String) {
        TODO("not implemented")
    }

    override fun updateTotalBalance(balance: String) {
        TODO("not implemented")
    }

    override fun updateCryptoCurrencyPrice(price: String) {
        textview_price.text = price
    }

    override fun createPresenter() = dashboardPresenter

    override fun getMvpView() = this

    private fun showLoading() {
        progress_bar.visible()
        chart.invisible()
    }

    private fun showError() {
        progress_bar.gone()
        chart.visible()
        chart.data = null
        chart.invalidate()

        activity.toast(R.string.dashboard_charts_error, ToastCustom.TYPE_ERROR)
    }

    private fun showData(data: List<Point>) {
        progress_bar.gone()
        chart.visible()

        val entries = data.map { Entry(it.x, it.y) }
        chart.data = LineData(LineDataSet(entries, null).apply {
            color = ContextCompat.getColor(context, R.color.primary_navy_medium)
            lineWidth = 3f
            mode = LineDataSet.Mode.LINEAR
            setDrawValues(false)
            circleRadius = 1.5f
            setDrawCircleHole(false)
            setCircleColor(ContextCompat.getColor(context, R.color.primary_navy_medium))
            setDrawFilled(false)
        })

        chart.animateX(500)
    }

    private fun selectButton(timeSpan: TimeSpan) {
        when (timeSpan) {
            TimeSpan.WEEK -> setTextViewSelected(textview_week, listOf(textview_month, textview_year))
            TimeSpan.MONTH -> setTextViewSelected(textview_month, listOf(textview_week, textview_year))
            TimeSpan.YEAR -> setTextViewSelected(textview_year, listOf(textview_week, textview_month))
            else -> throw IllegalArgumentException("Day isn't currently supported")
        }
    }

    private fun setTextViewSelected(selected: TextView, unselected: List<TextView>) {
        with(selected) {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            CalligraphyUtils.applyFontToTextView(this, typefaceRegular)
        }
        unselected.map {
            with(it) {
                paintFlags = paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
                CalligraphyUtils.applyFontToTextView(this, typefaceLight)
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun configureChart() {
        chart.apply {
            setDrawGridBackground(false)
            setDrawBorders(false)
            setTouchEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            isHighlightPerTapEnabled = false
            isHighlightPerDragEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            axisLeft.setDrawGridLines(false)
            axisLeft.setValueFormatter { fl, _ -> "${presenter.getCurrencySymbol()}${fl.toInt()}" }
            axisLeft.typeface = typefaceLight
            axisLeft.textColor = ContextCompat.getColor(context, R.color.primary_gray_medium)
            axisRight.isEnabled = false
            xAxis.setDrawGridLines(false)
            xAxis.typeface = typefaceLight
            xAxis.textColor = ContextCompat.getColor(context, R.color.primary_gray_medium)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setValueFormatter { fl, _ ->
                SimpleDateFormat("MMM dd").format(Date(fl.toLong() * 1000))
            }
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(): DashboardFragment {
            return DashboardFragment()
        }

    }

}