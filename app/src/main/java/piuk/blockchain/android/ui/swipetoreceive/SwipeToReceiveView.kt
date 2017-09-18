package piuk.blockchain.android.ui.swipetoreceive

import android.graphics.Bitmap
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.ui.base.UiState
import piuk.blockchain.android.ui.base.View

interface SwipeToReceiveView : View {

    val cryptoCurrency: CryptoCurrencies

    fun displayQrCode(bitmap: Bitmap)

    fun displayReceiveAddress(address: String)

    fun displayReceiveAccount(accountName: String)

    fun setUiState(@UiState.UiStateDef uiState: Int)

}