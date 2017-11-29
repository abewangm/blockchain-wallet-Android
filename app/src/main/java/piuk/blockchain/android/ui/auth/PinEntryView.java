package piuk.blockchain.android.ui.auth;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatDialogFragment;
import android.widget.ImageView;

import piuk.blockchain.android.ui.base.View;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.DialogButtonCallback;
import piuk.blockchain.android.util.ViewUtils;

interface PinEntryView extends View {

    Intent getPageIntent();

    ImageView[] getPinBoxArray();

    void showProgressDialog(@StringRes int messageId, @Nullable String suffix);

    void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    void dismissProgressDialog();

    void showMaxAttemptsDialog();

    void showValidationDialog();

    void showCommonPinWarning(DialogButtonCallback callback);

    void showWalletVersionNotSupportedDialog(String walletVersion);

    void goToUpgradeWalletActivity();

    void restartPageAndClearTop();

    void setTitleString(@StringRes int title);

    void setTitleVisibility(@ViewUtils.Visibility int visibility);

    void clearPinBoxes();

    void goToPasswordRequiredActivity();

    void finishWithResultOk(String pin);

    void showFingerprintDialog(String pincode);

    void showKeyboard();

    void showAccountLockedDialog();

    void showCustomPrompt(AppCompatDialogFragment alertFragment);

    void forceUpgrade();
}
