package piuk.blockchain.android.ui.receive;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.StringRes;

import piuk.blockchain.android.ui.base.View;
import piuk.blockchain.android.ui.customviews.ToastCustom;

interface ReceiveQrView extends View {

    Intent getPageIntent();

    void finishActivity();

    void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    void setAddressLabel(String label);

    void setAddressInfo(String addressInfo);

    void setImageBitmap(Bitmap bitmap);

    void showClipboardWarning(String receiveAddressString);

}
