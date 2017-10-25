package piuk.blockchain.android.ui.shapeshift.newexchange

import android.support.annotation.StringRes
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.customviews.ToastCustom

interface NewExchangeView : View {

    fun updateUi(
            fromCurrency: CryptoCurrencies,
            displayDropDown: Boolean,
            fromLabel: String,
            toLabel: String
    )

    fun launchAccountChooserActivityTo()

    fun launchAccountChooserActivityFrom()

    fun showProgressDialog(@StringRes message: Int)

    fun dismissProgressDialog()

    fun finishPage()

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun updateFromCryptoText(text: String)

    fun updateToCryptoText(text: String)

    fun updateFromFiatText(text: String)

    fun updateToFiatText(text: String)

}