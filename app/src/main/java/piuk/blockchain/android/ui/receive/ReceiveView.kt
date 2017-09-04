package piuk.blockchain.android.ui.receive

import android.graphics.Bitmap
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.customviews.ToastCustom

interface ReceiveView : View {

    fun getQrBitmap(): Bitmap

    fun getContactName(): String

    fun getBtcAmount(): String

    fun showQrLoading()

    fun showQrCode(bitmap: Bitmap?)

    fun showToast(message: String, @ToastCustom.ToastType toastType: String)

    fun updateFiatTextField(text: String)

    fun updateBtcTextField(text: String)

    fun startContactSelectionActivity()

    fun updateReceiveAddress(address: String)

    fun hideContactsIntroduction()

    fun showContactsIntroduction()

    fun showWatchOnlyWarning()

    fun displayBitcoinLayout()

    fun hideBitcoinLayout()

    fun updateToAddress(label: String)

    fun showBottomSheet(uri: String)

}
