package piuk.blockchain.android.ui.onboarding;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;

import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.datamanagers.SettingsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.util.PrefsUtil;

import static piuk.blockchain.android.ui.onboarding.OnboardingActivity.EXTRAS_EMAIL_ONLY;


@SuppressWarnings("WeakerAccess")
public class OnboardingViewModel extends BaseViewModel {

    private DataListener dataListener;
    private boolean showEmailOnly;
    @VisibleForTesting String email;
    @Inject protected FingerprintHelper fingerprintHelper;
    @Inject protected AccessState accessState;
    @Inject protected SettingsDataManager settingsDataManager;
    @Inject protected PayloadDataManager payloadDataManager;

    interface DataListener {

        Intent getPageIntent();

        void showFingerprintPrompt();

        void showEmailPrompt();

        void showFingerprintDialog(String pincode);

        void showEnrollFingerprintsDialog();

    }

    OnboardingViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        Intent intent = dataListener.getPageIntent();
        if (intent != null && intent.hasExtra(EXTRAS_EMAIL_ONLY)) {
            showEmailOnly = true;
        }

        compositeDisposable.add(
                settingsDataManager.initSettings(
                        payloadDataManager.getWallet().getGuid(),
                        payloadDataManager.getWallet().getSharedKey())
                        .subscribe(
                                settings -> {
                                    email = settings.getEmail();
                                    checkAppState();
                                },
                                throwable -> checkAppState()));
    }

    /**
     * Checks status of fingerprint hardware and either prompts the user to verify their fingerprint
     * or enroll one if the fingerprint sensor has never been set up.
     */
    void onEnableFingerprintClicked() {
        if (fingerprintHelper.isFingerprintAvailable()) {
            if (accessState.getPIN() != null && !accessState.getPIN().isEmpty()) {
                dataListener.showFingerprintDialog(accessState.getPIN());
            } else {
                throw new IllegalStateException("PIN not found");
            }
        } else if (fingerprintHelper.isHardwareDetected()) {
            // Hardware available but user has never set up fingerprints
            dataListener.showEnrollFingerprintsDialog();
        } else {
            throw new IllegalStateException("Fingerprint hardware not available, yet functions requiring hardware called.");
        }
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

    @Nullable
    String getEmail() {
        return email;
    }

    private void checkAppState() {
        if (showEmailOnly) {
            dataListener.showEmailPrompt();
        } else if (fingerprintHelper.isHardwareDetected()) {
            dataListener.showFingerprintPrompt();
        } else {
            dataListener.showEmailPrompt();
        }
    }
}
