package piuk.blockchain.android.ui.dashboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_dashboard.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.balance.BalanceFragment
import piuk.blockchain.android.ui.base.BaseAuthActivity
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.charts.ChartsActivity
import piuk.blockchain.android.ui.dashboard.adapter.DashboardDelegateAdapter
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.util.AndroidUtils
import piuk.blockchain.android.util.ViewUtils
import piuk.blockchain.android.util.extensions.inflate
import piuk.blockchain.android.util.extensions.toast
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import java.util.*
import javax.inject.Inject

@Suppress("MemberVisibilityCanPrivate")
class DashboardFragment : BaseFragment<DashboardView, DashboardPresenter>(), DashboardView {

    override val shouldShowBuy: Boolean = AndroidUtils.is19orHigher()
    override val locale: Locale = Locale.getDefault()

    @Inject lateinit var dashboardPresenter: DashboardPresenter
    private val dashboardAdapter by unsafeLazy {
        DashboardDelegateAdapter(context!!) { ChartsActivity.start(context!!, it) }
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BalanceFragment.ACTION_INTENT && activity != null) {
                // Update balances
                presenter?.onResume()
            }
        }
    }

    init {
        Injector.INSTANCE.presenterComponent.inject(this)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_dashboard)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler_view?.apply {
            layoutManager = LinearLayoutManager(activity)
            this.adapter = dashboardAdapter
        }

        onViewReady()
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()
        presenter.onResume()
        if (activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView.restoreBottomNavigation()
            ViewUtils.setElevation((activity as MainActivity).toolbar, 5f)
        }
        LocalBroadcastManager.getInstance(context!!)
                .registerReceiver(receiver, IntentFilter(BalanceFragment.ACTION_INTENT))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(context!!).unregisterReceiver(receiver)
    }

    override fun notifyItemAdded(displayItems: MutableList<Any>, position: Int) {
        dashboardAdapter.items = displayItems
        dashboardAdapter.notifyItemInserted(position)
        recycler_view.scrollToPosition(0)
    }

    override fun notifyItemRemoved(displayItems: MutableList<Any>, position: Int) {
        dashboardAdapter.items = displayItems
        dashboardAdapter.notifyItemRemoved(position)
        recycler_view.smoothScrollToPosition(0)
    }

    override fun notifyItemUpdated(displayItems: MutableList<Any>, position: Int) {
        dashboardAdapter.items = displayItems
        dashboardAdapter.notifyItemChanged(position)
    }

    override fun updatePieChartState(chartsState: PieChartsState) {
        dashboardAdapter.updatePieChartState(chartsState)
    }

    override fun showToast(message: Int, toastType: String) {
        toast(message, toastType)
    }

    override fun startBuyActivity() {
        activity?.run {
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(Intent(MainActivity.ACTION_BUY))
        }
    }

    override fun startShapeShiftActivity() {
        activity?.run {
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(Intent(MainActivity.ACTION_SHAPESHIFT))
        }
    }

    override fun createPresenter() = dashboardPresenter

    override fun getMvpView() = this

    private fun setupToolbar() {
        if ((activity as AppCompatActivity).supportActionBar != null) {
            (activity as BaseAuthActivity).setupToolbar(
                    (activity as MainActivity).supportActionBar, R.string.dashboard_title)
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(): DashboardFragment {
            return DashboardFragment()
        }

    }

}