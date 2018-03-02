package piuk.blockchain.android.data.notifications;

import com.google.common.base.Optional;
import com.google.firebase.iid.FirebaseInstanceId;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Wallet;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.access.AuthEvent;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.util.PrefsUtil;
import timber.log.Timber;

public class NotificationTokenManager {

    private NotificationService notificationService;
    private AccessState accessState;
    private PayloadManager payloadManager;
    private PrefsUtil prefsUtil;
    private FirebaseInstanceId firebaseInstanceId;
    private RxBus rxBus;

    private Observable loginObservable;

    public NotificationTokenManager(NotificationService notificationService,
                                    AccessState accessState,
                                    PayloadManager payloadManager,
                                    PrefsUtil prefsUtil,
                                    FirebaseInstanceId firebaseInstanceId,
                                    RxBus rxBus) {
        this.notificationService = notificationService;
        this.accessState = accessState;
        this.payloadManager = payloadManager;
        this.prefsUtil = prefsUtil;
        this.firebaseInstanceId = firebaseInstanceId;
        this.rxBus = rxBus;
    }

    /**
     * Sends the access token to the update-firebase endpoint once the user is logged in fully, as
     * the token is generally only generated on first install or first start after updating.
     *
     * @param token A Firebase access token
     */
    void storeAndUpdateToken(@NonNull String token) {
        if (accessState.isLoggedIn()) {
            // Send token
            sendFirebaseToken(token)
                    .subscribeOn(Schedulers.io());
        } else {
            // Store token and send once login event happens
            registerAuthEvent();
        }

        Timber.d("vos storing1 "+token);
        prefsUtil.setValue(PrefsUtil.KEY_FIREBASE_TOKEN, token);
    }

    private void registerAuthEvent() {
        loginObservable = rxBus.register(AuthEvent.class);
        loginObservable
                .compose(RxUtil.applySchedulersToObservable())
                .flatMapCompletable(authEvent -> {
                    if (authEvent == AuthEvent.LOGIN) {
                        Timber.d("vos LOGIN");
                        String storedToken = prefsUtil.getValue(PrefsUtil.KEY_FIREBASE_TOKEN, "");

                        if (!storedToken.isEmpty()) {
                            Timber.d("vos sendFirebaseToken");
                            return sendFirebaseToken(storedToken);
                        } else {
                            Timber.d("vos resendNotificationToken");
                            return resendNotificationToken();
                        }

                    } else if (authEvent == AuthEvent.UNPAIR) {
                        Timber.d("vos UNPAIR");
                        prefsUtil.removeValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED);
                        return revokeAccessToken();
                    } else {
                        return Completable.complete();
                    }
                })
                .subscribe(() -> {
                    //no-op
                });
    }

    /**
     * Returns the stored Firebase token, otherwise attempts to trigger a refresh of the token which
     * will be handled appropriately by {@link FcmCallbackService}
     *
     * @return The Firebase token
     */
    @Nullable
    private Observable<Optional<String>> getStoredFirebaseToken() {

        String storedToken = prefsUtil.getValue(PrefsUtil.KEY_FIREBASE_TOKEN, "");

        if (!storedToken.isEmpty()) {
            return Observable.just(Optional.of(storedToken));
        } else {
            return Observable.fromCallable(() -> {
                firebaseInstanceId.getToken();
                return Optional.absent();
            });
        }
    }

    /**
     * Disables push notifications flag.
     * Resets Instance ID and revokes all tokens. Clears stored token if successful
     */
    public Completable disableNotifications() {
        prefsUtil.setValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, false);
        return revokeAccessToken();
    }

    /**
     * Resets Instance ID and revokes all tokens. Clears stored token if successful
     */
    private Completable revokeAccessToken() {
        return Completable.fromCallable(() -> {
            firebaseInstanceId.deleteInstanceId();
            return Void.TYPE;
        })
                .andThen(removeNotificationToken())
                .doOnComplete(this::clearStoredToken)
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Enables push notifications flag.
     * Resend stored notification token, or generate and send new token if no stored token exists.
     */
    public Completable enableNotifications() {
        prefsUtil.setValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true);
        return resendNotificationToken();
    }

    /**
     * Re-sends the notification token. The token is only ever generated on app installation, so it
     * may be preferable to store the token and resend it on startup rather than have an app end up
     * in a state where it may not have registered with the right endpoint or similar.
     * <p>
     * If no stored notification token exists, it will be refreshed
     * and will be handled appropriately by {@link FcmCallbackService}
     */
    private Completable resendNotificationToken() {
        return getStoredFirebaseToken()
                .flatMapCompletable(optional -> {
                    if (optional.isPresent()) {
                        return sendFirebaseToken(optional.get());
                    } else {
                        return Completable.complete();
                    }
                });
    }

    private Completable sendFirebaseToken(@NonNull String refreshedToken) {

        if (prefsUtil.getValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true)) {

            Wallet payload = payloadManager.getPayload();
            String guid = payload.getGuid();
            String sharedKey = payload.getSharedKey();

            // TODO: 09/11/2016 Decide what to do if sending fails, perhaps retry?
            Timber.d("vos sendNotificationToken "+refreshedToken);
            return notificationService.sendNotificationToken(refreshedToken, guid, sharedKey);
        } else {
            return Completable.complete();
        }
    }

    /**
     * Removes the stored token from Shared Preferences
     */
    private void clearStoredToken() {
        prefsUtil.removeValue(PrefsUtil.KEY_FIREBASE_TOKEN);
    }

    /**
     * Removes the stored token from back end
     */
    private Completable removeNotificationToken() {

        String token = prefsUtil.getValue(PrefsUtil.KEY_FIREBASE_TOKEN, "");

        if (!token.isEmpty()) {
            return notificationService.removeNotificationToken(token);
        } else {
            return Completable.complete();
        }
    }
}
