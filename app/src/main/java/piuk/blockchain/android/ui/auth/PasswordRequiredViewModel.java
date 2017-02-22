package piuk.blockchain.android.ui.auth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.DialogButtonCallback;
import piuk.blockchain.android.util.PrefsUtil;

public class PasswordRequiredViewModel extends BaseViewModel {

    @Inject protected AppUtil mAppUtil;
    @Inject protected PrefsUtil mPrefsUtil;
    @Inject protected AuthDataManager mAuthDataManager;
    @Inject protected AccessState mAccessState;
    private DataListener mDataListener;
    @VisibleForTesting boolean mWaitingForAuth = false;

    public interface DataListener {

        String getPassword();

        void resetPasswordField();

        void goToPinPage();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void restartPage();

        void updateWaitingForAuthDialog(int secondsRemaining);

        void showProgressDialog(@StringRes int messageId, @Nullable String suffix, boolean cancellable);

        void dismissProgressDialog();

        void showForgetWalletWarning(DialogButtonCallback callback);

    }

    public PasswordRequiredViewModel(DataListener listener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        mDataListener = listener;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    public void onContinueClicked() {
        if (mDataListener.getPassword().length() > 1) {
            verifyPassword(mDataListener.getPassword());
        } else {
            mDataListener.showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR);
            mDataListener.restartPage();
        }
    }

    public void onForgetWalletClicked() {
        mDataListener.showForgetWalletWarning(new DialogButtonCallback() {
            @Override
            public void onPositiveClicked() {
                mAppUtil.clearCredentialsAndRestart();
            }

            @Override
            public void onNegativeClicked() {
                // No-op
            }
        });
    }

    private void verifyPassword(String password) {
        throw new NotImplementedException("Session id not done yet");
        // TODO: 22/02/2017
//        mDataListener.showProgressDialog(R.string.validating_password, null, false);
//
//        String guid = mPrefsUtil.getValue(PrefsUtil.KEY_GUID, "");
//        mWaitingForAuth = true;
//
//        compositeDisposable.add(
//                mAuthDataManager.getSessionId(guid)
//                        .flatMap(sessionId -> mAuthDataManager.getEncryptedPayload(guid, sessionId))
//                        .subscribe(response -> {
//                            if (response.equals(WalletPayload.KEY_AUTH_REQUIRED)) {
//                                showCheckEmailDialog();
//                                compositeDisposable.add(
//                                        mAuthDataManager.startPollingAuthStatus(guid).subscribe(payloadResponse -> {
//                                            mWaitingForAuth = false;
//
//                                            if (payloadResponse == null || payloadResponse.equals(WalletPayload.KEY_AUTH_REQUIRED)) {
//                                                showErrorToastAndRestartApp(R.string.auth_failed);
//                                                return;
//
//                                            }
//                                            attemptDecryptPayload(password, guid, payloadResponse);
//
//                                        }, throwable -> {
//                                            mWaitingForAuth = false;
//                                            showErrorToastAndRestartApp(R.string.auth_failed);
//                                        }));
//                            } else {
//                                mWaitingForAuth = false;
//                                attemptDecryptPayload(password, guid, response);
//                            }
//                        }, throwable -> {
//                            throwable.printStackTrace();
//                            showErrorToastAndRestartApp(R.string.auth_failed);
//                        }));
    }

    private void attemptDecryptPayload(String password, String guid, String payload) {
        mAuthDataManager.attemptDecryptPayload(password, guid, payload, new AuthDataManager.DecryptPayloadListener() {
            @Override
            public void onSuccess() {
                mDataListener.goToPinPage();
            }

            @Override
            public void onPairFail() {
                showErrorToast(R.string.pairing_failed);
            }

            @Override
            public void onAuthFail() {
                showErrorToast(R.string.auth_failed);
            }

            @Override
            public void onFatalError() {
                showErrorToastAndRestartApp(R.string.auth_failed);
            }
        });
    }

    private void showCheckEmailDialog() {
        mDataListener.showProgressDialog(R.string.check_email_to_auth_login, "120", true);

        compositeDisposable.add(mAuthDataManager.createCheckEmailTimer()
                .takeUntil(integer -> !mWaitingForAuth)
                .subscribe(integer -> {
                    if (integer <= 0) {
                        // Only called if timer has run out
                        showErrorToastAndRestartApp(R.string.pairing_failed);
                    } else {
                        mDataListener.updateWaitingForAuthDialog(integer);
                    }
                }, throwable -> {
                    showErrorToast(R.string.auth_failed);
                    mWaitingForAuth = false;
                }));
    }

    public void onProgressCancelled() {
        mWaitingForAuth = false;
        destroy();
    }

    @UiThread
    private void showErrorToast(@StringRes int message) {
        mDataListener.dismissProgressDialog();
        mDataListener.resetPasswordField();
        mDataListener.showToast(message, ToastCustom.TYPE_ERROR);
    }

    @UiThread
    private void showErrorToastAndRestartApp(@StringRes int message) {
        mDataListener.resetPasswordField();
        mDataListener.dismissProgressDialog();
        mDataListener.showToast(message, ToastCustom.TYPE_ERROR);
        mAppUtil.clearCredentialsAndRestart();
    }

    @NonNull
    public AppUtil getAppUtil() {
        return mAppUtil;
    }
}
