package piuk.blockchain.android.ui.shapeshift.newexchange

import android.support.annotation.StringRes

import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.shapeshift.models.ShapeShiftData
import java.util.*

interface NewExchangeView : View {

    val locale: Locale

    val shapeShiftApiKey: String

    fun updateUi(
            fromCurrency: CryptoCurrencies,
            fromLabel: String,
            toLabel: String,
            fiatHint: String
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

    fun clearEditTexts()

    fun showAmountError(errorMessage: String)

    fun clearError()

    fun setButtonEnabled(enabled: Boolean)

    fun showQuoteInProgress(inProgress: Boolean)

    fun launchConfirmationPage(shapeShiftData: ShapeShiftData)

}