package piuk.blockchain.android.ui.settings;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import info.blockchain.api.Settings;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.CharSequenceX;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.SettingsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;
import rx.exceptions.Exceptions;

public class SettingsViewModel extends BaseViewModel {

    @Inject FingerprintHelper fingerprintHelper;
    @Inject SettingsDataManager settingsDataManager;
    @Inject PayloadManager payloadManager;
    @Inject StringUtils stringUtils;
    @Inject PrefsUtil prefsUtil;
    private DataListener dataListener;
    private MonetaryUtil monetaryUtil;
    private Settings settings;

    interface DataListener {

        void verifyPinCode();

        void showFingerprintDialog(CharSequenceX pincode);

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

        void setSmsNotificationsVisibility(boolean visible);

        void setEmailNotificationPref(boolean enabled);

        void setSmsNotificationPref(boolean enabled);

        void setFingerprintVisibility(boolean visible);

        void setTwoFaPreference(boolean enabled);

        void setTwoFaSummary(String summary);

        void setPasswordHintSummary(String summary);

        void setTorBlocked(boolean blocked);

        void showDialogEmailVerification();

    }

    SettingsViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
    }

    @Override
    public void onViewReady() {
        // Fetch updated settings
        mCompositeSubscription.add(
                settingsDataManager.updateSettings(
                        payloadManager.getPayload().getGuid(),
                        payloadManager.getPayload().getSharedKey())
                        .doOnTerminate(() -> dataListener.hideProgressDialog())
                        .subscribe(settings -> {
                            this.settings = settings;
                            updateUi();
                        }, throwable -> {
                            // This isn't ideal
                            settings = new Settings();
                        }));
    }

    private void updateUi() {
        // GUID
        dataListener.setGuidSummary(settings.getGuid());

        // Email
        String emailAndStatus = settings.getEmail();
        if (emailAndStatus == null || emailAndStatus.isEmpty()) {
            emailAndStatus = stringUtils.getString(R.string.not_specified);
        } else if (settings.isEmailVerified()) {
            emailAndStatus += "  (" + stringUtils.getString(R.string.verified) + ")";
        } else {
            emailAndStatus += "  (" + stringUtils.getString(R.string.unverified) + ")";
        }
        dataListener.setEmailSummary(emailAndStatus);

        // Phone
        String smsAndStatus = settings.getSms();
        if (smsAndStatus == null || smsAndStatus.isEmpty()) {
            smsAndStatus = stringUtils.getString(R.string.not_specified);
        } else if (settings.isSmsVerified()) {
            smsAndStatus += "  (" + stringUtils.getString(R.string.verified) + ")";
        } else {
            smsAndStatus += "  (" + stringUtils.getString(R.string.unverified) + ")";
        }
        dataListener.setSmsSummary(smsAndStatus);

        // Units
        dataListener.setUnitsSummary(getDisplayUnits());

        // Fiat
        dataListener.setFiatSummary(getFiatUnits());

        // Email notifications
        dataListener.setEmailNotificationsVisibility(settings.isEmailVerified());

        // SMS notifications
        dataListener.setSmsNotificationsVisibility(settings.isSmsVerified());

        // SMS and Email notification status
        dataListener.setEmailNotificationPref(false);
        dataListener.setSmsNotificationPref(false);

        if (settings.isNotificationsOn() && settings.getNotificationTypes().size() > 0) {
            for (int type : settings.getNotificationTypes()) {
                if (type == Settings.NOTIFICATION_TYPE_EMAIL) {
                    dataListener.setEmailNotificationPref(true);
                }

                if (type == Settings.NOTIFICATION_TYPE_SMS) {
                    dataListener.setEmailNotificationPref(true);
                }
            }
        }

        // Fingerprint
        dataListener.setFingerprintVisibility(getIfFingerprintHardwareAvailable());
        dataListener.updateFingerprintPreferenceStatus();

        // 2FA
        dataListener.setTwoFaPreference(settings.getAuthType() != Settings.AUTH_TYPE_OFF);
        dataListener.setTwoFaSummary(getTwoFaSummary(settings.getAuthType()));

        // Password hint
        if (settings.getPasswordHint1() != null && !settings.getPasswordHint1().isEmpty()) {
            dataListener.setPasswordHintSummary(settings.getPasswordHint1());
        } else {
            dataListener.setPasswordHintSummary("");
        }

        // Tor
        dataListener.setTorBlocked(settings.isTorBlocked());

    }

    private String getTwoFaSummary(int type) {
        String summary;
        switch (type) {
            case Settings.AUTH_TYPE_GOOGLE_AUTHENTICATOR:
                summary = stringUtils.getString(R.string.google_authenticator);
                break;
            case Settings.AUTH_TYPE_SMS:
                summary = stringUtils.getString(R.string.sms);
                break;
            case Settings.AUTH_TYPE_YUBI_KEY:
                summary = stringUtils.getString(R.string.yubikey);
                break;
            default:
                summary = "";
                break;
        }
        return summary;
    }

    String getDisplayUnits() {
        return (String) monetaryUtil.getBTCUnits()[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];
    }

    CharSequence[] getBtcUnits() {
        return monetaryUtil.getBTCUnits();
    }

    private String getFiatUnits() {
        return prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
    }

    boolean getIfFingerprintHardwareAvailable() {
        return fingerprintHelper.isHardwareDetected();
    }

    /**
     * Returns true if the user has previously enabled fingerprint login
     */
    boolean getIfFingerprintUnlockEnabled() {
        return fingerprintHelper.getIfFingerprintUnlockEnabled();
    }

    void setFingerprintUnlockEnabled(boolean enabled) {
        fingerprintHelper.setFingerprintUnlockEnabled(enabled);
        if (!enabled) {
            fingerprintHelper.clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE);
        }
    }

    void onFingerprintClicked() {
        if (getIfFingerprintUnlockEnabled()) {
            // Show dialog "are you sure you want to disable fingerprint login?
            dataListener.showDisableFingerprintDialog();
        } else if (!fingerprintHelper.areFingerprintsEnrolled()) {
            // No fingerprints enrolled, prompt user to add some
            dataListener.showNoFingerprintsAddedDialog();
        } else {
            // Verify PIN before continuing
            dataListener.verifyPinCode();
        }
    }

    void pinCodeValidated(CharSequenceX pinCode) {
        dataListener.showFingerprintDialog(pinCode);
    }

    private boolean isStringValid(String string) {
        return string != null && !string.isEmpty();
    }

    // TODO: 18/10/2016 Move these elsewhere
    String getEmail() {
        return settings.getEmail();
    }

    @NonNull
    String getSms() {
        return settings.getSms() != null ? settings.getSms() : "";
    }

    @NonNull
    String getPasswordHint() {
        return settings.getPasswordHint1() != null ? settings.getPasswordHint1() : "";
    }

    boolean isSmsVerified() {
        return settings.isSmsVerified();
    }

    int getAuthType() {
        return settings.getAuthType();
    }

    // TODO: 18/10/2016 ^^^^ Move these elsewhere

    void updateEmail(String email) {
        if (!isStringValid(email)) {
            dataListener.setEmailSummary(stringUtils.getString(R.string.not_specified));
        } else {
            mCompositeSubscription.add(
                    settingsDataManager.updateEmail(email)
                            .subscribe(success -> {
                                if (success) {
                                    updateNotification(Settings.NOTIFICATION_TYPE_EMAIL, false);
                                    dataListener.showDialogEmailVerification();
                                } else {
                                    throw Exceptions.propagate(new Throwable("Update email failed"));
                                }
                            }, throwable -> {
                                dataListener.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR);
                            }));
        }
    }

    void updateSms(String sms) {
        
    }

    void updateNotification(int type, boolean enabled) {
        mCompositeSubscription.add(
                settingsDataManager.updateNotifications(type, enabled)
                .subscribe(success -> {
                    if (success) {
                        updateUi();
                    } else {
                        throw Exceptions.propagate(new Throwable("Update notification failed"));
                    }
                }, throwable -> {
                    dataListener.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR);
                }));
    }
}
