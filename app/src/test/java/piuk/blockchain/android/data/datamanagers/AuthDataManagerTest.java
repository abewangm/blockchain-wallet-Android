package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.api.data.Status;
import info.blockchain.wallet.api.data.WalletOptions;
import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.services.WalletService;
import piuk.blockchain.android.util.AESUtilWrapper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;
import retrofit2.Response;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


public class AuthDataManagerTest extends RxTest {

    private static final String ERROR_BODY = "{\n" +
            "\t\"authorization_required\": \"true\"\n" +
            "}";

    @Mock private PrefsUtil prefsUtil;
    @Mock private WalletService walletService;
    @Mock private AppUtil appUtil;
    @Mock private AccessState accessState;
    @Mock private AESUtilWrapper aesUtilWrapper;
    @Mock private RxBus rxBus;
    @InjectMocks private AuthDataManager subject;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getEncryptedPayload() throws Exception {
        // Arrange
        ResponseBody mockResponseBody = mock(ResponseBody.class);
        when(walletService.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(Response.success(mockResponseBody)));
        // Act
        TestObserver<Response<ResponseBody>> observer = subject.getEncryptedPayload("1234567890", "1234567890").test();
        // Assert
        verify(walletService).getEncryptedPayload(anyString(), anyString());
        observer.assertComplete();
        observer.assertNoErrors();
        assertTrue(observer.values().get(0).isSuccessful());
    }

    @Test
    public void getSessionId() throws Exception {
        // Arrange
        String sessionId = "SESSION_ID";
        when(walletService.getSessionId(anyString())).thenReturn(Observable.just(sessionId));
        // Act
        TestObserver<String> testObserver = subject.getSessionId("1234567890").test();
        // Assert
        verify(walletService).getSessionId(anyString());
        testObserver.assertComplete();
        testObserver.onNext(sessionId);
        testObserver.assertNoErrors();
    }

    @Test
    public void submitTwoFactorCode() throws Exception {
        // Arrange
        String sessionId = "SESSION_ID";
        String guid = "GUID";
        String code = "123456";
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), "{}");
        when(walletService.submitTwoFactorCode(sessionId, guid, code))
                .thenReturn(Observable.just(responseBody));
        // Act
        TestObserver<ResponseBody> testObserver = subject.submitTwoFactorCode(sessionId, guid, code).test();
        // Assert
        verify(walletService).submitTwoFactorCode(sessionId, guid, code);
        testObserver.assertComplete();
        testObserver.onNext(responseBody);
        testObserver.assertNoErrors();
    }

    @Test
    public void validatePinSuccessful() throws Exception {
        // Arrange
        String pin = "1234";
        String key = "SHARED_KEY";
        String encryptedPassword = "ENCRYPTED_PASSWORD";
        String decryptionKey = "DECRYPTION_KEY";
        String plaintextPassword = "PLAINTEXT_PASSWORD";
        Status status = new Status();
        status.setSuccess(decryptionKey);
        when(prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn(key);
        when(prefsUtil.getValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, ""))
                .thenReturn(encryptedPassword);
        when(walletService.validateAccess(key, pin))
                .thenReturn(Observable.just(Response.success(status)));
        when(aesUtilWrapper.decrypt(encryptedPassword, decryptionKey, AESUtil.PIN_PBKDF2_ITERATIONS))
                .thenReturn(plaintextPassword);
        // Act
        TestObserver<String> observer = subject.validatePin(pin).test();
        // Assert
        verify(accessState).setPIN(pin);
        verifyNoMoreInteractions(accessState);
        verify(prefsUtil).getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "");
        verify(prefsUtil).getValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, "");
        verifyNoMoreInteractions(prefsUtil);
        verify(walletService).validateAccess(key, pin);
        verifyNoMoreInteractions(walletService);
        verify(aesUtilWrapper).decrypt(encryptedPassword, decryptionKey, AESUtil.PIN_PBKDF2_ITERATIONS);
        verifyNoMoreInteractions(aesUtilWrapper);
        verify(appUtil).setNewlyCreated(false);
        verifyZeroInteractions(appUtil);
        observer.assertComplete();
        observer.assertValue(plaintextPassword);
        observer.assertNoErrors();
    }

    @Test
    public void validatePinFailure() throws Exception {
        // Arrange
        String pin = "1234";
        String key = "SHARED_KEY";
        String encryptedPassword = "ENCRYPTED_PASSWORD";
        String decryptionKey = "DECRYPTION_KEY";
        Status status = new Status();
        status.setSuccess(decryptionKey);
        when(prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn(key);
        when(prefsUtil.getValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, ""))
                .thenReturn(encryptedPassword);
        when(walletService.validateAccess(key, pin))
                .thenReturn(Observable.just(Response.error(
                        500,
                        ResponseBody.create(MediaType.parse("application/json"), "{}"))));
        // Act
        TestObserver<String> observer = subject.validatePin(pin).test();
        // Assert
        verify(accessState).setPIN(pin);
        verifyNoMoreInteractions(accessState);
        verify(prefsUtil).getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "");
        verify(prefsUtil).getValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, "");
        verifyNoMoreInteractions(prefsUtil);
        verify(walletService).validateAccess(key, pin);
        verifyNoMoreInteractions(walletService);
        verifyZeroInteractions(aesUtilWrapper);
        observer.assertNotComplete();
        observer.assertNoValues();
        observer.assertError(InvalidCredentialsException.class);
    }

    @Test
    public void createPinInvalid() throws Exception {
        // Arrange
        String password = "PASSWORD";
        String pin = "123";
        // Act
        TestObserver<Void> observer = subject.createPin(password, pin).test();
        // Assert
        verifyZeroInteractions(accessState);
        verifyZeroInteractions(prefsUtil);
        verifyZeroInteractions(walletService);
        verifyZeroInteractions(aesUtilWrapper);
        observer.assertNotComplete();
        observer.assertError(Throwable.class);
    }

    @Test
    public void createPinSuccessful() throws Exception {
        // Arrange
        String password = "PASSWORD";
        String pin = "1234";
        String encryptedPassword = "ENCRYPTED_PASSWORD";
        Status status = new Status();
        when(walletService.setAccessKey(anyString(), anyString(), eq(pin)))
                .thenReturn(Observable.just(Response.success(status)));
        when(aesUtilWrapper.encrypt(eq(password), anyString(), eq(AESUtil.PIN_PBKDF2_ITERATIONS)))
                .thenReturn(encryptedPassword);
        // Act
        TestObserver<Void> observer = subject.createPin(password, pin).test();
        // Assert
        verify(accessState).setPIN(pin);
        verifyNoMoreInteractions(accessState);
        verify(appUtil).applyPRNGFixes();
        verifyNoMoreInteractions(appUtil);
        verify(walletService).setAccessKey(anyString(), anyString(), eq(pin));
        verifyNoMoreInteractions(walletService);
        verify(aesUtilWrapper).encrypt(eq(password), anyString(), eq(AESUtil.PIN_PBKDF2_ITERATIONS));
        verifyNoMoreInteractions(aesUtilWrapper);
        verify(prefsUtil).setValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, encryptedPassword);
        verify(prefsUtil).setValue(eq(PrefsUtil.KEY_PIN_IDENTIFIER), anyString());
        verifyNoMoreInteractions(prefsUtil);
        observer.assertComplete();
        observer.assertNoErrors();
    }

    @Test
    public void createPinError() throws Exception {
        // Arrange
        String password = "PASSWORD";
        String pin = "1234";
        when(walletService.setAccessKey(anyString(), anyString(), eq(pin)))
                .thenReturn(Observable.just(Response.error(
                        500,
                        ResponseBody.create(MediaType.parse("application/json"), "{}"))));
        // Act
        TestObserver<Void> observer = subject.createPin(password, pin).test();
        // Assert
        verify(accessState).setPIN(pin);
        verifyNoMoreInteractions(accessState);
        verify(appUtil).applyPRNGFixes();
        verifyNoMoreInteractions(appUtil);
        verify(walletService).setAccessKey(anyString(), anyString(), eq(pin));
        verifyNoMoreInteractions(walletService);
        verifyZeroInteractions(aesUtilWrapper);
        verifyZeroInteractions(prefsUtil);
        observer.assertNotComplete();
        observer.assertError(Throwable.class);
    }

    @Test
    public void getWalletOptions() throws Exception {
        // Arrange
        WalletOptions walletOptions = new WalletOptions();
        when(walletService.getWalletOptions()).thenReturn(Observable.just(walletOptions));
        // Act
        TestObserver<WalletOptions> observer = subject.getWalletOptions().test();
        // Assert
        verify(walletService).getWalletOptions();
        observer.assertComplete();
        observer.assertNoErrors();
        observer.assertValue(walletOptions);
    }

    /**
     * Getting encrypted payload returns error, should be caught by Observable and transformed into
     * {@link AuthDataManager#AUTHORIZATION_REQUIRED}
     */
    @Test
    public void startPollingAuthStatusError() throws Exception {
        // Arrange
        String sessionId = "SESSION_ID";
        String guid = "GUID";
        when(walletService.getEncryptedPayload(guid, sessionId)).thenReturn(Observable.error(new Throwable()));
        // Act
        TestObserver<String> testObserver = subject.startPollingAuthStatus(guid, sessionId).test();
        getTestScheduler().advanceTimeBy(3, TimeUnit.SECONDS);
        // Assert
        verify(walletService).getEncryptedPayload(guid, sessionId);
        testObserver.assertComplete();
        testObserver.assertValue(AuthDataManager.AUTHORIZATION_REQUIRED);
        testObserver.assertNoErrors();
    }

    /**
     * Getting encrypted payload returns Auth Required, should be filtered out and emit no values.
     */
    @Test
    public void startPollingAuthStatusAccessRequired() throws Exception {
        // Arrange
        String sessionId = "SESSION_ID";
        String guid = "GUID";
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), ERROR_BODY);
        when(walletService.getEncryptedPayload(guid, sessionId))
                .thenReturn(Observable.just(Response.error(500, responseBody)));
        // Act
        TestObserver<String> testObserver = subject.startPollingAuthStatus(guid, sessionId).test();
        getTestScheduler().advanceTimeBy(2, TimeUnit.SECONDS);
        // Assert
        verify(walletService).getEncryptedPayload(guid, sessionId);
        testObserver.assertNotComplete();
        testObserver.assertNoValues();
        testObserver.assertNoErrors();
    }

    @Test
    public void createCheckEmailTimer() throws Exception {
        // Arrange

        // Act
        TestObserver<Integer> testObserver = subject.createCheckEmailTimer().take(1).test();
        subject.timer = 1;
        getTestScheduler().advanceTimeBy(2, TimeUnit.SECONDS);
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(1);
    }

}