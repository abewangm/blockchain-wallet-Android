package piuk.blockchain.android.ui.pairing_code

import android.graphics.Bitmap
import android.support.annotation.StringRes

import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.customviews.ToastCustom

interface PairingCodeView : View {

    fun onQrLoaded(bitmap: Bitmap)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun showProgressSpinner()

    fun hideProgressSpinner()
}
