package piuk.blockchain.android.ui.fingerprint;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mtramin.rxfingerprint.RxFingerprint;

import info.blockchain.wallet.util.CharSequenceX;

import piuk.blockchain.android.util.PrefsUtil;

public class FingerprintHelper {

    public static final String KEY_PIN_CODE = "pin_code";
    public static final String KEY_FINGERPRINT_ENABLED = "fingerprint_enabled";

    private Context applicationContext;
    private PrefsUtil prefsUtil;

    public FingerprintHelper(Context applicationContext, PrefsUtil prefsUtil) {
        this.applicationContext = applicationContext;
        this.prefsUtil = prefsUtil;
    }

    /**
     * Returns true only if there is appropriate hardware available && there are enrolled fingerprints
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

    public void setFingerprintUnlockEnabled(boolean enabled) {
        prefsUtil.setValue(KEY_FINGERPRINT_ENABLED, enabled);
    }

    public void storeEncryptedData(@NonNull String key, @NonNull CharSequenceX data) {
        prefsUtil.setValue(key, data.toString());
    }

    public void authenticateFingerprint(AuthCallback callback) {
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
                            callback.onAuthenticated(null);
                            break;
                    }
                }, throwable -> {
                    callback.onFailure();
                });
    }

    public void encryptString(String key, String stringToEncrypt, AuthCallback callback) {
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
                });
    }

    public void decryptString(String key, String encryptedString, AuthCallback callback) {
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
                        callback.onFailure();
                    }
                    Log.e("ERROR", "decrypt", throwable);
                });
    }

    public interface AuthCallback {

        void onFailure();

        void onHelp(String message);

        void onAuthenticated(@Nullable CharSequenceX data);

        void onKeyInvalidated();
    }
}
