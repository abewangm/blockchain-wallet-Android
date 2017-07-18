package piuk.blockchain.android.ui.auth;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import javax.inject.Inject;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.auth.AuthDataManager;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.DialogButtonCallback;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.annotations.Thunk;
import retrofit2.Response;
import timber.log.Timber;

public class PasswordRequiredPresenter extends BasePresenter<PasswordRequiredView> {

    @VisibleForTesting static final String KEY_AUTH_REQUIRED = "authorization_required";

    @SuppressWarnings("WeakerAccess")
    @Thunk AppUtil appUtil;
    private PrefsUtil prefsUtil;
    private AuthDataManager authDataManager;
    private PayloadDataManager payloadDataManager;
    private String sessionId;
    @VisibleForTesting boolean waitingForAuth = false;

    @Inject
    PasswordRequiredPresenter(AppUtil appUtil,
                              PrefsUtil prefsUtil,
                              AuthDataManager authDataManager,
                              PayloadDataManager payloadDataManager) {

        this.appUtil = appUtil;
        this.prefsUtil = prefsUtil;
        this.authDataManager = authDataManager;
        this.payloadDataManager = payloadDataManager;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    void onContinueClicked() {
        if (getView().getPassword().length() > 1) {
            verifyPassword(getView().getPassword());
        } else {
            getView().showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR);
            getView().restartPage();
        }
    }

    void onForgetWalletClicked() {
        getView().showForgetWalletWarning(new DialogButtonCallback() {
            @Override
            public void onPositiveClicked() {
                appUtil.clearCredentialsAndRestart();
            }

            @Override
            public void onNegativeClicked() {
                // No-op
            }
        });
    }

    void submitTwoFactorCode(JSONObject responseObject, String sessionId, String password, String code) {
        if (code == null || code.isEmpty()) {
            getView().showToast(R.string.two_factor_null_error, ToastCustom.TYPE_ERROR);
        } else {
            String guid = prefsUtil.getValue(PrefsUtil.KEY_GUID, "");
            getCompositeDisposable().add(
                    authDataManager.submitTwoFactorCode(sessionId, guid, code)
                            .doOnSubscribe(disposable -> getView().showProgressDialog(R.string.please_wait, null, false))
                            .doAfterTerminate(() -> getView().dismissProgressDialog())
                            .subscribe(response -> {
                                        // This is slightly hacky, but if the user requires 2FA login,
                                        // the payload comes in two parts. Here we combine them and
                                        // parse/decrypt normally.
                                        responseObject.put("payload", response.string());
                                        ResponseBody responseBody = ResponseBody.create(
                                                MediaType.parse("application/json"),
                                                responseObject.toString());

                                        Response<ResponseBody> payload = Response.success(responseBody);
                                        handleResponse(password, guid, payload);
                                    },
                                    throwable -> showErrorToast(R.string.two_factor_incorrect_error)));
        }
    }

    private void verifyPassword(String password) {
        String guid = prefsUtil.getValue(PrefsUtil.KEY_GUID, "");
        waitingForAuth = true;

        getCompositeDisposable().add(
                authDataManager.getSessionId(guid)
                        .doOnSubscribe(disposable -> getView().showProgressDialog(R.string.validating_password, null, false))
                        .doOnNext(s -> sessionId = s)
                        .flatMap(sessionId -> authDataManager.getEncryptedPayload(guid, sessionId))
                        .subscribe(response -> handleResponse(password, guid, response),
                                throwable -> {
                                    Timber.e(throwable);
                                    showErrorToastAndRestartApp(R.string.auth_failed);
                                }));
    }

    private void handleResponse(String password, String guid, Response<ResponseBody> response) throws IOException, JSONException {
        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
        if (errorBody.contains(KEY_AUTH_REQUIRED)) {
            showCheckEmailDialog();

            getCompositeDisposable().add(
                    authDataManager.startPollingAuthStatus(guid, sessionId)
                            .subscribe(payloadResponse -> {
                                waitingForAuth = false;

                                if (payloadResponse == null || payloadResponse.contains(KEY_AUTH_REQUIRED)) {
                                    showErrorToastAndRestartApp(R.string.auth_failed);
                                    return;
                                }

                                ResponseBody responseBody = ResponseBody.create(
                                        MediaType.parse("application/json"),
                                        payloadResponse);
                                checkTwoFactor(password, Response.success(responseBody));
                            }, throwable -> {
                                Timber.e("handleResponse: ", throwable);
                                waitingForAuth = false;
                                showErrorToastAndRestartApp(R.string.auth_failed);
                            }));
        } else {
            waitingForAuth = false;
            checkTwoFactor(password, response);
        }
    }

    private void checkTwoFactor(String password, Response<ResponseBody> response) throws
            IOException, JSONException {

        String responseBody = response.body().string();
        JSONObject jsonObject = new JSONObject(responseBody);
        // Check if the response has a 2FA Auth Type but is also missing the payload,
        // as it comes in two parts if 2FA enabled.
        if (jsonObject.has("auth_type") && !jsonObject.has("payload")
                && (jsonObject.getInt("auth_type") == Settings.AUTH_TYPE_GOOGLE_AUTHENTICATOR
                || jsonObject.getInt("auth_type") == Settings.AUTH_TYPE_SMS)) {

            getView().dismissProgressDialog();
            getView().showTwoFactorCodeNeededDialog(jsonObject,
                    sessionId,
                    jsonObject.getInt("auth_type"),
                    password);
        } else {
            attemptDecryptPayload(password, responseBody);
        }
    }

    private void attemptDecryptPayload(String password, String payload) {
        getCompositeDisposable().add(
                payloadDataManager.initializeFromPayload(payload, password)
                        .doOnComplete(() -> {
                            prefsUtil.setValue(PrefsUtil.KEY_GUID, payloadDataManager.getWallet().getGuid());
                            appUtil.setSharedKey(payloadDataManager.getWallet().getSharedKey());
                            prefsUtil.setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
                        })
                        .subscribe(() -> getView().goToPinPage(),
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
        getView().showProgressDialog(R.string.check_email_to_auth_login, "120", true);

        getCompositeDisposable().add(authDataManager.createCheckEmailTimer()
                .takeUntil(integer -> !waitingForAuth)
                .subscribe(integer -> {
                    if (integer <= 0) {
                        // Only called if timer has run out
                        showErrorToastAndRestartApp(R.string.pairing_failed);
                    } else {
                        getView().updateWaitingForAuthDialog(integer);
                    }
                }, throwable -> {
                    showErrorToast(R.string.auth_failed);
                    waitingForAuth = false;
                }));
    }

    void onProgressCancelled() {
        waitingForAuth = false;
        onViewDestroyed();
    }

    private void showErrorToast(@StringRes int message) {
        getView().dismissProgressDialog();
        getView().resetPasswordField();
        getView().showToast(message, ToastCustom.TYPE_ERROR);
    }

    private void showErrorToastAndRestartApp(@StringRes int message) {
        getView().resetPasswordField();
        getView().dismissProgressDialog();
        getView().showToast(message, ToastCustom.TYPE_ERROR);
        appUtil.clearCredentialsAndRestart();
    }

    @NonNull
    AppUtil getAppUtil() {
        return appUtil;
    }
}
