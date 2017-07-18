package piuk.blockchain.android.ui.contacts.pairing;

import android.content.Intent;
import android.support.annotation.StringRes;

import piuk.blockchain.android.ui.base.View;
import piuk.blockchain.android.ui.customviews.ToastCustom;

interface ContactsInvitationBuilderView extends View {

    void showProgressDialog();

    void dismissProgressDialog();

    void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    void onLinkGenerated(Intent intent);

    void onUriGenerated(String uri, String recipientName);

    void finishPage();
}
