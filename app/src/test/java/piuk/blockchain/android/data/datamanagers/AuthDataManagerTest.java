package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.api.data.WalletOptions;
import info.blockchain.wallet.payload.data.Wallet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.services.WalletService;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;
import retrofit2.Response;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class AuthDataManagerTest extends RxTest {

    private static final String ERROR_BODY = "{\n" +
            "\t\"authorization_required\": \"true\"\n" +
            "}";

    @Mock private PayloadDataManager payloadDataManager;
    @Mock private PrefsUtil prefsUtil;
    @Mock private WalletService walletService;
    @Mock private AppUtil appUtil;
    @Mock private AccessState accessState;
    @Mock private StringUtils stringUtils;
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
        String code= "123456";
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
    public void updatePayload() throws Exception {
        // Arrange
        String sharedKey = "SHARED_KEY";
        String guid = "GUID";
        String password = "PASSWORD";
        when(payloadDataManager.initializeAndDecrypt(sharedKey, guid, password)).thenReturn(Completable.complete());
        // Act
        TestObserver<Void> observer = subject.updatePayload(sharedKey, guid, password).test();
        // Assert
        verify(payloadDataManager).initializeAndDecrypt(sharedKey, guid, password);
        observer.assertComplete();
        observer.assertNoErrors();
    }

    @Test
    public void validatePin() throws Exception {
        // Arrange
        String decryptedPassword = "1234567890";
        when(accessState.validatePin(anyString())).thenReturn(Observable.just(decryptedPassword));
        // Act
        TestObserver<String> observer = subject.validatePin(anyString()).test();
        // Assert
        verify(accessState).validatePin(anyString());
        observer.assertComplete();
        observer.onNext(decryptedPassword);
        observer.assertNoErrors();
    }

    @Test
    public void createPin() throws Exception {
        // Arrange
        when(accessState.createPin(any(String.class), anyString())).thenReturn(Observable.just(true));
        // Act
        TestObserver<Boolean> observer = subject.createPin("", "").test();
        // Assert
        verify(accessState).createPin(any(String.class), anyString());
        observer.assertComplete();
        observer.assertValue(true);
        observer.assertNoErrors();
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

    @Test
    public void createHdWallet() throws Exception {
        // Arrange
        Wallet payload = new Wallet();
        payload.setSharedKey("shared key");
        payload.setGuid("guid");
        when(payloadDataManager.createHdWallet(anyString(), anyString(), anyString()))
                .thenReturn(Observable.just(payload));
        // Act
        TestObserver<Wallet> observer = subject.createHdWallet("", "", "").test();
        // Assert
        verify(payloadDataManager).createHdWallet(anyString(), anyString(), anyString());
        verify(appUtil).setSharedKey("shared key");
        verify(appUtil).setNewlyCreated(true);
        verify(prefsUtil).setValue(PrefsUtil.KEY_GUID, "guid");
        observer.assertComplete();
        observer.onNext(payload);
        observer.assertNoErrors();
    }

    @Test
    public void restoreHdWallet() throws Exception {
        // Arrange
        Wallet payload = new Wallet();
        payload.setSharedKey("shared key");
        payload.setGuid("guid");
        when(payloadDataManager.restoreHdWallet(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Observable.just(payload));
        when(stringUtils.getString(anyInt())).thenReturn("string resource");
        // Act
        TestObserver<Wallet> observer = subject.restoreHdWallet("", "", "").test();
        // Assert
        verify(payloadDataManager).restoreHdWallet(anyString(), anyString(), anyString(), anyString());
        verify(appUtil).setSharedKey("shared key");
        verify(appUtil).setNewlyCreated(true);
        verify(prefsUtil).setValue(PrefsUtil.KEY_GUID, "guid");
        observer.assertComplete();
        observer.onNext(payload);
        observer.assertNoErrors();
    }

    @Test
    public void initializeFromPayload() throws Exception {
        // Arrange
        String payload = "PAYLOAD";
        String password = "PASSWORD";
        String guid = "GUID";
        String sharedKey = "SHARED_KEY";
        Wallet mockWallet = mock(Wallet.class);
        when(mockWallet.getGuid()).thenReturn(guid);
        when(mockWallet.getSharedKey()).thenReturn(sharedKey);
        when(payloadDataManager.getWallet()).thenReturn(mockWallet);
        when(payloadDataManager.initializeFromPayload(payload, password)).thenReturn(Completable.complete());
        // Act
        TestObserver<Void> testObserver = subject.initializeFromPayload(payload, password).test();
        // Assert
        verify(payloadDataManager).initializeFromPayload(payload, password);
        verify(payloadDataManager, times(2)).getWallet();
        verify(prefsUtil).setValue(PrefsUtil.KEY_GUID, guid);
        verify(prefsUtil).setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
        verify(appUtil).setSharedKey(sharedKey);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
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