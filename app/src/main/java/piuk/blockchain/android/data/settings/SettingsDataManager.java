package piuk.blockchain.android.data.settings;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.settings.SettingsManager;

import java.util.List;

import io.reactivex.Observable;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxPinning;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.settings.datastore.SettingsDataStore;

public class SettingsDataManager {

    private SettingsService settingsService;
    private SettingsDataStore settingsDataStore;
    private RxPinning rxPinning;

    public SettingsDataManager(SettingsService settingsService,
                               SettingsDataStore settingsDataStore,
                               RxBus rxBus) {
        this.settingsService = settingsService;
        this.settingsDataStore = settingsDataStore;
        rxPinning = new RxPinning(rxBus);
    }

    /**
     * Updates the settings object by syncing it with the server. Must be called to set up the
     * {@link SettingsManager} class before a fetch is called.
     *
     * @param guid      The user's GUID
     * @param sharedKey The shared key
     * @return {@link Observable<Settings>} wrapping the Settings object
     */
    public Observable<Settings> initSettings(String guid, String sharedKey) {
        settingsService.initSettings(guid, sharedKey);
        return rxPinning.call(this::fetchSettings)
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Grabs the latest user {@link Settings} object from memory, or makes a web request if not
     * available.
     *
     * @return An {@link Observable<Settings>} object
     */
    public Observable<Settings> getSettings() {
        return rxPinning.call(this::attemptFetchSettingsFromMemory);
    }

    /**
     * Fetches the latest user {@link Settings} object from the server
     *
     * @return An {@link Observable<Settings>} object
     */
    private Observable<Settings> fetchSettings() {
        return rxPinning.call(this::fetchSettingsFromWeb);
    }

    /**
     * Update the user's email and fetches an updated {@link Settings} object.
     *
     * @param email The email to be stored
     * @return {@link Observable<Settings>} wrapping the Settings object
     */
    public Observable<Settings> updateEmail(String email) {
        return rxPinning.call(() -> settingsService.updateEmail(email))
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
        return rxPinning.call(() -> settingsService.updateSms(sms))
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
        return rxPinning.call(() -> settingsService.verifySms(code))
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
        return rxPinning.call(() -> settingsService.updateTor(blocked))
                .flatMap(responseBody -> fetchSettings())
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Update the user's two factor status
     *
     * @param authType The auth type being used for 2FA
     * @return {@link Observable<Settings>} wrapping the Settings object
     * @see SettingsManager for notification types
     */
    public Observable<Settings> updateTwoFactor(int authType) {
        return rxPinning.call(() -> settingsService.updateTwoFactor(authType))
                .flatMap(responseBody -> fetchSettings())
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Update the user's notification preferences and fetches an updated {@link Settings} object.
     *
     * @param notificationType The type of notification to enable
     * @param notifications    An ArrayList of the currently enabled notifications
     * @return {@link Observable<Settings>} wrapping the Settings object
     * @see SettingsManager for notification types
     */
    public Observable<Settings> enableNotification(int notificationType, List<Integer> notifications) {
        if (notifications.isEmpty() || notifications.contains(SettingsManager.NOTIFICATION_TYPE_NONE)) {
            // No notification type registered, enable
            return rxPinning.call(() -> settingsService.enableNotifications(true))
                    .flatMap(responseBody -> updateNotifications(notificationType))
                    .compose(RxUtil.applySchedulersToObservable());
        } else if (notifications.size() == 1
                && ((notifications.contains(SettingsManager.NOTIFICATION_TYPE_EMAIL)
                && notificationType == SettingsManager.NOTIFICATION_TYPE_SMS)
                || (notifications.contains(SettingsManager.NOTIFICATION_TYPE_SMS)
                && notificationType == SettingsManager.NOTIFICATION_TYPE_EMAIL))) {
            // Contains another type already, send "All"
            return rxPinning.call(() -> settingsService.enableNotifications(true))
                    .flatMap(responseBody -> updateNotifications(SettingsManager.NOTIFICATION_TYPE_ALL))
                    .compose(RxUtil.applySchedulersToObservable());
        } else {
            return rxPinning.call(() -> settingsService.enableNotifications(true))
                    .flatMap(responseBody -> fetchSettings())
                    .compose(RxUtil.applySchedulersToObservable());
        }
    }

    /**
     * Update the user's notification preferences and fetches an updated {@link Settings} object.
     *
     * @param notificationType The type of notification to disable
     * @param notifications    An ArrayList of the currently enabled notifications
     * @return {@link Observable<Settings>} wrapping the Settings object
     * @see SettingsManager for notification types
     */
    public Observable<Settings> disableNotification(int notificationType, List<Integer> notifications) {
        if (notifications.isEmpty() || notifications.contains(SettingsManager.NOTIFICATION_TYPE_NONE)) {
            // No notifications anyway, return Settings
            return rxPinning.call(this::fetchSettings)
                    .compose(RxUtil.applySchedulersToObservable());
        } else if (notifications.contains(SettingsManager.NOTIFICATION_TYPE_ALL)
                || (notifications.contains(SettingsManager.NOTIFICATION_TYPE_EMAIL)
                && notifications.contains(SettingsManager.NOTIFICATION_TYPE_SMS))) {
            // All types enabled, disable passed type and enable other
            return updateNotifications(notificationType == SettingsManager.NOTIFICATION_TYPE_EMAIL
                    ? SettingsManager.NOTIFICATION_TYPE_SMS : SettingsManager.NOTIFICATION_TYPE_EMAIL)
                    .compose(RxUtil.applySchedulersToObservable());
        } else if (notifications.size() == 1) {
            if (notifications.get(0).equals(notificationType)) {
                // Remove all
                return rxPinning.call(() -> settingsService.enableNotifications(false))
                        .flatMap(responseBody -> updateNotifications(SettingsManager.NOTIFICATION_TYPE_NONE))
                        .compose(RxUtil.applySchedulersToObservable());
            } else {
                // Notification type not present, no need to remove it
                return rxPinning.call(this::fetchSettings)
                        .compose(RxUtil.applySchedulersToObservable());
            }
        } else {
            // This should never be reached
            return rxPinning.call(this::fetchSettings)
                    .compose(RxUtil.applySchedulersToObservable());
        }
    }

    /**
     * Updates a passed notification type and then fetches the current settings object.
     *
     * @param notificationType The notification type you wish to enable/disable
     * @return {@link Observable<Settings>} wrapping the Settings object
     * @see SettingsManager for notification types
     */
    private Observable<Settings> updateNotifications(int notificationType) {
        return rxPinning.call(() -> settingsService.updateNotifications(notificationType))
                .flatMap(responseBody -> fetchSettings())
                .compose(RxUtil.applySchedulersToObservable());
    }

    private Observable<Settings> fetchSettingsFromWeb() {
        return Observable.defer(() -> settingsDataStore.fetchSettings());
    }

    private Observable<Settings> attemptFetchSettingsFromMemory() {
        return Observable.defer(() -> settingsDataStore.getSettings());
    }

    /**
     * Update the user's btcUnit unit preference and fetches an updated {@link Settings} object.
     *
     * @param btcUnit The user's preference for btcUnit unit
     * @return {@link Observable<Settings>} wrapping the Settings object
     */
    public Observable<Settings> updateBtcUnit(String btcUnit) {
        return rxPinning.call(() -> settingsService.updateBtcUnit(btcUnit))
                .flatMap(responseBody -> fetchSettings())
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Update the user's fiat unit preference and fetches an updated {@link Settings} object.
     *
     * @param fiatUnit The user's preference for fiat unit
     * @return {@link Observable<Settings>} wrapping the Settings object
     */
    public Observable<Settings> updateFiatUnit(String fiatUnit) {
        return rxPinning.call(() -> settingsService.updateFiatUnit(fiatUnit))
                .flatMap(responseBody -> fetchSettings())
                .compose(RxUtil.applySchedulersToObservable());
    }
}
