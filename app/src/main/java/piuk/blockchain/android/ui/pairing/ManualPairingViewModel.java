package piuk.blockchain.android.ui.pairing;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.annotations.Thunk;

import static android.support.annotation.VisibleForTesting.PRIVATE;

@SuppressWarnings("WeakerAccess")
public class ManualPairingViewModel extends BaseViewModel {

    private static final String KEY_AUTH_REQUIRED = "Authorization Required";
    private static final String TAG = ManualPairingViewModel.class.getSimpleName();

    @Inject protected AppUtil appUtil;
    @Inject protected AuthDataManager authDataManager;
    private DataListener dataListener;
    @VisibleForTesting(otherwise = PRIVATE) boolean waitingForAuth = false;

    public interface DataListener {

        String getGuid();

        String getPassword();

        void goToPinPage();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void updateWaitingForAuthDialog(int secondsRemaining);

        void showProgressDialog(@StringRes int messageId, @Nullable String suffix, boolean cancellable);

        void dismissProgressDialog();

        void resetPasswordField();

    }

    ManualPairingViewModel(DataListener listener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        dataListener = listener;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    void onContinueClicked() {

        String guid = dataListener.getGuid();
        String password = dataListener.getPassword();

        if (guid == null || guid.isEmpty()) {
            showErrorToast(R.string.invalid_guid);
        } else if (password == null || password.isEmpty()) {
            showErrorToast(R.string.invalid_password);
        } else {
            verifyPassword(password, guid);
        }
    }

    private void verifyPassword(String password, String guid) {
        dataListener.showProgressDialog(R.string.validating_password, null, false);

        waitingForAuth = true;
        final String[] finalSessionId = new String[1];

        compositeDisposable.add(
                authDataManager.getSessionId(guid)
                        .doOnNext(s -> finalSessionId[0] = s)
                        .flatMap(sessionId -> authDataManager.getEncryptedPayload(guid, sessionId))
                        .subscribe(response -> {
                            if (response.errorBody() != null
                                    && response.errorBody().string().contains(KEY_AUTH_REQUIRED)) {

                                showCheckEmailDialog();

                                compositeDisposable.add(
                                        authDataManager.startPollingAuthStatus(guid, finalSessionId[0])
                                                .subscribe(payloadResponse -> {
                                                    waitingForAuth = false;

                                                    if (payloadResponse == null || payloadResponse.contains(KEY_AUTH_REQUIRED)) {
                                                        showErrorToastAndRestartApp(R.string.auth_failed);
                                                        return;

                                                    }
                                                    attemptDecryptPayload(password, payloadResponse);

                                                }, throwable -> {
                                                    Log.e(TAG, "verifyPassword: ", throwable);
                                                    waitingForAuth = false;
                                                    showErrorToastAndRestartApp(R.string.auth_failed);
                                                }));
                            } else {
                                waitingForAuth = false;
                                attemptDecryptPayload(password, response.message());
                            }
                        }, throwable -> {
                            Log.e(TAG, "verifyPassword: ", throwable);
                            showErrorToastAndRestartApp(R.string.auth_failed);
                        }));
    }

    private void attemptDecryptPayload(String password, String payload) {
        compositeDisposable.add(
                authDataManager.initializeFromPayload(payload, password)
                        .subscribe(() -> dataListener.goToPinPage(),
                                throwable -> {
                                    if (throwable instanceof HDWalletException) {
                                        showErrorToast(R.string.pairing_failed);
                                    } else if (throwable instanceof DecryptionException) {
                                        showErrorToast(R.string.auth_failed);
                                    } else {
                                        showErrorToastAndRestartApp(R.string.auth_failed);
                                    }
                                }));
    }

    private void showCheckEmailDialog() {
        dataListener.showProgressDialog(R.string.check_email_to_auth_login, "120", true);

        compositeDisposable.add(authDataManager.createCheckEmailTimer()
                .takeUntil(integer -> !waitingForAuth)
                .subscribe(integer -> {
                    if (integer <= 0) {
                        // Only called if timer has run out
                        showErrorToastAndRestartApp(R.string.pairing_failed);
                    } else {
                        dataListener.updateWaitingForAuthDialog(integer);
                    }
                }, throwable -> {
                    showErrorToast(R.string.auth_failed);
                    waitingForAuth = false;
                }));
    }

    void onProgressCancelled() {
        waitingForAuth = false;
        destroy();
    }

    @Thunk
    private void showErrorToast(@StringRes int message) {
        dataListener.dismissProgressDialog();
        dataListener.resetPasswordField();
        dataListener.showToast(message, ToastCustom.TYPE_ERROR);
    }

    private void showErrorToastAndRestartApp(@StringRes int message) {
        dataListener.resetPasswordField();
        dataListener.dismissProgressDialog();
        dataListener.showToast(message, ToastCustom.TYPE_ERROR);
        appUtil.clearCredentialsAndRestart();
    }

    @NonNull
    AppUtil getAppUtil() {
        return appUtil;
    }
}
