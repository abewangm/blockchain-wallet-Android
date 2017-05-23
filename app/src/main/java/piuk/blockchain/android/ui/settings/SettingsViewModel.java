package piuk.blockchain.android.ui.settings;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.settings.SettingsManager;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.datamanagers.SettingsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.util.AndroidUtils;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

@SuppressWarnings("WeakerAccess")
public class SettingsViewModel extends BaseViewModel {

    @Inject protected FingerprintHelper fingerprintHelper;
    @Inject protected AuthDataManager authDataManager;
    @Inject protected SettingsDataManager settingsDataManager;
    @Inject protected PayloadManager payloadManager;
    @Inject protected PayloadDataManager payloadDataManager;
    @Inject protected StringUtils stringUtils;
    @Inject protected PrefsUtil prefsUtil;
    @Inject protected AccessState accessState;
    @VisibleForTesting Settings settings;
    private DataListener dataListener;
    private MonetaryUtil monetaryUtil;

    interface DataListener {

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

        void setSmsNotificationsVisibility(boolean visible);

        void setEmailNotificationPref(boolean enabled);

        void setSmsNotificationPref(boolean enabled);

        void setFingerprintVisibility(boolean visible);

        void setTwoFaPreference(boolean enabled);

        void setTwoFaSummary(String summary);

        void setTorBlocked(boolean blocked);

        void setScreenshotsEnabled(boolean enabled);

        void showDialogEmailVerification();

        void showDialogVerifySms();

        void showDialogSmsVerified();

        void goToPinEntryPage();

        void setLauncherShortcutVisibility(boolean visible);

        void showWarningDialog(@StringRes int message);
    }

    SettingsViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
    }

    @Override
    public void onViewReady() {
        dataListener.showProgressDialog(R.string.please_wait);
        // Fetch updated settings
        compositeDisposable.add(
                settingsDataManager.initSettings(
                        prefsUtil.getValue(PrefsUtil.KEY_GUID, ""),
                        prefsUtil.getValue(PrefsUtil.KEY_SHARED_KEY, ""))
                        .subscribe(
                                updatedSettings -> {
                                    settings = updatedSettings;
                                    handleUpdate();
                                },
                                throwable -> {
                                    if (settings == null) {
                                        // Show unloaded if necessary, keep old settings if failed update
                                        settings = new Settings();
                                    }
                                    handleUpdate();
                                    // Warn error when updating
                                    dataListener.showToast(R.string.settings_error_updating, ToastCustom.TYPE_ERROR);
                                }));
    }

    private void handleUpdate() {
        dataListener.hideProgressDialog();
        dataListener.setUpUi();
        updateUi();
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
        String smsAndStatus = settings.getSmsNumber();
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

        if (settings.isNotificationsOn() && !settings.getNotificationsType().isEmpty()) {
            for (int type : settings.getNotificationsType()) {
                if (type == Settings.NOTIFICATION_TYPE_EMAIL) {
                    dataListener.setEmailNotificationPref(true);
                }

                if (type == Settings.NOTIFICATION_TYPE_SMS) {
                    dataListener.setSmsNotificationPref(true);
                }

                if (type == Settings.NOTIFICATION_TYPE_ALL) {
                    dataListener.setSmsNotificationPref(true);
                    dataListener.setEmailNotificationPref(true);
                    break;
                }
            }
        }

        // Fingerprint
        dataListener.setFingerprintVisibility(getIfFingerprintHardwareAvailable());
        dataListener.updateFingerprintPreferenceStatus();

        // 2FA
        dataListener.setTwoFaPreference(settings.getAuthType() != Settings.AUTH_TYPE_OFF);
        dataListener.setTwoFaSummary(getTwoFaSummary(settings.getAuthType()));

        // Tor
        dataListener.setTorBlocked(settings.isBlockTorIps());

        // Screenshots
        dataListener.setScreenshotsEnabled(prefsUtil.getValue(PrefsUtil.KEY_SCREENSHOTS_ENABLED, false));

        // Launcher shortcuts
        dataListener.setLauncherShortcutVisibility(AndroidUtils.is25orHigher());
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

    /**
     * @return true if the device has usable fingerprint hardware
     */
    boolean getIfFingerprintHardwareAvailable() {
        return fingerprintHelper.isHardwareDetected();
    }

    /**
     * @return true if the user has previously enabled fingerprint login
     */
    boolean getIfFingerprintUnlockEnabled() {
        return fingerprintHelper.isFingerprintUnlockEnabled();
    }

    /**
     * Sets fingerprint unlock enabled and clears the encrypted PIN if {@param enabled} is false
     *
     * @param enabled Whether or not the fingerprint unlock feature is set up
     */
    void setFingerprintUnlockEnabled(boolean enabled) {
        fingerprintHelper.setFingerprintUnlockEnabled(enabled);
        if (!enabled) {
            fingerprintHelper.clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE);
        }
    }

    /**
     * Handle fingerprint preference toggle
     */
    void onFingerprintClicked() {
        if (getIfFingerprintUnlockEnabled()) {
            // Show dialog "are you sure you want to disable fingerprint login?
            dataListener.showDisableFingerprintDialog();
        } else if (!fingerprintHelper.areFingerprintsEnrolled()) {
            // No fingerprints enrolled, prompt user to add some
            dataListener.showNoFingerprintsAddedDialog();
        } else {
            if (accessState.getPIN() != null && !accessState.getPIN().isEmpty()) {
                dataListener.showFingerprintDialog(accessState.getPIN());
            } else {
                throw new IllegalStateException("PIN code not found in AccessState");
            }
        }
    }

    private boolean isStringValid(String string) {
        return string != null && !string.isEmpty() && string.length() < 256;
    }

    /**
     * @return position of user's BTC unit preference
     */
    int getBtcUnitsPosition() {
        return prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
    }

    /**
     * @return the user's preferred BTC units
     */
    @NonNull
    String getDisplayUnits() {
        return (String) monetaryUtil.getBTCUnits()[getBtcUnitsPosition()];
    }

    /**
     * @return an array of possible BTC units
     */
    @NonNull
    CharSequence[] getBtcUnits() {
        return monetaryUtil.getBTCUnits();
    }

    /**
     * @return the user's preferred Fiat currency unit
     */
    @NonNull
    String getFiatUnits() {
        return prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
    }

    /**
     * @return the temporary password from the Payload Manager
     */
    @NonNull
    String getTempPassword() {
        return payloadManager.getTempPassword();
    }

    /**
     * @return the user's email or an empty string if not set
     */
    @NonNull
    String getEmail() {
        return settings.getEmail() != null ? settings.getEmail() : "";
    }

    /**
     * @return the user's phone number or an empty string if not set
     */
    @NonNull
    String getSms() {
        return settings.getSmsNumber() != null ? settings.getSmsNumber() : "";
    }

    /**
     * @return is the user's phone number is verified
     */
    boolean isSmsVerified() {
        return settings.isSmsVerified();
    }

    /**
     * @return the current auth type
     * @see Settings
     */
    int getAuthType() {
        return settings.getAuthType();
    }

    /**
     * Write key/value to {@link android.content.SharedPreferences}
     *
     * @param key   The key under which to store the data
     * @param value The value to be stored as a String
     */
    void updatePreferences(String key, String value) {
        prefsUtil.setValue(key, value);
        updateUi();
    }

    /**
     * Write key/value to {@link android.content.SharedPreferences}
     *
     * @param key   The key under which to store the data
     * @param value The value to be stored as an int
     */
    void updatePreferences(String key, int value) {
        prefsUtil.setValue(key, value);
        updateUi();
    }

    /**
     * Write key/value to {@link android.content.SharedPreferences}
     *
     * @param key   The key under which to store the data
     * @param value The value to be stored as a boolean
     */
    void updatePreferences(String key, boolean value) {
        prefsUtil.setValue(key, value);
        updateUi();
    }

    /**
     * Updates the user's email, prompts user to check their email for verification after success
     *
     * @param email The email address to be saved
     */
    void updateEmail(String email) {
        if (!isStringValid(email)) {
            dataListener.setEmailSummary(stringUtils.getString(R.string.not_specified));
        } else {
            compositeDisposable.add(
                    settingsDataManager.updateEmail(email)
                            .subscribe(settings -> {
                                this.settings = settings;
                                updateNotification(Settings.NOTIFICATION_TYPE_EMAIL, false);
                                dataListener.showDialogEmailVerification();
                            }, throwable -> dataListener.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
        }
    }

    /**
     * Updates the user's phone number, prompts user to verify their number after success
     *
     * @param sms The phone number to be saved
     */
    void updateSms(String sms) {
        if (!isStringValid(sms)) {
            dataListener.setSmsSummary(stringUtils.getString(R.string.not_specified));
        } else {
            compositeDisposable.add(
                    settingsDataManager.updateSms(sms)
                            .subscribe(settings -> {
                                this.settings = settings;
                                updateNotification(Settings.NOTIFICATION_TYPE_SMS, false);
                                dataListener.showDialogVerifySms();
                            }, throwable -> dataListener.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
        }
    }

    /**
     * Verifies a user's number, shows verified dialog after success
     *
     * @param code The verification code which has been sent to the user
     */
    void verifySms(@NonNull String code) {
        dataListener.showProgressDialog(R.string.please_wait);
        compositeDisposable.add(
                settingsDataManager.verifySms(code)
                        .doAfterTerminate(() -> {
                            dataListener.hideProgressDialog();
                            updateUi();
                        })
                        .subscribe(settings -> {
                            this.settings = settings;
                            dataListener.showDialogSmsVerified();
                        }, throwable -> dataListener.showWarningDialog(R.string.verify_sms_failed)));
    }

    /**
     * Updates the user's Tor blocking preference
     *
     * @param blocked Whether or not to block Tor requests
     */
    void updateTor(boolean blocked) {
        compositeDisposable.add(
                settingsDataManager.updateTor(blocked)
                        .doAfterTerminate(this::updateUi)
                        .subscribe(
                                settings -> this.settings = settings,
                                throwable -> dataListener.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
    }

    /**
     * Sets the auth type used for 2FA. Pass in {@link Settings#AUTH_TYPE_OFF} to disable 2FA
     *
     * @param type The auth type used for 2FA
     * @see Settings
     */
    void updateTwoFa(int type) {
        compositeDisposable.add(
                settingsDataManager.updateTwoFactor(type)
                        .doAfterTerminate(this::updateUi)
                        .subscribe(
                                settings -> this.settings = settings,
                                throwable -> dataListener.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
    }

    /**
     * Updates the user's notification preferences. Will not make any web requests if not necessary.
     *
     * @param type   The notification type to be updated
     * @param enable Whether or not to enable the notification type
     * @see Settings
     */
    void updateNotification(int type, boolean enable) {
        if (enable && isNotificationTypeEnabled(type)) {
            // No need to change
            updateUi();
            return;
        } else if (!enable && isNotificationTypeDisabled(type)) {
            // No need to change
            updateUi();
            return;
        }

        compositeDisposable.add(
                Observable.just(enable)
                        .flatMap(aBoolean -> {
                            if (aBoolean) {
                                return settingsDataManager.enableNotification(type, settings.getNotificationsType());
                            } else {
                                return settingsDataManager.disableNotification(type, settings.getNotificationsType());
                            }
                        })
                        .doOnNext(settings -> this.settings = settings)
                        .flatMapCompletable(ignored -> {
                            if (enable) {
                                return payloadDataManager.syncPayloadAndPublicKeys();
                            } else {
                                return payloadDataManager.syncPayloadWithServer();
                            }
                        })
                        .doAfterTerminate(this::updateUi)
                        .subscribe(
                                () -> {
                                    // No-op
                                },
                                throwable -> dataListener.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
    }

    private boolean isNotificationTypeEnabled(int type) {
        return settings.isNotificationsOn()
                && (settings.getNotificationsType().contains(type)
                || settings.getNotificationsType().contains(SettingsManager.NOTIFICATION_TYPE_ALL));
    }

    private boolean isNotificationTypeDisabled(int type) {
        return settings.getNotificationsType().contains(SettingsManager.NOTIFICATION_TYPE_NONE)
                || (!settings.getNotificationsType().contains(SettingsManager.NOTIFICATION_TYPE_ALL)
                && !settings.getNotificationsType().contains(type));
    }

    /**
     * PIN code validated, take user to PIN change page
     */
    void pinCodeValidatedForChange() {
        prefsUtil.removeValue(PrefsUtil.KEY_PIN_FAILS);
        prefsUtil.removeValue(PrefsUtil.KEY_PIN_IDENTIFIER);

        dataListener.goToPinEntryPage();
    }

    /**
     * Updates the user's password
     *
     * @param password         The requested new password as a {@link String}
     * @param fallbackPassword The user's current password as a fallback
     */
    void updatePassword(@NonNull String password, @NonNull String fallbackPassword) {
        dataListener.showProgressDialog(R.string.please_wait);
        payloadManager.setTempPassword(password);

        compositeDisposable.add(
                authDataManager.createPin(password, accessState.getPIN())
                        .andThen(payloadDataManager.syncPayloadWithServer())
                        .doAfterTerminate(() -> dataListener.hideProgressDialog())
                        .subscribe(
                                () -> dataListener.showToast(R.string.password_changed, ToastCustom.TYPE_OK),
                                throwable -> showUpdatePasswordFailed(fallbackPassword)));
    }

    private void showUpdatePasswordFailed(@NonNull String fallbackPassword) {
        payloadManager.setTempPassword(fallbackPassword);

        dataListener.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR);
        dataListener.showToast(R.string.password_unchanged, ToastCustom.TYPE_ERROR);
    }
}
