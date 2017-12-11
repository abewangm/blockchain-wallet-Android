package piuk.blockchain.android.ui.dashboard.adapter

import android.content.Context
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.dashboard.ChartsState

class DashboardDelegateAdapter(
        context: Context
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    private val chartDelegate = ChartDelegate<Any>(context)
    private val onboardingDelegate = OnboardingDelegate<Any>(context)

    init {
        // Add all necessary AdapterDelegate objects here
        delegatesManager.addAdapterDelegate(AnnouncementDelegate())
        delegatesManager.addAdapterDelegate(onboardingDelegate)
        delegatesManager.addAdapterDelegate(chartDelegate)
    }

    /**
     * Updates the state of the Charts card without causing a refresh of the entire View.
     */
    fun updateChartState(chartsState: ChartsState) {
        chartDelegate.updateChartState(chartsState)
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