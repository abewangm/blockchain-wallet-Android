package piuk.blockchain.android.ui.login;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import org.json.JSONObject;

import piuk.blockchain.android.ui.base.View;
import piuk.blockchain.android.ui.customviews.ToastCustom;

interface ManualPairingView extends View {

    String getGuid();

    String getPassword();

    void goToPinPage();

    void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    void updateWaitingForAuthDialog(int secondsRemaining);

    void showProgressDialog(@StringRes int messageId, @Nullable String suffix, boolean cancellable);

    void dismissProgressDialog();

    void resetPasswordField();

    void showTwoFactorCodeNeededDialog(JSONObject responseObject, String sessionId, int authType, String guid, String password);

}
