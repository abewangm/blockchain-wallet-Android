package piuk.blockchain.android.ui.send

import android.support.annotation.StringRes
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.customviews.ToastCustom

interface SendViewNew : View {

    fun setSendingAddress(get: ItemAccount)

    fun finishPage(paymentMade: Boolean)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun setReceivingHint(hint: Int)

    fun hideReceivingDropdown()

    fun showReceivingDropdown()

    fun hideSendingFieldDropdown()

    fun showSendingFieldDropdown()

    fun disableCryptoTextChangeListener()

    fun enableCryptoTextChangeListener()

    fun updateCryptoTextField(amountString: String?)

    fun disableFiatTextChangeListener()

    fun enableFiatTextChangeListener()

    fun updateFiatTextField(amountString: String?)

    fun setCryptoCurrency(currency: String)

    fun resetAmounts()
}
