package piuk.blockchain.android.data.notifications;

import com.google.firebase.iid.FirebaseInstanceId;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;

import io.reactivex.Completable;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.NotificationService;
import piuk.blockchain.android.util.PrefsUtil;

public class NotificationTokenManager {

    private static final String TAG = NotificationTokenManager.class.getSimpleName();

    private NotificationService notificationService;
    private AccessState accessState;
    private PayloadManager payloadManager;
    private PrefsUtil prefsUtil;

    public NotificationTokenManager(NotificationService notificationService, AccessState accessState, PayloadManager payloadManager, PrefsUtil prefsUtil) {
        this.notificationService = notificationService;
        this.accessState = accessState;
        this.payloadManager = payloadManager;
        this.prefsUtil = prefsUtil;
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
            sendFirebaseToken(token);
        } else {
            // Store token and send once login event happens
            accessState.getAuthEventSubject().subscribe(authEvent -> {
                if (authEvent == AccessState.AuthEvent.Login) {
                    // Send token
                    sendFirebaseToken(token);
                }
            }, Throwable::printStackTrace);
        }

        prefsUtil.setValue(PrefsUtil.KEY_FIREBASE_TOKEN, token);
    }

    // TODO: 28/11/2016 This may want to be transformed into an Observable?

    /**
     * Returns the stored Firebase token, otherwise attempts to trigger a refresh of the token which
     * will be handled appropriately by {@link FcmCallbackService}
     *
     * @return The Firebase token
     */
    @Nullable
    public String getFirebaseToken() {
        return !prefsUtil.getValue(PrefsUtil.KEY_FIREBASE_TOKEN, "").isEmpty()
                ? prefsUtil.getValue(PrefsUtil.KEY_FIREBASE_TOKEN, "")
                : FirebaseInstanceId.getInstance().getToken();
    }

    /**
     * Removes the stored token from Shared Preferences
     */
    private void clearStoredToken() {
        prefsUtil.removeValue(PrefsUtil.KEY_FIREBASE_TOKEN);
    }

    // TODO: 16/01/2017 Call me on logout?
    /**
     * Resets Instance ID and revokes all tokens. Clears stored token if successful
     */
    public Completable revokeAccessToken() {
        return Completable.fromCallable(() -> {
            FirebaseInstanceId.getInstance().deleteInstanceId();
            return Void.TYPE;
        }).doOnComplete(this::clearStoredToken)
                .compose(RxUtil.applySchedulersToCompletable());
    }

    private void sendFirebaseToken(String refreshedToken) {
        Payload payload = payloadManager.getPayload();
        String guid = payload.getGuid();
        String sharedKey = payload.getSharedKey();

        // TODO: 09/11/2016 Decide what to do if sending fails, perhaps retry?
        notificationService.sendNotificationToken(refreshedToken, guid, sharedKey)
                .subscribe(() -> Log.d(TAG, "sendFirebaseToken: success"), Throwable::printStackTrace);
    }

}
