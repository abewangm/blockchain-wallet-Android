package piuk.blockchain.android.ui.receive;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import piuk.blockchain.android.ui.base.View;
import piuk.blockchain.android.ui.customviews.ToastCustom;

interface ReceiveView extends View {

    Bitmap getQrBitmap();

    String getContactName();

    String getBtcAmount();

    int getSelectedAccountPosition();

    void onAccountDataChanged();

    void showQrLoading();

    void showQrCode(@Nullable Bitmap bitmap);

    void showToast(String message, @ToastCustom.ToastType String toastType);

    void updateFiatTextField(String text);

    void updateBtcTextField(String text);

    void startContactSelectionActivity();

    void updateReceiveAddress(String address);

    void hideContactsIntroduction();

    void showContactsIntroduction();
}
