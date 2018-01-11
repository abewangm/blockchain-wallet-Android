package piuk.blockchain.android.ui.charts

import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.ui.base.View
import java.util.*

interface ChartsView : View {

    val cryptoCurrency: CryptoCurrencies

    val locale: Locale

    fun updateChartState(state: ChartsState)

    fun updateSelectedCurrency(cryptoCurrency: CryptoCurrencies)

    fun updateCurrentPrice(fiatSymbol: String, price: Double)

}