package piuk.blockchain.android.ui.dashboard.adapter

import android.content.Context
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.dashboard.ChartsState
import piuk.blockchain.android.ui.dashboard.PieChartsState

class DashboardDelegateAdapter(
        context: Context
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    private val chartDelegate = ChartDelegate<Any>(context)
    private val onboardingDelegate = OnboardingDelegate<Any>(context)
    private val pieChartDelegate = PieChartDelegate<Any>(context)

    init {
        // Add all necessary AdapterDelegate objects here
        delegatesManager.addAdapterDelegate(AnnouncementDelegate())
        delegatesManager.addAdapterDelegate(HeaderDelegate())
        delegatesManager.addAdapterDelegate(onboardingDelegate)
        delegatesManager.addAdapterDelegate(pieChartDelegate)
    }

    /**
     * Updates the state of the Charts card without causing a refresh of the entire View.
     */
    fun updateChartState(chartsState: ChartsState) {
        chartDelegate.updateChartState(chartsState)
    }

    /**
     * Updates the state of the Balance card without causing a refresh of the entire View.
     */
    fun updatePieChartState(chartsState: PieChartsState) {
        pieChartDelegate.updateChartState(chartsState)
    }

    /**
     * Updates the selected currency without causing a refresh of the entire View.
     */
    fun updateSelectedCurrency(cryptoCurrency: CryptoCurrencies) {
        chartDelegate.updateSelectedCurrency(cryptoCurrency)
    }

    /**
     * Updates the price of the selected currency without causing a refresh of the entire View.
     */
    fun updateCurrencyPrice(price: String) {
        chartDelegate.updateCurrencyPrice(price)
    }

}