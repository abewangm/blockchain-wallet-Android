package piuk.blockchain.android.ui.settings;

import android.support.annotation.StringRes;

import piuk.blockchain.android.ui.base.View;
import piuk.blockchain.android.ui.customviews.ToastCustom;

interface SettingsView extends View {

    void setUpUi();

    void showFingerprintDialog(String pincode);

    void showDisableFingerprintDialog();

    void updateFingerprintPreferenceStatus();

    void showNoFingerprintsAddedDialog();

    void showProgressDialog(@StringRes int message);

    void hideProgressDialog();

    void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    void setGuidSummary(String summary);

    void setEmailSummary(String summary);

    void setSmsSummary(String summary);

    void setUnitsSummary(String summary);

    void setFiatSummary(String summary);

    void setEmailNotificationsVisibility(boolean visible);

    void setEmailNotificationPref(boolean enabled);

    void setPushNotificationPref(boolean enabled, String token);

    void setFingerprintVisibility(boolean visible);

    void setTwoFaPreference(boolean enabled);

    void setTorBlocked(boolean blocked);

    void setScreenshotsEnabled(boolean enabled);

    void showDialogEmailVerification();

    void showDialogVerifySms();

    void showDialogSmsVerified();

    void goToPinEntryPage();

    void setLauncherShortcutVisibility(boolean visible);

    void showWarningDialog(@StringRes int message);

    void showFirebaseToken(String firebaseToken);
}
