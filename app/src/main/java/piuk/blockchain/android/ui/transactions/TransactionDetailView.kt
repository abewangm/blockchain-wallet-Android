package piuk.blockchain.android.ui.transactions

import android.content.Intent
import android.support.annotation.ColorRes
import android.support.annotation.StringRes
import info.blockchain.wallet.multiaddress.TransactionSummary
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.customviews.ToastCustom

interface TransactionDetailView : View {

    fun getPageIntent(): Intent?

    fun pageFinish()

    fun setTransactionType(type: TransactionSummary.Direction)

    fun setTransactionValueBtc(value: String?)

    fun setTransactionValueFiat(fiat: String?)

    fun setToAddresses(addresses: List<RecipientModel>)

    fun setFromAddress(address: String?)

    fun setStatus(status: String?, hash: String?)

    fun setFee(fee: String?)

    fun setDate(date: String?)

    fun setDescription(description: String?)

    fun setIsDoubleSpend(isDoubleSpend: Boolean)

    fun setTransactionNote(note: String?)

    fun setTransactionColour(@ColorRes colour: Int)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun onDataLoaded()

    fun showTransactionAsPaid()
}