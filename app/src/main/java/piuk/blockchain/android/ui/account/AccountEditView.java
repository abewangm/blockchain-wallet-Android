package piuk.blockchain.android.ui.account;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import piuk.blockchain.android.ui.base.View;
import piuk.blockchain.android.ui.customviews.ToastCustom;

interface AccountEditView extends View {

    Intent getIntent();

    void promptAccountLabel(@Nullable String currentLabel);

    void showToast(@StringRes int message, @ToastCustom.ToastType String type);

    void setActivityResult(int resultCode);

    void startScanActivity();

    void promptPrivateKey(String message);

    void promptArchive(String title, String message);

    void promptBIP38Password(String data);

    void privateKeyImportMismatch();

    void privateKeyImportSuccess();

    void showXpubSharingWarning();

    void showAddressDetails(String heading, String note, String copy, Bitmap bitmap, String qrString);

    void showPaymentDetails(PaymentConfirmationDetails details);

    void showTransactionSuccess();

    void showProgressDialog(@StringRes int message);

    void dismissProgressDialog();

    void sendBroadcast(String key, String data);

    void updateAppShortcuts();

}
