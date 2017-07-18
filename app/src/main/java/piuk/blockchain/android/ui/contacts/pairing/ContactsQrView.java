package piuk.blockchain.android.ui.contacts.pairing;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.StringRes;

import piuk.blockchain.android.ui.base.View;
import piuk.blockchain.android.ui.customviews.ToastCustom;

interface ContactsQrView extends View {

    Bundle getFragmentBundle();

    void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    void onQrLoaded(Bitmap bitmap);

    void updateDisplayMessage(String name);

    void finishPage();

}
