package piuk.blockchain.android.ui.dashboard

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_dashboard.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.dashboard.adapter.DashboardDelegateAdapter
import piuk.blockchain.android.util.extensions.inflate
import piuk.blockchain.android.util.extensions.toast
import piuk.blockchain.android.util.helperfunctions.setOnTabSelectedListener
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import javax.inject.Inject


class DashboardFragment : BaseFragment<DashboardView, DashboardPresenter>(), DashboardView {

    @Inject lateinit var dashboardPresenter: DashboardPresenter
    private val dashboardAdapter by unsafeLazy { DashboardDelegateAdapter(activity) }

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
                    presenter.updateSelectedCurrency(CryptoCurrencies.BTC)
                    dashboardAdapter.updateSelectedCurrency(CryptoCurrencies.BTC)
                } else {
                    presenter.updateSelectedCurrency(CryptoCurrencies.ETHER)
                    dashboardAdapter.updateSelectedCurrency(CryptoCurrencies.ETHER)
                }
            }
        }

        recycler_view?.apply {
            layoutManager = LinearLayoutManager(activity)
            this.adapter = dashboardAdapter
        }

        onViewReady()
    }

    override fun onResume() {
        super.onResume()
        presenter.updatePrices()
    }

    override fun updateAdapterItems(displayItems: MutableList<Any>) {
        dashboardAdapter.items = displayItems
    }

    override fun updateChartState(chartsState: ChartsState) {
        dashboardAdapter.updateChartState(chartsState)
    }

    override fun updateEthBalance(balance: String) {
        textview_eth.text = balance
    }

    override fun updateBtcBalance(balance: String) {
        textview_btc.text = balance
    }

    override fun updateTotalBalance(balance: String) {
        textview_total.text = balance
    }

    override fun updateCryptoCurrencyPrice(price: String) {
        dashboardAdapter.updateCurrencyPrice(price)
    }

    override fun showToast(message: Int, toastType: String) {
        toast(message, toastType)
    }

    override fun createPresenter() = dashboardPresenter

    override fun getMvpView() = this

    companion object {

        @JvmStatic
        fun newInstance(): DashboardFragment {
            return DashboardFragment()
        }

    }

}