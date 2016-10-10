package piuk.blockchain.android.ui.settings;

import info.blockchain.wallet.util.CharSequenceX;

import javax.inject.Inject;

import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.util.PrefsUtil;

import static piuk.blockchain.android.ui.fingerprint.FingerprintHelper.KEY_PIN_CODE;

public class SettingsViewModel extends BaseViewModel {

    @Inject FingerprintHelper fingerprintHelper;
    @Inject PrefsUtil prefsUtil;
    private DataListener dataListener;

    interface DataListener {

        void verifyPinCode();

        void showFingerprintDialog(CharSequenceX pincode, FingerprintHelper fingerprintHelper);

        void showDisableFingerprintDialog();

        void updateFingerprintPreferenceStatus();

        void showNoFingerprintsAddedDialog();

    }

    SettingsViewModel(DataListener dataListener) {
        Injector.getInstance().getAppComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        // No-op
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
            fingerprintHelper.clearEncryptedData(KEY_PIN_CODE);
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
        dataListener.showFingerprintDialog(pinCode, fingerprintHelper);
    }
}
