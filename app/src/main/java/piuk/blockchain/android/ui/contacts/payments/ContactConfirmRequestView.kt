package piuk.blockchain.android.ui.contacts.payments

import android.os.Bundle
import android.support.annotation.StringRes
import piuk.blockchain.android.data.contacts.models.PaymentRequestType

import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.customviews.ToastCustom

interface ContactConfirmRequestView : View {

    val fragmentBundle: Bundle?

    val note: String

    fun finishPage()

    fun contactLoaded(name: String)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun showProgressDialog()

    fun dismissProgressDialog()

    fun onRequestSuccessful(
            paymentRequestType: PaymentRequestType,
            contactName: String,
            btcAmount: String
    )

    fun updateTotalBtc(total: String)

    fun updateTotalFiat(total: String)

    fun updateAccountName(name: String)

    fun updatePaymentType(paymentRequestType: PaymentRequestType)

}
