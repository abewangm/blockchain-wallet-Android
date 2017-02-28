package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.api.data.Settings;

import io.reactivex.Observable;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.SettingsService;

public class SettingsDataManager {

    private SettingsService settingsService;

    public SettingsDataManager(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Updates the settings object by syncing it with the server
     *
     * @param guid      The user's GUID
     * @param sharedKey The shared key
     * @return {@link Observable<Settings>} wrapping the Settings object
     */
    public Observable<Settings> initSettings(String guid, String sharedKey) {
        settingsService.initSettings(guid, sharedKey);
        return fetchSettings()
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Update the user's email and fetches an updated {@link Settings} object.
     *
     * @param email The email to be stored
     * @return {@link Observable<Settings>} wrapping the Settings object
     */
    public Observable<Settings> updateEmail(String email) {
        return settingsService.updateEmail(email)
                .flatMap(responseBody -> fetchSettings())
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Update the user's phone number and fetches an updated {@link Settings} object.
     *
     * @param sms The phone number to be stored
     * @return {@link Observable<Settings>} wrapping the Settings object
     */
    public Observable<Settings> updateSms(String sms) {
        return settingsService.updateSms(sms)
                .flatMap(responseBody -> fetchSettings())
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Verify the user's phone number with a verification code and fetches an updated {@link
     * Settings} object.
     *
     * @param code The verification code
     * @return {@link Observable<Settings>} wrapping the Settings object
     */
    public Observable<Settings> verifySms(String code) {
        return settingsService.verifySms(code)
                .flatMap(responseBody -> fetchSettings())
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Update the user's Tor blocking preference and fetches an updated {@link Settings} object.
     *
     * @param blocked The user's preference for blocking Tor requests
     * @return {@link Observable<Settings>} wrapping the Settings object
     */
    public Observable<Settings> updateTor(boolean blocked) {
        return settingsService.updateTor(blocked)
                .flatMap(responseBody -> fetchSettings())
                .compose(RxUtil.applySchedulersToObservable());
    }

//    /**
//     * Update the user's notification preferences and fetches an updated {@link Settings} object.
//     *
//     * @param notificationType The type of notification to enable
//     * @param enabled          Whether to enable or disable this particular notification type
//     * @return {@link Observable<Settings>} wrapping the Settings object
//     * @see SettingsDataManager
//     */
//    public Observable<ResponseBody> updateNotifications(int notificationType, boolean enabled) {
//        if (enabled) {
//            return settingsService.updateNotifications(notificationType)
//                    .compose(RxUtil.applySchedulersToObservable());
//        } else {
//            return settingsService.disableNotifications(notificationType)
//                    .compose(RxUtil.applySchedulersToObservable());
//        }
//    }

    /**
     * Update the user's two factor status
     *
     * @param authType The auth type being used for 2FA
     * @return {@link Observable<Settings>} wrapping the Settings object
     * @see SettingsDataManager
     */
    public Observable<Settings> updateTwoFactor(int authType) {
        return settingsService.updateTwoFactor(authType)
                .flatMap(responseBody -> fetchSettings())
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Fetches the latest user {@link Settings} object.
     *
     * @return A {@link Observable<Settings>} object
     */
    private Observable<Settings> fetchSettings() {
        return settingsService.getSettings();
    }
}
