package piuk.blockchain.android.ui.auth;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import org.json.JSONObject;

import piuk.blockchain.android.ui.base.View;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.DialogButtonCallback;

interface PasswordRequiredView extends View {

    String getPassword();

    void resetPasswordField();

    void goToPinPage();

    void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    void restartPage();

    void updateWaitingForAuthDialog(int secondsRemaining);

    void showProgressDialog(@StringRes int messageId, @Nullable String suffix, boolean cancellable);

    void dismissProgressDialog();

    void showForgetWalletWarning(DialogButtonCallback callback);

    void showTwoFactorCodeNeededDialog(JSONObject responseObject, String sessionId, int authType, String password);

}
