package piuk.blockchain.android.ui.auth;

import android.app.Application;

import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import io.reactivex.Completable;
import io.reactivex.Observable;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.DialogButtonCallback;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;
import retrofit2.Response;

import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static piuk.blockchain.android.ui.auth.PasswordRequiredViewModel.KEY_AUTH_REQUIRED;


@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class PasswordRequiredViewModelTest extends RxTest {

    private PasswordRequiredViewModel subject;
    @Mock private PasswordRequiredActivity activity;
    @Mock private AppUtil appUtil;
    @Mock private PrefsUtil prefsUtil;
    @Mock private AuthDataManager authDataManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new ApiModule(),
                new MockDataManagerModule());

        subject = new PasswordRequiredViewModel(activity);
    }

    /**
     * Password is missing, should trigger {@link PasswordRequiredActivity#showToast(int, String)}
     */
    @Test
    public void onContinueClickedNoPassword() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("");
        when(activity.getPassword()).thenReturn("");
        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
    }

    /**
     * Password is correct, should trigger {@link PasswordRequiredActivity#goToPinPage()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedCorrectPassword() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), "");
        Response response = Response.success(responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        when(authDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.complete());

        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).goToPinPage();
    }

    /**
     * AuthDataManager returns a failure when getting encrypted payload, should trigger {@link
     * PasswordRequiredActivity#showToast(int, String)}
     */
    @Test
    public void onContinueClickedPairingFailure() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.error(new Throwable()));
        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
        verify(activity).dismissProgressDialog();
    }


    /**
     * AuthDataManager returns failure when polling auth status, should trigger {@link
     * PasswordRequiredActivity#showToast(int, String)}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedCreateFailure() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), "");
        Response response = Response.success(responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.error(new Throwable()));

        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
        verify(activity).dismissProgressDialog();
    }


    /**
     * AuthDataManager returns a {@link DecryptionException}, should trigger {@link
     * PasswordRequiredActivity#showToast(int, String)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedDecryptionFailure() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), "");
        Response response = Response.success(responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        when(authDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.error(new DecryptionException()));

        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
        verify(activity).dismissProgressDialog();
    }

    /**
     * AuthDataManager returns a {@link HDWalletException}, should trigger {@link
     * PasswordRequiredActivity#showToast(int, String)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedHDWalletExceptionFailure() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), "");
        Response response = Response.success(responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        when(authDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.error(new HDWalletException()));

        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
        verify(activity).dismissProgressDialog();
    }

    /**
     * AuthDataManager returns a fatal exception, should restart the app and clear credentials.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedFatalErrorClearData() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), "");
        Response response = Response.success(responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        when(authDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.error(new RuntimeException()));

        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
        verify(activity).dismissProgressDialog();
        verify(appUtil).clearCredentialsAndRestart();
    }

    /**
     * AuthDataManager returns an error when getting session ID, should trigger {@link
     * AppUtil#clearCredentialsAndRestart()}
     */
    @Test
    public void onContinueClickedFatalError() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString()))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
        verify(activity).dismissProgressDialog();
        verify(appUtil).clearCredentialsAndRestart();
    }


    /**
     * {@link AuthDataManager#getEncryptedPayload(String, String)} throws exception. Should restart
     * the app via {@link AppUtil#clearCredentialsAndRestart()}
     */
    @Test
    public void onContinueClickedEncryptedPayloadFailure() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
        verify(activity).dismissProgressDialog();
        verify(appUtil).clearCredentialsAndRestart();
    }

    /**
     * {@link AuthDataManager#startPollingAuthStatus(String, String)} returns Access Required.
     * Should restart the app via {@link AppUtil#clearCredentialsAndRestart()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedWaitingForAuthRequired() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), KEY_AUTH_REQUIRED);
        Response response = Response.error(500, responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just(KEY_AUTH_REQUIRED));
        when(authDataManager.createCheckEmailTimer()).thenReturn(Observable.just(1));
        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
        verify(appUtil).clearCredentialsAndRestart();
    }

    /**
     * {@link AuthDataManager#startPollingAuthStatus(String, String)} returns payload. Should
     * attempt to decrypt the payload.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedWaitingForAuthSuccess() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), KEY_AUTH_REQUIRED);
        Response response = Response.error(500, responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        when(authDataManager.createCheckEmailTimer()).thenReturn(Observable.just(1));
        when(authDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.complete());
        // Act
        subject.onContinueClicked();
        // Assert
        verify(authDataManager).initializeFromPayload(anyString(), anyString());
    }

    /**
     * {@link AuthDataManager#createCheckEmailTimer()} throws an error. Should show error toast.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedWaitingForAuthEmailTimerError() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), KEY_AUTH_REQUIRED);
        Response response = Response.error(500, responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        when(authDataManager.createCheckEmailTimer())
                .thenReturn(Observable.error(new Throwable()));
        when(authDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.complete());
        // Act
        subject.onContinueClicked();
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
    }

    /**
     * {@link AuthDataManager#startPollingAuthStatus(String, String)} returns an error. Should
     * restart the app via {@link AppUtil#clearCredentialsAndRestart()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedWaitingForAuthFailure() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), KEY_AUTH_REQUIRED);
        Response response = Response.error(500, responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.createCheckEmailTimer()).thenReturn(Observable.just(1));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
        verify(appUtil).clearCredentialsAndRestart();
    }

    /**
     * {@link AuthDataManager#startPollingAuthStatus(String, String)} counts down to zero. Should
     * restart the app via {@link AppUtil#clearCredentialsAndRestart()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedWaitingForAuthCountdownComplete() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");
        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), KEY_AUTH_REQUIRED);
        Response response = Response.error(500, responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.createCheckEmailTimer()).thenReturn(Observable.just(0));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity, times(2)).showToast(anyInt(), anyString());
        verify(activity, times(2)).resetPasswordField();
        verify(appUtil, times(2)).clearCredentialsAndRestart();
    }

    @Test
    public void onProgressCancelled() throws Exception {
        // Arrange

        // Act
        subject.onProgressCancelled();
        // Assert
        assertFalse(subject.waitingForAuth);
        assertEquals(0, subject.compositeDisposable.size());
    }

    @Test
    public void onForgetWalletClickedShowWarningAndContinue() throws Exception {
        // Arrange
        doAnswer(invocation -> {
            ((DialogButtonCallback) invocation.getArguments()[0]).onPositiveClicked();
            return null;
        }).when(activity).showForgetWalletWarning(any(DialogButtonCallback.class));
        // Act
        subject.onForgetWalletClicked();
        // Assert
        verify(activity).showForgetWalletWarning(any(DialogButtonCallback.class));
        verify(appUtil).clearCredentialsAndRestart();
    }

    @Test
    public void onForgetWalletClickedShowWarningAndDismiss() throws Exception {
        // Arrange
        doAnswer(invocation -> {
            ((DialogButtonCallback) invocation.getArguments()[0]).onNegativeClicked();
            return null;
        }).when(activity).showForgetWalletWarning(any(DialogButtonCallback.class));
        // Act
        subject.onForgetWalletClicked();
        // Assert
        verify(activity).showForgetWalletWarning(any(DialogButtonCallback.class));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void getAppUtil() throws Exception {
        // Arrange

        // Act
        AppUtil util = subject.getAppUtil();
        // Assert
        assertEquals(util, appUtil);
    }

    @Test
    public void onViewReady() throws Exception {
        // Arrange

        // Act
        subject.onViewReady();
        // Assert
        assertTrue(true);
    }

    @SuppressWarnings("SyntheticAccessorCall")
    private class MockApplicationModule extends ApplicationModule {

        MockApplicationModule(Application application) {
            super(application);
        }

        @Override
        protected PrefsUtil providePrefsUtil() {
            return prefsUtil;
        }

        @Override
        protected AppUtil provideAppUtil() {
            return appUtil;
        }
    }

    @SuppressWarnings("SyntheticAccessorCall")
    private class MockDataManagerModule extends DataManagerModule {

        @Override
        protected AuthDataManager provideAuthDataManager(PayloadDataManager payloadDataManager,
                                                         PrefsUtil prefsUtil, AppUtil appUtil,
                                                         AccessState accessState,
                                                         StringUtils stringUtils) {
            return authDataManager;
        }
    }
}