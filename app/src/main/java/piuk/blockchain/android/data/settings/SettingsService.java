package piuk.blockchain.android.data.settings;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.settings.SettingsManager;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import piuk.blockchain.android.util.annotations.WebRequest;

public class SettingsService {

    private SettingsManager settingsApi;

    public SettingsService(SettingsManager settingsApi) {
        this.settingsApi = settingsApi;
    }

    /**
     * Fetches a new {@link Settings} object from the server and returns it as an Observable.
     * @return An {@link Observable<Settings>} containing the user's settings
     */
    @WebRequest
    public Observable<Settings> getSettingsObservable() {
        return Observable.defer(this::getSettings);
    }

    /**
     * Initializes the {@link SettingsManager} with the user's GUID and SharedKey.
     *
     * @param guid      The user's GUID
     * @param sharedKey The shared key
     */
    void initSettings(String guid, String sharedKey) {
        settingsApi.initSettings(guid, sharedKey);
    }

    /**
     * Fetches the latest {@link Settings} object for the user.
     *
     * @return An {@link Observable<Settings>} for the current user
     */
    @WebRequest
    Observable<Settings> getSettings() {
        return settingsApi.getInfo();
    }

    /**
     * Update the user's email
     *
     * @param email The email to be stored
     * @return An {@link Observable<ResponseBody>} containing the response from the server
     */
    @WebRequest
    Observable<ResponseBody> updateEmail(String email) {
        return settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_EMAIL, email);
    }

    /**
     * Update the user's phone number
     *
     * @param sms The phone number to be stored
     * @return An {@link Observable<ResponseBody>} containing the response from the server
     */
    @WebRequest
    Observable<ResponseBody> updateSms(String sms) {
        return settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_SMS, sms);
    }

    /**
     * Verify the user's phone number with a verification code
     *
     * @param code The verification code
     * @return An {@link Observable<ResponseBody>} containing the response from the server
     */
    @WebRequest
    Observable<ResponseBody> verifySms(String code) {
        return settingsApi.updateSetting(SettingsManager.METHOD_VERIFY_SMS, code);
    }

    /**
     * Update the user's Tor blocking preference
     *
     * @param blocked The user's preference for blocking Tor requests
     * @return An {@link Observable<ResponseBody>} containing the response from the server
     */
    @WebRequest
    Observable<ResponseBody> updateTor(boolean blocked) {
        return settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_BLOCK_TOR_IPS, blocked ? 1 : 0);
    }

    /**
     * Enable or disable a specific notification type for a user/
     *
     * @param notificationType The type of notification to enable
     * @return An {@link Observable<ResponseBody>} containing the response from the server
     * @see Settings
     */
    @WebRequest
    Observable<ResponseBody> updateNotifications(int notificationType) {
        return settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_NOTIFICATION_TYPE, notificationType);
    }

    /**
     * Enable or disable all notifications
     *
     * @param enable Whether or not to enable notifications
     * @return An {@link Observable<ResponseBody>} containing the response from the server
     * @see Settings
     */
    @WebRequest
    Observable<ResponseBody> enableNotifications(boolean enable) {
        return settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_NOTIFICATION_ON,
                enable ? SettingsManager.NOTIFICATION_ON : SettingsManager.NOTIFICATION_OFF);
    }

    /**
     * Update the user's two factor status
     *
     * @param authType The auth type being used for 2FA
     * @return An {@link Observable<ResponseBody>} containing the response from the server
     * @see Settings
     */
    @WebRequest
    Observable<ResponseBody> updateTwoFactor(int authType) {
        return settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_AUTH_TYPE, authType);
    }

    /**
     * Update the user's btc unit preference
     *
     * @param btcUnit The user's preference for btc unit
     * @return An {@link Observable<ResponseBody>} containing the response from the server
     */
    @WebRequest
    Observable<ResponseBody> updateBtcUnit(String btcUnit) {
        return settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_BTC_CURRENCY, btcUnit);
    }

    /**
     * Update the user's fiat unit preference
     *
     * @param fiatUnit The user's preference for fiat unit
     * @return An {@link Observable<ResponseBody>} containing the response from the server
     */
    @WebRequest
    Observable<ResponseBody> updateFiatUnit(String fiatUnit) {
        return settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_CURRENCY, fiatUnit);
    }
}
