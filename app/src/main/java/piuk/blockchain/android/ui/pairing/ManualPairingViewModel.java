package piuk.blockchain.android.ui.pairing;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.annotations.Thunk;
import retrofit2.Response;

@SuppressWarnings("WeakerAccess")
public class ManualPairingViewModel extends BaseViewModel {

    @VisibleForTesting static final String KEY_AUTH_REQUIRED = "authorization_required";
    private static final String TAG = ManualPairingViewModel.class.getSimpleName();

    private DataListener dataListener;
    private String sessionId;
    @Inject protected AppUtil appUtil;
    @Inject protected AuthDataManager authDataManager;
    @VisibleForTesting boolean waitingForAuth = false;

    public interface DataListener {

        String getGuid();

        String getPassword();

        void goToPinPage();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void updateWaitingForAuthDialog(int secondsRemaining);

        void showProgressDialog(@StringRes int messageId, @Nullable String suffix, boolean cancellable);

        void dismissProgressDialog();

        void resetPasswordField();

        void showTwoFactorCodeNeededDialog(JSONObject responseObject, String sessionId, int authType, String guid, String password);

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

    void submitTwoFactorCode(JSONObject responseObject, String sessionId, String guid, String password, String code) {
        if (code == null || code.isEmpty()) {
            dataListener.showToast(R.string.two_factor_null_error, ToastCustom.TYPE_ERROR);
        } else {
            compositeDisposable.add(
                    authDataManager.submitTwoFactorCode(sessionId, guid, code)
                            .doOnSubscribe(disposable -> dataListener.showProgressDialog(R.string.please_wait, null, false))
                            .doAfterTerminate(() -> dataListener.dismissProgressDialog())
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

    private void verifyPassword(String password, String guid) {
        waitingForAuth = true;

        compositeDisposable.add(
                authDataManager.getSessionId(guid)
                        .doOnSubscribe(disposable -> dataListener.showProgressDialog(R.string.validating_password, null, false))
                        .doOnNext(s -> sessionId = s)
                        .flatMap(sessionId -> authDataManager.getEncryptedPayload(guid, sessionId))
                        .subscribe(response -> handleResponse(password, guid, response),
                                throwable -> {
                                    Log.e(TAG, "verifyPassword: ", throwable);
                                    showErrorToast(R.string.auth_failed);
                                }));
    }

    private void handleResponse(String password, String guid, Response<ResponseBody> response) throws IOException, JSONException {
        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
        if (response.isSuccessful()) {
            if (errorBody.contains(KEY_AUTH_REQUIRED)) {
                //2FA
                showCheckEmailDialog();

                compositeDisposable.add(
                        authDataManager.startPollingAuthStatus(guid, sessionId)
                                .subscribe(payloadResponse -> {
                                    waitingForAuth = false;

                                    if (payloadResponse == null || payloadResponse.contains(KEY_AUTH_REQUIRED)) {
                                        showErrorToast(R.string.auth_failed);
                                        return;
                                    }

                                    ResponseBody responseBody = ResponseBody.create(
                                            MediaType.parse("application/json"),
                                            payloadResponse);
                                    checkTwoFactor(password, guid, Response.success(responseBody));
                                }, throwable -> {
                                    waitingForAuth = false;
                                    showErrorToast(R.string.auth_failed);
                                }));
            } else {
                //No 2FA
                waitingForAuth = false;
                checkTwoFactor(password, guid, response);
            }
        } else {
            if (response.code() == 500) {
                showErrorToast(R.string.invalid_guid);
            } else {
                showErrorToast(R.string.unexpected_error);
            }
        }
    }

    private void checkTwoFactor(String password, String guid, Response<ResponseBody> response) throws
            IOException, JSONException {

        String responseBody = response.body().string();
        JSONObject jsonObject = new JSONObject(responseBody);
        // Check if the response has a 2FA Auth Type but is also missing the payload,
        // as it comes in two parts if 2FA enabled.
        if (jsonObject.has("auth_type") && !jsonObject.has("payload")
                && (jsonObject.getInt("auth_type") == Settings.AUTH_TYPE_GOOGLE_AUTHENTICATOR
                || jsonObject.getInt("auth_type") == Settings.AUTH_TYPE_SMS)) {

            dataListener.dismissProgressDialog();
            dataListener.showTwoFactorCodeNeededDialog(jsonObject,
                    sessionId,
                    jsonObject.getInt("auth_type"),
                    guid,
                    password);
        } else {
            attemptDecryptPayload(password, responseBody);
        }
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
        compositeDisposable.add(
                authDataManager.createCheckEmailTimer()
                        .doOnSubscribe(disposable -> dataListener.showProgressDialog(R.string.check_email_to_auth_login, "120", true))
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
        appUtil.clearCredentials();
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
