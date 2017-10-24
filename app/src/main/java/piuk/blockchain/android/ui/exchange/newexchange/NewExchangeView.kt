package piuk.blockchain.android.ui.exchange.newexchange

import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.ui.base.View

interface NewExchangeView : View {

    fun showFrom(cryptoCurrency: CryptoCurrencies)

}