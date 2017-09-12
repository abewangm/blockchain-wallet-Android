package piuk.blockchain.android.ui.send

import android.support.annotation.ColorRes
import android.support.annotation.StringRes
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.customviews.ToastCustom

interface SendViewNew : View {

    //Update field
    fun updateSendingAddress(label: String)

    fun updateReceivingHint(hint: Int)

    fun updateCryptoCurrency(currency: String)

    fun updateCryptoAmount(amountString: String?)

    fun updateFiatAmount(amountString: String?)

    fun updateWarning(message: String)

    fun updateMaxAvailable(max: String)

    fun updateMaxAvailableColor(@ColorRes color: Int)

    fun updateReceivingAddress(address: String)

    fun updateFeeAmount(fee: String)

    //Set property
    fun setCryptoMaxLength(length: Int)

    fun setTabSelection(tabIndex: Int)

    fun clearWarning()

    //Hide / Show
    fun hideReceivingDropdown()

    fun showReceivingDropdown()

    fun hideSendingFieldDropdown()

    fun showSendingFieldDropdown()

    fun showMaxAvailable()

    fun hideMaxAvailable()

    fun showFeePriority()

    fun hideFeePriority()

    //Enable / Disable
    fun disableCryptoTextChangeListener()

    fun enableCryptoTextChangeListener()

    fun disableFiatTextChangeListener()

    fun enableFiatTextChangeListener()

    fun enableFeeDropdown()

    fun disableFeeDropdown()

    // Fetch value
    fun getCustomFeeValue(): Long

    fun getClipboardContents(): String?

    fun getReceivingAddress(): String?

    // Prompts
    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun showBIP38PassphrasePrompt(scanData: String)

    fun showWatchOnlyWarning(address: String)

    fun showProgressDialog(@StringRes title: Int)

    fun showSpendFromWatchOnlyWarning(address: String)

    fun showSecondPasswordDialog()

    fun showPaymentDetails(confirmationDetails: PaymentConfirmationDetails, note: String?)

    fun showLargeTransactionWarning()

    fun dismissProgressDialog()

    fun finishPage(paymentMade: Boolean)
}
