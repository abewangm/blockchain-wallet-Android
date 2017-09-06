package piuk.blockchain.android.ui.dashboard

import piuk.blockchain.android.ui.base.View

interface DashboardView : View {

    fun updateChartState(chartsState: ChartsState)

    fun updateEthBalance(balance: String)

    fun updateBtcBalance(balance: String)

    fun updateTotalBalance(balance: String)

    fun updateCryptoCurrencyPrice(price: String)

}