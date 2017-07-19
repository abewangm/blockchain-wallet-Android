package piuk.blockchain.android.ui.login;

import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.reactivex.Completable;
import io.reactivex.Observable;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import piuk.blockchain.android.data.auth.AuthDataManager;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;
import retrofit2.Response;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static piuk.blockchain.android.ui.login.ManualPairingPresenter.KEY_AUTH_REQUIRED;


public class ManualPairingPresenterTest {

    private static final String KEY_AUTH_REQUIRED_JSON = "{\n" +
            "  \"authorization_required\": true\n" +
            "}";

    private static final String TWO_FA_RESPONSE = "{\n" +
            "  \"auth_type\": 5\n" +
            "}";

    private ManualPairingPresenter mSubject;

    @Mock private ManualPairingActivity mActivity;
    @Mock private AppUtil mAppUtil;
    @Mock private AuthDataManager mAuthDataManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private PayloadDataManager mPayloadDataManager;
    @Mock private PrefsUtil mPrefsUtil;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mSubject = new ManualPairingPresenter(mAppUtil, mAuthDataManager, mPayloadDataManager, mPrefsUtil);
        mSubject.initView(mActivity);
    }

    /**
     * Password is missing, should trigger {@link ManualPairingActivity#showToast(int, String)}
     */
    @Test
    public void onContinueClickedNoPassword() throws Exception {
        // Arrange
        when(mActivity.getGuid()).thenReturn("1234567890");
        when(mActivity.getPassword()).thenReturn("");
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
    }

    /**
     * GUID is missing, should trigger {@link ManualPairingActivity#showToast(int, String)}
     */
    @Test
    public void onContinueClickedNoGuid() throws Exception {
        // Arrange
        when(mActivity.getGuid()).thenReturn("");
        when(mActivity.getPassword()).thenReturn("1234567890");
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
    }

    /**
     * Password is correct, should trigger {@link ManualPairingActivity#goToPinPage()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedCorrectPassword() throws Exception {
        // Arrange
        when(mActivity.getGuid()).thenReturn("1234567890");
        when(mActivity.getPassword()).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), "{}");
        Response response = Response.success(responseBody);
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(mAuthDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        when(mPayloadDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.complete());
        when(mPayloadDataManager.getWallet().getGuid()).thenReturn("guid");
        when(mPayloadDataManager.getWallet().getSharedKey()).thenReturn("shared_key");
        // Act
        mSubject.onContinueClicked();
        // Assert
        verify(mActivity).goToPinPage();
        verify(mPrefsUtil).setValue(anyString(), anyString());
        verify(mPrefsUtil).setValue(anyString(), anyBoolean());
        verify(mAppUtil).setSharedKey(anyString());
    }

    /**
     * Password is correct but 2FA is enabled, should trigger {@link ManualPairingActivity#showTwoFactorCodeNeededDialog(JSONObject,
     * String, int, String, String)}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedCorrectPasswordTwoFa() throws Exception {
        // Arrange
        when(mActivity.getGuid()).thenReturn("1234567890");
        when(mActivity.getPassword()).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), TWO_FA_RESPONSE);
        Response response = Response.success(responseBody);
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(mAuthDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        when(mPayloadDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.complete());
        // Act
        mSubject.onContinueClicked();
        // Assert
        verify(mActivity).dismissProgressDialog();
        verify(mActivity).showTwoFactorCodeNeededDialog(any(), anyString(), anyInt(), anyString(), anyString());
    }

    /**
     * AuthDataManager returns a failure when getting encrypted payload, should trigger {@link
     * ManualPairingActivity#showToast(int, String)}
     */
    @Test
    public void onContinueClickedPairingFailure() throws Exception {
        // Arrange
        when(mActivity.getGuid()).thenReturn("1234567890");
        when(mActivity.getPassword()).thenReturn("1234567890");
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.error(new Throwable()));
        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
        verify(mActivity).dismissProgressDialog();
    }

    /**
     * AuthDataManager returns failure when polling auth status, should trigger {@link
     * ManualPairingActivity#showToast(int, String)}
     */
    @Ignore("This has never actually worked, but refactoring has highlighted the failure")
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedCreateFailure() throws Exception {
        // Arrange
        when(mActivity.getGuid()).thenReturn("1234567890");
        when(mActivity.getPassword()).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), "{}");
        Response response = Response.success(responseBody);
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(mAuthDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
        verify(mActivity).dismissProgressDialog();
    }

    /**
     * AuthDataManager returns a {@link DecryptionException}, should trigger {@link
     * ManualPairingActivity#showToast(int, String)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedDecryptionFailure() throws Exception {
        // Arrange
        when(mActivity.getGuid()).thenReturn("1234567890");
        when(mActivity.getPassword()).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), "{}");
        Response response = Response.success(responseBody);
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(mAuthDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        when(mPayloadDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.error(new DecryptionException()));
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
        verify(mActivity).dismissProgressDialog();
    }

    /**
     * AuthDataManager returns a {@link HDWalletException}, should trigger {@link
     * ManualPairingActivity#showToast(int, String)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedHDWalletExceptionFailure() throws Exception {
        // Arrange
        when(mActivity.getGuid()).thenReturn("1234567890");
        when(mActivity.getPassword()).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), "{}");
        Response response = Response.success(responseBody);
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(mAuthDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        when(mPayloadDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.error(new HDWalletException()));
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
        verify(mActivity).dismissProgressDialog();
    }

    /**
     * AuthDataManager returns a fatal exception, should restart the app and clear credentials.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedFatalErrorClearData() throws Exception {
        // Arrange
        when(mActivity.getGuid()).thenReturn("1234567890");
        when(mActivity.getPassword()).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), "{}");
        Response response = Response.success(responseBody);
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(mAuthDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        when(mPayloadDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.error(new RuntimeException()));
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
        verify(mActivity).dismissProgressDialog();
        verify(mAppUtil).clearCredentialsAndRestart();
    }

    /**
     * AuthDataManager returns an error when getting session ID, should trigger {@link
     * AppUtil#clearCredentialsAndRestart()}
     */
    @Test
    public void onContinueClickedFatalError() throws Exception {
        // Arrange
        when(mActivity.getGuid()).thenReturn("1234567890");
        when(mActivity.getPassword()).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString()))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
        verify(mActivity).dismissProgressDialog();
        verify(mAppUtil).clearCredentials();
    }


    /**
     * {@link AuthDataManager#getEncryptedPayload(String, String)} throws exception. Should restart
     * the app via {@link AppUtil#clearCredentialsAndRestart()}
     */
    @Test
    public void onContinueClickedEncryptedPayloadFailure() throws Exception {
        // Arrange
        when(mActivity.getGuid()).thenReturn("1234567890");
        when(mActivity.getPassword()).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
        verify(mActivity).dismissProgressDialog();
        verify(mAppUtil).clearCredentials();
    }

    /**
     * {@link AuthDataManager#startPollingAuthStatus(String, String)} returns Access Required.
     * Should restart the app via {@link AppUtil#clearCredentialsAndRestart()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedWaitingForAuthRequired() throws Exception {
        // Arrange
        when(mActivity.getGuid()).thenReturn("1234567890");
        when(mActivity.getPassword()).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), KEY_AUTH_REQUIRED_JSON);
        Response response = Response.error(500, responseBody);
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(mAuthDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just(KEY_AUTH_REQUIRED));
        when(mAuthDataManager.createCheckEmailTimer()).thenReturn(Observable.just(1));
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
        verify(mAppUtil).clearCredentials();
    }

    /**
     * {@link AuthDataManager#startPollingAuthStatus(String, String)} returns payload. Should
     * attempt to decrypt the payload.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedWaitingForAuthSuccess() throws Exception {
        // Arrange
        when(mActivity.getGuid()).thenReturn("1234567890");
        when(mActivity.getPassword()).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), KEY_AUTH_REQUIRED_JSON);
        Response response = Response.error(500, responseBody);
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(mAuthDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("{}"));
        when(mAuthDataManager.createCheckEmailTimer()).thenReturn(Observable.just(1));
        when(mPayloadDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.complete());
        // Act
        mSubject.onContinueClicked();
        // Assert
        verify(mPayloadDataManager).initializeFromPayload(anyString(), anyString());
    }

    /**
     * {@link AuthDataManager#createCheckEmailTimer()} throws an error. Should show error toast.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedWaitingForAuthEmailTimerError() throws Exception {
        // Arrange
        when(mActivity.getGuid()).thenReturn("1234567890");
        when(mActivity.getPassword()).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), KEY_AUTH_REQUIRED_JSON);
        Response response = Response.error(500, responseBody);
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(mAuthDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("{}"));
        when(mAuthDataManager.createCheckEmailTimer())
                .thenReturn(Observable.error(new Throwable()));
        when(mPayloadDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.complete());
        // Act
        mSubject.onContinueClicked();
        // Assert
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
    }

    /**
     * {@link AuthDataManager#startPollingAuthStatus(String, String)} returns an error. Should
     * restart the app via {@link AppUtil#clearCredentialsAndRestart()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedWaitingForAuthFailure() throws Exception {
        // Arrange
        when(mActivity.getGuid()).thenReturn("1234567890");
        when(mActivity.getPassword()).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), KEY_AUTH_REQUIRED_JSON);
        Response response = Response.error(500, responseBody);
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(mAuthDataManager.createCheckEmailTimer()).thenReturn(Observable.just(1));
        when(mAuthDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
        verify(mAppUtil).clearCredentials();
    }

    /**
     * {@link AuthDataManager#startPollingAuthStatus(String, String)} counts down to zero. Should
     * restart the app via {@link AppUtil#clearCredentialsAndRestart()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedWaitingForAuthCountdownComplete() throws Exception {
        when(mActivity.getGuid()).thenReturn("1234567890");
        when(mActivity.getPassword()).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), KEY_AUTH_REQUIRED_JSON);
        Response response = Response.error(500, responseBody);
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(mAuthDataManager.createCheckEmailTimer()).thenReturn(Observable.just(0));
        when(mAuthDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity, times(2)).showToast(anyInt(), anyString());
        verify(mActivity, times(2)).resetPasswordField();
        verify(mAppUtil).clearCredentialsAndRestart();
    }

    @Test
    public void submitTwoFactorCodeNull() throws Exception {
        // Arrange
        JSONObject responseObject = new JSONObject();
        String sessionId = "SESSION_ID";
        String guid = "GUID";
        String password = "PASSWORD";
        // Act
        mSubject.submitTwoFactorCode(responseObject, sessionId, guid, password, null);
        // Assert
        verify(mActivity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void submitTwoFactorCodeFailed() throws Exception {
        // Arrange
        JSONObject responseObject = new JSONObject();
        String sessionId = "SESSION_ID";
        String guid = "GUID";
        String password = "PASSWORD";
        String code = "123456";
        when(mAuthDataManager.submitTwoFactorCode(sessionId, guid, code))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        mSubject.submitTwoFactorCode(responseObject, sessionId, guid, password, code);
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), isNull(), eq(false));
        // TODO: 12/04/2017 There's a bug in Mockito here were it asserts that this was called twice.
        // Check this with Mockito 2.8.0 when it makes it onto bintray
        verify(mActivity, atLeastOnce()).dismissProgressDialog();
        verify(mActivity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(mAuthDataManager).submitTwoFactorCode(sessionId, guid, code);
    }

    @Test
    public void submitTwoFactorCodeSuccess() throws Exception {
        // Arrange
        JSONObject responseObject = new JSONObject();
        String sessionId = "SESSION_ID";
        String guid = "GUID";
        String password = "PASSWORD";
        String code = "123456";
        when(mAuthDataManager.submitTwoFactorCode(sessionId, guid, code))
                .thenReturn(Observable.just(ResponseBody.create(MediaType.parse("application/json"), TWO_FA_RESPONSE)));
        when(mPayloadDataManager.initializeFromPayload(anyString(), eq(password))).thenReturn(Completable.complete());
        // Act
        mSubject.submitTwoFactorCode(responseObject, sessionId, guid, password, code);
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), isNull(), eq(false));
        // TODO: 12/04/2017 There's a bug in Mockito here were it asserts that this was called twice.
        // Check this with Mockito 2.8.0 when it makes it onto bintray
        verify(mActivity, atLeastOnce()).dismissProgressDialog();
        verify(mActivity).goToPinPage();
        verify(mAuthDataManager).submitTwoFactorCode(sessionId, guid, code);
        verify(mPayloadDataManager).initializeFromPayload(anyString(), eq(password));
    }

    @Test
    public void onProgressCancelled() throws Exception {
        // Arrange

        // Act
        mSubject.onProgressCancelled();
        // Assert
        assertFalse(mSubject.waitingForAuth);
        assertEquals(0, mSubject.getCompositeDisposable().size());
    }

    @Test
    public void getAppUtil() throws Exception {
        // Arrange

        // Act
        AppUtil util = mSubject.getAppUtil();
        // Assert
        assertEquals(util, mAppUtil);
    }

    @Test
    public void onViewReady() throws Exception {
        // Arrange

        // Act
        mSubject.onViewReady();
        // Assert
        assertTrue(true);
    }

}