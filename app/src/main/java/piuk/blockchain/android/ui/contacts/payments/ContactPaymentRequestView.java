package piuk.blockchain.android.ui.contacts.payments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import piuk.blockchain.android.data.contacts.PaymentRequestType;
import piuk.blockchain.android.ui.base.View;
import piuk.blockchain.android.ui.customviews.ToastCustom;

interface ContactPaymentRequestView extends View {

    Bundle getFragmentBundle();

    @Nullable
    String getNote();

    void finishPage();

    void contactLoaded(String name, PaymentRequestType paymentRequestType);

    void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    void showProgressDialog();

    void dismissProgressDialog();

    void showSendSuccessfulDialog(String name);

    void showRequestSuccessfulDialog();
}
