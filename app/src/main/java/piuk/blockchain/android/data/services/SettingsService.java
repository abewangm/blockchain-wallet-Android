package piuk.blockchain.android.data.services;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.settings.SettingsManager;

import io.reactivex.Observable;
import okhttp3.ResponseBody;

public class SettingsService {

    private SettingsManager settingsApi;

    public SettingsService(SettingsManager settingsApi) {
        this.settingsApi = settingsApi;
    }

    /**
     * Initializes the {@link SettingsManager} with the user's GUID and SharedKey.
     *
     * @param guid      The user's GUID
     * @param sharedKey The shared key
     */
    public void initSettings(String guid, String sharedKey) {
        settingsApi.initSettings(guid, sharedKey);
    }

    /**
     * Fetches the latest {@link Settings} object for the user
     *
     * @return An {@link Observable<Settings>} for the current user
     */
    public Observable<Settings> getSettings() {
        return settingsApi.getInfo();
    }

    /**
     * Update the user's email
     *
     * @param email The email to be stored
     * @return An {@link Observable<ResponseBody>} containing the response from the server
     */
    public Observable<ResponseBody> updateEmail(String email) {
        return settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_EMAIL, email);
    }

    /**
     * Update the user's phone number
     *
     * @param sms The phone number to be stored
     * @return An {@link Observable<ResponseBody>} containing the response from the server
     */
    public Observable<ResponseBody> updateSms(String sms) {
        return settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_SMS, sms);
    }

    /**
     * Verify the user's phone number with a verification code
     *
     * @param code The verification code
     * @return An {@link Observable<ResponseBody>} containing the response from the server
     */
    public Observable<ResponseBody> verifySms(String code) {
        return settingsApi.updateSetting(SettingsManager.METHOD_VERIFY_SMS, code);
    }

    /**
     * Update the user's Tor blocking preference
     *
     * @param blocked The user's preference for blocking Tor requests
     * @return An {@link Observable<ResponseBody>} containing the response from the server
     */
    public Observable<ResponseBody> updateTor(boolean blocked) {
        return settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_BLOCK_TOR_IPS, blocked ? 1 : 0);
    }

    /**
     * Enable a specific notification type for a user
     *
     * @param notificationType The type of notification to enable
     * @return An {@link Observable<ResponseBody>} containing the response from the server
     * @see Settings
     */
    public Observable<ResponseBody> updateNotifications(int notificationType) {
        return settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_NOTIFICATION_TYPE, notificationType);
    }

    /**
     * Update the user's two factor status
     *
     * @param authType The auth type being used for 2FA
     * @return An {@link Observable<ResponseBody>} containing the response from the server
     * @see Settings
     */
    public Observable<ResponseBody> updateTwoFactor(int authType) {
        return settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_AUTH_TYPE, authType);
    }

}
