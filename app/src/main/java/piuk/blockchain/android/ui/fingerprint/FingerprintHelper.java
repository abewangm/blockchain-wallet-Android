package piuk.blockchain.android.ui.fingerprint;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.mtramin.rxfingerprint.RxFingerprint;

import info.blockchain.wallet.util.CharSequenceX;

import java.io.UnsupportedEncodingException;

import piuk.blockchain.android.util.PrefsUtil;
import rx.subscriptions.CompositeSubscription;

public class FingerprintHelper {

    public static final String KEY_PIN_CODE = "pin_code";
    public static final String KEY_FINGERPRINT_ENABLED = "fingerprint_enabled";

    private Context applicationContext;
    private PrefsUtil prefsUtil;
    private CompositeSubscription compositeSubscription;

    public FingerprintHelper(Context applicationContext, PrefsUtil prefsUtil) {
        this.applicationContext = applicationContext;
        this.prefsUtil = prefsUtil;
        compositeSubscription = new CompositeSubscription();
    }

    /**
     * Returns true only if there is appropriate hardware available && there are enrolled
     * fingerprints
     */
    public boolean isFingerprintAvailable() {
        return RxFingerprint.isAvailable(applicationContext);
    }

    /**
     * Returns true if the device has the appropriate hardware for fingerprint authentication
     */
    public boolean isHardwareDetected() {
        return RxFingerprint.isHardwareDetected(applicationContext);
    }

    /**
     * Returns if any fingerprints are registered
     */
    public boolean areFingerprintsEnrolled() {
        return RxFingerprint.hasEnrolledFingerprints(applicationContext);
    }

    /**
     * Returns true if the user has previously enabled fingerprint login
     */
    public boolean getIfFingerprintUnlockEnabled() {
        return isFingerprintAvailable() && prefsUtil.getValue(KEY_FINGERPRINT_ENABLED, false);
    }

    /**
     * Store whether or not fingerprint login has been successfully set up
     */
    public void setFingerprintUnlockEnabled(boolean enabled) {
        prefsUtil.setValue(KEY_FINGERPRINT_ENABLED, enabled);
    }

    /**
     * Allows you to store the encrypted result of fingerprint authentication. The data is converted
     * into a Base64 string and written to shared prefs with a key. Please note that this doesn't
     * encrypt the data in any way, just obfuscates it.
     *
     * @param key   The key to write/retrieve the data to/from
     * @param data  The data to be stored, in the form of a {@link CharSequenceX}
     * @return      Returns true if data stored successfully
     */
    public boolean storeEncryptedData(@NonNull String key, @NonNull CharSequenceX data) {
        try {
            String base64 = Base64.encodeToString(data.toString().getBytes("UTF-8"), Base64.DEFAULT);
            prefsUtil.setValue(key, base64);
            return true;
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }

    /**
     * Retrieve previously saved encrypted data from shared preferences
     * @param key   The key of the item to be retrieved
     * @return      A {@link CharSequenceX} wrapping the saved String, or null if not found
     */
    @Nullable
    public CharSequenceX getEncryptedData(@NonNull String key) {
        String encryptedData = prefsUtil.getValue(key, "");
        if (!encryptedData.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(encryptedData.getBytes("UTF-8"), Base64.DEFAULT);
                return new CharSequenceX(new String(bytes));
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Deletes the data stored under the passed in key
     * @param key   The key of the data to be stored
     */
    public void clearEncryptedData(@NonNull String key) {
        prefsUtil.removeValue(key);
    }

    public void authenticateFingerprint(AuthCallback callback) {
        compositeSubscription.add(
                RxFingerprint.authenticate(applicationContext)
                        .subscribe(fingerprintAuthenticationResult -> {
                            switch (fingerprintAuthenticationResult.getResult()) {
                                case FAILED:
                                    callback.onFailure();
                                    break;
                                case HELP:
                                    callback.onHelp(fingerprintAuthenticationResult.getMessage());
                                    break;
                                case AUTHENTICATED:
                                    callback.onAuthenticated(new CharSequenceX(fingerprintAuthenticationResult.getMessage()));
                                    break;
                            }
                        }, throwable -> {
                            callback.onFailure();
                        }));
    }

    public void encryptString(String key, String stringToEncrypt, AuthCallback callback) {
        compositeSubscription.add(
                RxFingerprint.encrypt(applicationContext, key, stringToEncrypt)
                        .subscribe(encryptionResult -> {
                            switch (encryptionResult.getResult()) {
                                case FAILED:
                                    callback.onFailure();
                                    break;
                                case HELP:
                                    callback.onHelp(encryptionResult.getMessage());
                                    break;
                                case AUTHENTICATED:
                                    String encrypted = encryptionResult.getEncrypted();
                                    callback.onAuthenticated(new CharSequenceX(encrypted));
                                    break;
                            }
                        }, throwable -> {
                            callback.onFailure();
                            Log.e("ERROR", "encrypt", throwable);
                        }));
    }

    public void decryptString(String key, String encryptedString, AuthCallback callback) {
        compositeSubscription.add(
                RxFingerprint.decrypt(applicationContext, key, encryptedString)
                        .subscribe(decryptionResult -> {
                            switch (decryptionResult.getResult()) {
                                case FAILED:
                                    callback.onFailure();
                                    break;
                                case HELP:
                                    callback.onHelp(decryptionResult.getMessage());
                                    break;
                                case AUTHENTICATED:
                                    String decrypted = decryptionResult.getDecrypted();
                                    callback.onAuthenticated(new CharSequenceX(decrypted));
                                    break;
                            }
                        }, throwable -> {
                            if (RxFingerprint.keyInvalidated(throwable)) {
                                // The keys you wanted to use are invalidated because the user has turned off his
                                // secure lock screen or changed the fingerprints stored on the device
                                // You have to re-encrypt the data to access it
                                callback.onKeyInvalidated();
                            } else {
                                callback.onFatalError();
                            }
                            Log.e("ERROR", "decrypt", throwable);
                        }));
    }

    /**
     * This should be called when authentication completed or no longer required, otherwise
     * the fingerprint sensor will keep listening in the background for touch events and leak
     * memory.
     */
    public void releaseFingerprintReader() {
        compositeSubscription.clear();
    }

    public interface AuthCallback {

        void onFailure();

        void onHelp(String message);

        void onAuthenticated(@NonNull CharSequenceX data);

        void onKeyInvalidated();

        void onFatalError();
    }
}
