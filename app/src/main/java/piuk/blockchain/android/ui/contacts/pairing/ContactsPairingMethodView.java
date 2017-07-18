package piuk.blockchain.android.ui.contacts.pairing;

import android.support.annotation.StringRes;

import piuk.blockchain.android.ui.base.View;
import piuk.blockchain.android.ui.customviews.ToastCustom;

interface ContactsPairingMethodView extends View {

    void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    void finishActivityWithResult(int resultCode);

}
