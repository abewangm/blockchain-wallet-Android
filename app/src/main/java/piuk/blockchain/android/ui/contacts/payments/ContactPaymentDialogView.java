package piuk.blockchain.android.ui.contacts.payments;

import android.os.Bundle;
import android.support.annotation.StringRes;

import piuk.blockchain.android.ui.base.View;
import piuk.blockchain.android.ui.customviews.ToastCustom;

interface ContactPaymentDialogView extends View {

    Bundle getFragmentBundle();

    void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    void showProgressDialog();

    void hideProgressDialog();

    void setContactName(String name);

    void finishPage(boolean paymentSent);

    void updatePaymentAmountBtc(String amount);

    void updateFeeAmountFiat(String amount);

    void updatePaymentAmountFiat(String amount);

    void updateFeeAmountBtc(String amount);

    void setPaymentButtonEnabled(boolean enabled);

    void onUiUpdated();

    void onShowTransactionSuccess(String contactMdid, String hash, String fctxId, long amount);
}
