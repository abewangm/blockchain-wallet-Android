package piuk.blockchain.android.ui.onboarding;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;

import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.settings.SettingsDataManager;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.util.PrefsUtil;

import static piuk.blockchain.android.ui.onboarding.OnboardingActivity.EXTRAS_EMAIL_ONLY;


public class OnboardingPresenter extends BasePresenter<OnboardingView> {

    private boolean showEmailOnly;
    @VisibleForTesting String email;
    private FingerprintHelper fingerprintHelper;
    private AccessState accessState;
    private SettingsDataManager settingsDataManager;

    @Inject
    OnboardingPresenter(FingerprintHelper fingerprintHelper,
                        AccessState accessState,
                        SettingsDataManager settingsDataManager) {

        this.fingerprintHelper = fingerprintHelper;
        this.accessState = accessState;
        this.settingsDataManager = settingsDataManager;
    }

    @Override
    public void onViewReady() {
        Intent intent = getView().getPageIntent();
        if (intent != null && intent.hasExtra(EXTRAS_EMAIL_ONLY)) {
            showEmailOnly = intent.getBooleanExtra(EXTRAS_EMAIL_ONLY, false);
        }

        getCompositeDisposable().add(
                settingsDataManager.getSettings()
                        .doAfterTerminate(this::checkAppState)
                        .subscribe(
                                settings -> email = settings.getEmail(),
                                Throwable::printStackTrace));
    }

    /**
     * Checks status of fingerprint hardware and either prompts the user to verify their fingerprint
     * or enroll one if the fingerprint sensor has never been set up.
     */
    void onEnableFingerprintClicked() {
        if (fingerprintHelper.isFingerprintAvailable()) {
            if (accessState.getPIN() != null && !accessState.getPIN().isEmpty()) {
                getView().showFingerprintDialog(accessState.getPIN());
            } else {
                throw new IllegalStateException("PIN not found");
            }
        } else if (fingerprintHelper.isHardwareDetected()) {
            // Hardware available but user has never set up fingerprints
            getView().showEnrollFingerprintsDialog();
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
            getView().showEmailPrompt();
        } else if (fingerprintHelper.isHardwareDetected()) {
            getView().showFingerprintPrompt();
        } else {
            getView().showEmailPrompt();
        }
    }
}
