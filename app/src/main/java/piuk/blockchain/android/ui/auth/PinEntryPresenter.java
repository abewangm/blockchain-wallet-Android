package piuk.blockchain.android.ui.auth;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.widget.ImageView;

import com.crashlytics.android.answers.LoginEvent;

import info.blockchain.wallet.api.Environment;
import info.blockchain.wallet.exceptions.AccountLockedException;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.exceptions.PayloadException;
import info.blockchain.wallet.exceptions.ServerConnectionException;
import info.blockchain.wallet.exceptions.UnsupportedVersionException;

import org.spongycastle.crypto.InvalidCipherTextException;

import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.answers.Logging;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.auth.AuthDataManager;
import piuk.blockchain.android.data.currency.CryptoCurrencies;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.walletoptions.WalletOptionsDataManager;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.ui.home.SecurityPromptDialog;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.DialogButtonCallback;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;
import piuk.blockchain.android.util.annotations.Thunk;
import timber.log.Timber;

import static piuk.blockchain.android.ui.auth.PinEntryFragment.KEY_VALIDATING_PIN_FOR_RESULT;

public class PinEntryPresenter extends BasePresenter<PinEntryView> {

    private static final int PIN_LENGTH = 4;
    private static final int MAX_ATTEMPTS = 4;

    private AuthDataManager mAuthDataManager;
    private AppUtil mAppUtil;
    private PrefsUtil mPrefsUtil;
    private PayloadDataManager mPayloadDataManager;
    private StringUtils mStringUtils;
    private FingerprintHelper mFingerprintHelper;
    private AccessState mAccessState;
    private WalletOptionsDataManager walletOptionsDataManager;
    private EnvironmentSettings environmentSettings;

    @VisibleForTesting boolean mCanShowFingerprintDialog = true;
    @VisibleForTesting boolean mValidatingPinForResult = false;
    @VisibleForTesting String mUserEnteredPin = "";
    @VisibleForTesting String mUserEnteredConfirmationPin;
    @VisibleForTesting boolean bAllowExit = true;

    @Inject
    PinEntryPresenter(AuthDataManager mAuthDataManager,
                      AppUtil mAppUtil,
                      PrefsUtil mPrefsUtil,
                      PayloadDataManager mPayloadDataManager,
                      StringUtils mStringUtils,
                      FingerprintHelper mFingerprintHelper,
                      AccessState mAccessState,
                      WalletOptionsDataManager walletOptionsDataManager,
                      EnvironmentSettings environmentSettings) {

        this.mAuthDataManager = mAuthDataManager;
        this.mAppUtil = mAppUtil;
        this.mPrefsUtil = mPrefsUtil;
        this.mPayloadDataManager = mPayloadDataManager;
        this.mStringUtils = mStringUtils;
        this.mFingerprintHelper = mFingerprintHelper;
        this.mAccessState = mAccessState;
        this.walletOptionsDataManager = walletOptionsDataManager;
        this.environmentSettings = environmentSettings;
    }

    @Override
    public void onViewReady() {
        mAppUtil.applyPRNGFixes();

        if (getView().getPageIntent() != null) {
            Bundle extras = getView().getPageIntent().getExtras();
            if (extras != null) {

                if (extras.containsKey(KEY_VALIDATING_PIN_FOR_RESULT)) {
                    mValidatingPinForResult = extras.getBoolean(KEY_VALIDATING_PIN_FOR_RESULT);
                }
            }
        }

        checkPinFails();
        checkFingerprintStatus();
        doTestnetCheck();
    }

    private void doTestnetCheck() {
        if (environmentSettings.getEnvironment().equals(Environment.TESTNET)) {
            getView().showTestnetWarning();
        }
    }

    void checkFingerprintStatus() {
        if (getIfShouldShowFingerprintLogin()) {
            getView().showFingerprintDialog(
                    mFingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE));
        } else {
            getView().showKeyboard();
        }
    }

    boolean canShowFingerprintDialog() {
        return mCanShowFingerprintDialog;
    }

    boolean getIfShouldShowFingerprintLogin() {
        return !(mValidatingPinForResult || isCreatingNewPin())
                && mFingerprintHelper.isFingerprintUnlockEnabled()
                && mFingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE) != null;
    }

    void loginWithDecryptedPin(String pincode) {
        mCanShowFingerprintDialog = false;
        for (ImageView view : getView().getPinBoxArray()) {
            view.setImageResource(R.drawable.rounded_view_dark_blue);
        }
        validatePIN(pincode);
    }

    void onDeleteClicked() {
        if (!mUserEnteredPin.isEmpty()) {
            // Remove last char from pin string
            mUserEnteredPin = mUserEnteredPin.substring(0, mUserEnteredPin.length() - 1);

            // Clear last box
            getView().getPinBoxArray()[mUserEnteredPin.length()].setImageResource(R.drawable.rounded_view_blue_white_border);
        }
    }

    void onPadClicked(String string) {
        if (mUserEnteredPin.length() == PIN_LENGTH) {
            return;
        }

        // Append tapped #
        mUserEnteredPin = mUserEnteredPin + string;

        for (int i = 0; i < mUserEnteredPin.length(); i++) {
            // Ensures that all necessary dots are filled
            getView().getPinBoxArray()[i].setImageResource(R.drawable.rounded_view_dark_blue);
        }

        // Perform appropriate action if PIN_LENGTH has been reached
        if (mUserEnteredPin.length() == PIN_LENGTH) {

            // Throw error on '0000' to avoid server-side type issue
            if (mUserEnteredPin.equals("0000")) {
                showErrorToast(R.string.zero_pin);
                clearPinViewAndReset();
                if (isCreatingNewPin()) {
                    getView().setTitleString(R.string.create_pin);
                }
                return;
            }

            // Only show warning on first entry and if user is creating a new PIN
            if (isCreatingNewPin() && isPinCommon(mUserEnteredPin) && mUserEnteredConfirmationPin == null) {
                getView().showCommonPinWarning(new DialogButtonCallback() {
                    @Override
                    public void onPositiveClicked() {
                        clearPinViewAndReset();
                    }

                    @Override
                    public void onNegativeClicked() {
                        validateAndConfirmPin();
                    }
                });

                // If user is changing their PIN and it matches their old one, disallow it
            } else if (isChangingPin()
                    && mUserEnteredConfirmationPin == null
                    && mAccessState.getPIN().equals(mUserEnteredPin)) {
                showErrorToast(R.string.change_pin_new_matches_current);
                clearPinViewAndReset();
            } else {
                validateAndConfirmPin();
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Thunk
    void validateAndConfirmPin() {
        // Validate
        if (!mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").isEmpty()) {
            getView().setTitleVisibility(View.INVISIBLE);
            validatePIN(mUserEnteredPin);
        } else if (mUserEnteredConfirmationPin == null) {
            // End of Create -  Change to Confirm
            mUserEnteredConfirmationPin = mUserEnteredPin;
            mUserEnteredPin = "";
            getView().setTitleString(R.string.confirm_pin);
            clearPinBoxes();
        } else if (mUserEnteredConfirmationPin.equals(mUserEnteredPin)) {
            // End of Confirm - Pin is confirmed
            createNewPin(mUserEnteredPin);
        } else {
            // End of Confirm - Pin Mismatch
            showErrorToast(R.string.pin_mismatch_error);
            getView().setTitleString(R.string.create_pin);
            clearPinViewAndReset();
        }
    }

    /**
     * Resets the view without restarting the page
     */
    @SuppressWarnings("WeakerAccess")
    @Thunk
    void clearPinViewAndReset() {
        clearPinBoxes();
        mUserEnteredConfirmationPin = null;
        checkFingerprintStatus();
    }

    void clearPinBoxes() {
        mUserEnteredPin = "";
        if (getView() != null) getView().clearPinBoxes();
    }

    @VisibleForTesting
    void updatePayload(String password) {
        getView().showProgressDialog(R.string.decrypting_wallet, null);

        getCompositeDisposable().add(
                mPayloadDataManager.initializeAndDecrypt(
                        mPrefsUtil.getValue(PrefsUtil.KEY_SHARED_KEY, ""),
                        mPrefsUtil.getValue(PrefsUtil.KEY_GUID, ""),
                        password)
                        .doAfterTerminate(() -> {
                            getView().dismissProgressDialog();
                            mCanShowFingerprintDialog = true;
                        })
                        .subscribe(() -> {
                            mAppUtil.setSharedKey(mPayloadDataManager.getWallet().getSharedKey());

                            setAccountLabelIfNecessary();

                            Logging.INSTANCE.logLogin(new LoginEvent().putSuccess(true));

                            if (!mPayloadDataManager.getWallet().isUpgraded()) {
                                getView().goToUpgradeWalletActivity();
                            } else {
                                mAppUtil.restartAppWithVerifiedPin();
                            }

                        }, throwable -> {
                            Logging.INSTANCE.logLogin(new LoginEvent().putSuccess(false));

                            if (throwable instanceof InvalidCredentialsException) {
                                getView().goToPasswordRequiredActivity();

                            } else if (throwable instanceof ServerConnectionException
                                    || throwable instanceof SocketTimeoutException) {
                                getView().showToast(R.string.check_connectivity_exit, ToastCustom.TYPE_ERROR);
                                mAppUtil.restartApp();

                            } else if (throwable instanceof UnsupportedVersionException) {
                                getView().showWalletVersionNotSupportedDialog(throwable.getMessage());

                            } else if (throwable instanceof DecryptionException) {
                                getView().goToPasswordRequiredActivity();

                            } else if (throwable instanceof PayloadException) {
                                //This shouldn't happen - Payload retrieved from server couldn't be parsed
                                getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                                mAppUtil.restartApp();

                            } else if (throwable instanceof HDWalletException) {
                                //This shouldn't happen. HD fatal error - not safe to continue - don't clear credentials
                                getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                                mAppUtil.restartApp();

                            } else if (throwable instanceof InvalidCipherTextException) {
                                // Password changed on web, needs re-pairing
                                getView().showToast(R.string.password_changed_explanation, ToastCustom.TYPE_ERROR);
                                mAccessState.setPIN(null);
                                mAppUtil.clearCredentialsAndRestart();

                            } else if (throwable instanceof AccountLockedException) {
                                getView().showAccountLockedDialog();

                            } else {
                                Logging.INSTANCE.logException(throwable);
                                getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                                mAppUtil.restartApp();
                            }

                        }));
    }

    boolean isForValidatingPinForResult() {
        return mValidatingPinForResult;
    }

    void validatePassword(String password) {
        getView().showProgressDialog(R.string.validating_password, null);

        getCompositeDisposable().add(
                mPayloadDataManager.initializeAndDecrypt(
                        mPrefsUtil.getValue(PrefsUtil.KEY_SHARED_KEY, ""),
                        mPrefsUtil.getValue(PrefsUtil.KEY_GUID, ""),
                        password)
                        .doAfterTerminate(() -> getView().dismissProgressDialog())
                        .subscribe(() -> {
                            getView().showToast(R.string.pin_4_strikes_password_accepted, ToastCustom.TYPE_OK);
                            mPrefsUtil.removeValue(PrefsUtil.KEY_PIN_FAILS);
                            mPrefsUtil.removeValue(PrefsUtil.KEY_PIN_IDENTIFIER);
                            mAccessState.setPIN(null);
                            getView().restartPageAndClearTop();
                        }, throwable -> {

                            if (throwable instanceof ServerConnectionException
                                    || throwable instanceof SocketTimeoutException) {
                                getView().showToast(R.string.check_connectivity_exit, ToastCustom.TYPE_ERROR);
                            } else if (throwable instanceof PayloadException) {
                                //This shouldn't happen - Payload retrieved from server couldn't be parsed
                                getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                                mAppUtil.restartApp();

                            } else if (throwable instanceof HDWalletException) {
                                //This shouldn't happen. HD fatal error - not safe to continue - don't clear credentials
                                getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                                mAppUtil.restartApp();

                            } else if (throwable instanceof AccountLockedException) {
                                getView().showAccountLockedDialog();

                            } else {
                                Logging.INSTANCE.logException(throwable);
                                showErrorToast(R.string.invalid_password);
                                getView().showValidationDialog();
                            }

                        }));
    }

    private void createNewPin(String pin) {
        getView().showProgressDialog(R.string.creating_pin, null);

        getCompositeDisposable().add(
                mAuthDataManager.createPin(mPayloadDataManager.getTempPassword(), pin)
                        .subscribe(() -> {
                            getView().dismissProgressDialog();
                            mFingerprintHelper.clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE);
                            mFingerprintHelper.setFingerprintUnlockEnabled(false);
                            mPrefsUtil.setValue(PrefsUtil.KEY_PIN_FAILS, 0);
                            updatePayload(mPayloadDataManager.getTempPassword());
                        }, throwable -> {
                            showErrorToast(R.string.create_pin_failed);
                            mPrefsUtil.clear();
                            mAppUtil.restartApp();
                        }));
    }

    private void validatePIN(String pin) {
        getView().showProgressDialog(R.string.validating_pin, null);

        mAuthDataManager.validatePin(pin)
                .subscribe(password -> {
                    getView().dismissProgressDialog();
                    if (password != null) {
                        if (mValidatingPinForResult) {
                            getView().finishWithResultOk(pin);
                        } else {
                            updatePayload(password);
                        }
                        mPrefsUtil.setValue(PrefsUtil.KEY_PIN_FAILS, 0);
                    } else {
                        handleValidateFailure();
                    }
                }, throwable -> {
                    Timber.e(throwable);
                    if (throwable instanceof InvalidCredentialsException) {
                        handleValidateFailure();
                    } else {
                        showErrorToast(R.string.api_fail);
                        getView().restartPageAndClearTop();
                    }
                });
    }

    private void handleValidateFailure() {
        if (mValidatingPinForResult) {
            incrementFailureCount();
        } else {
            incrementFailureCountAndRestart();
        }
    }

    private void incrementFailureCount() {
        int fails = mPrefsUtil.getValue(PrefsUtil.KEY_PIN_FAILS, 0);
        mPrefsUtil.setValue(PrefsUtil.KEY_PIN_FAILS, ++fails);
        showErrorToast(R.string.invalid_pin);
        mUserEnteredPin = "";
        for (ImageView textView : getView().getPinBoxArray()) {
            textView.setImageResource(R.drawable.rounded_view_blue_white_border);
        }
        getView().setTitleVisibility(View.VISIBLE);
        getView().setTitleString(R.string.pin_entry);
    }

    void incrementFailureCountAndRestart() {
        int fails = mPrefsUtil.getValue(PrefsUtil.KEY_PIN_FAILS, 0);
        mPrefsUtil.setValue(PrefsUtil.KEY_PIN_FAILS, ++fails);
        showErrorToast(R.string.invalid_pin);
        getView().restartPageAndClearTop();
    }

    // Check user's password if PIN fails >= 4
    private void checkPinFails() {
        int fails = mPrefsUtil.getValue(PrefsUtil.KEY_PIN_FAILS, 0);
        if (fails >= MAX_ATTEMPTS) {
            showErrorToast(R.string.pin_4_strikes);
            getView().showMaxAttemptsDialog();
        }
    }

    private void setAccountLabelIfNecessary() {
        if (mAppUtil.isNewlyCreated()
                && !mPayloadDataManager.getAccounts().isEmpty()
                && mPayloadDataManager.getAccount(0) != null
                && (mPayloadDataManager.getAccount(0).getLabel() == null
                || mPayloadDataManager.getAccount(0).getLabel().isEmpty())) {

            mPayloadDataManager.getAccount(0).setLabel(mStringUtils.getString(R.string.default_wallet_name));
        }
    }

    private boolean isPinCommon(String pin) {
        List<String> commonPins = Arrays.asList("1234", "1111", "1212", "7777", "1004");
        return commonPins.contains(pin);
    }

    void resetApp() {
        mAppUtil.clearCredentialsAndRestart();
    }

    boolean allowExit() {
        return bAllowExit;
    }

    boolean isCreatingNewPin() {
        return mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").isEmpty();
    }

    private boolean isChangingPin() {
        return isCreatingNewPin()
                && mAccessState.getPIN() != null
                && !mAccessState.getPIN().isEmpty();
    }

    @UiThread
    private void showErrorToast(@StringRes int message) {
        getView().dismissProgressDialog();
        getView().showToast(message, ToastCustom.TYPE_ERROR);
    }

    @NonNull
    AppUtil getAppUtil() {
        return mAppUtil;
    }

    void logout(Context context) {
        mAccessState.logout(context);
    }

    void fetchInfoMessage() {
        walletOptionsDataManager.fetchInfoMessage()
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(message -> {
                    if (!message.isEmpty()) getView().showCustomPrompt(getWarningPrompt(message));
                }, Timber::e);
    }

    void checkForceUpgradeStatus(int versionCode, int sdkVersion) {
        walletOptionsDataManager.checkForceUpgrade(versionCode, sdkVersion)
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(
                        forceUpgrade -> {
                            if (forceUpgrade) getView().forceUpgrade();
                        }, Timber::e);
    }

    private SecurityPromptDialog getWarningPrompt(String message) {
        SecurityPromptDialog prompt = SecurityPromptDialog.newInstance(
                R.string.information,
                message,
                R.drawable.vector_help,
                R.string.ok_cap,
                false,
                false);
        prompt.setPositiveButtonListener(view -> prompt.dismiss());
        return prompt;
    }
}
