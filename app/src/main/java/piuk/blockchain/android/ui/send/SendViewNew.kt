package piuk.blockchain.android.ui.send

import android.support.annotation.ColorRes
import android.support.annotation.StringRes
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.customviews.ToastCustom

interface SendViewNew : View {

    fun setSendingAddress(label: String)

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

    fun getCustomFeeValue(): Long

    fun showMaxAvailable()

    fun hideMaxAvailable()

    fun setUnconfirmedFunds(text: String)

    fun updateFeeField(fee: String)

    fun setMaxAvailable(max: String)

    fun setMaxAvailableColor(@ColorRes color: Int)

    fun setSpendAllAmount(textFromSatoshis: String)

    fun showFeePriority()

    fun hideFeePriority()

    fun setReceivingAddress(address: String)

    fun selectTab(tabIndex: Int)

    fun onShowBIP38PassphrasePrompt(scanData: String)

    fun setCryptoMaxLength(length: Int)
}
