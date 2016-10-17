package piuk.blockchain.android.data.datamanagers;

import info.blockchain.api.WalletPayload;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.exceptions.PayloadException;
import info.blockchain.wallet.exceptions.ServerConnectionException;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.CharSequenceX;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.services.WalletPayloadService;
import piuk.blockchain.android.util.AESUtilWrapper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;
import rx.Observable;
import rx.observers.TestSubscriber;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by adambennett on 15/08/2016.
 */
public class AuthDataManagerTest extends RxTest {

    private static final String STRING_TO_RETURN = "string_to_return";

    @Mock private PayloadManager mPayloadManager;
    @Mock private PrefsUtil mPrefsUtil;
    @Mock private WalletPayloadService mWalletPayloadService;
    @Mock private AppUtil mAppUtil;
    @Mock private AESUtilWrapper mAesUtils;
    @Mock private AccessState mAccessState;
    @Mock private StringUtils mStringUtils;
    @InjectMocks AuthDataManager mSubject;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getEncryptedPayload() throws Exception {
        // Arrange
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        when(mWalletPayloadService.getEncryptedPayload(anyString(), anyString())).thenReturn(Observable.just(STRING_TO_RETURN));
        // Act
        mSubject.getEncryptedPayload("1234567890", "1234567890").toBlocking().subscribe(subscriber);
        // Assert
        verify(mWalletPayloadService).getEncryptedPayload(anyString(), anyString());
        subscriber.assertCompleted();
        subscriber.onNext(STRING_TO_RETURN);
        subscriber.assertNoErrors();
    }

    @Test
    public void getSessionId() throws Exception {
        // Arrange
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        when(mWalletPayloadService.getSessionId(anyString())).thenReturn(Observable.just(STRING_TO_RETURN));
        // Act
        mSubject.getSessionId("1234567890").toBlocking().subscribe(subscriber);
        // Assert
        verify(mWalletPayloadService).getSessionId(anyString());
        subscriber.assertCompleted();
        subscriber.onNext(STRING_TO_RETURN);
        subscriber.assertNoErrors();
    }

    @Test
    public void validatePin() throws Exception {
        // Arrange
        TestSubscriber<CharSequenceX> subscriber = new TestSubscriber<>();
        CharSequenceX charSequenceX = new CharSequenceX("1234567890");
        when(mAccessState.validatePin(anyString())).thenReturn(Observable.just(charSequenceX));
        // Act
        mSubject.validatePin(anyString()).toBlocking().subscribe(subscriber);
        // Assert
        verify(mAccessState).validatePin(anyString());
        subscriber.assertCompleted();
        subscriber.onNext(charSequenceX);
        subscriber.assertNoErrors();
    }

    @Test
    public void createPin() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        when(mAccessState.createPin(any(CharSequenceX.class), anyString())).thenReturn(Observable.just(true));
        // Act
        mSubject.createPin(any(CharSequenceX.class), anyString()).toBlocking().subscribe(subscriber);
        // Assert
        verify(mAccessState).createPin(any(CharSequenceX.class), anyString());
        subscriber.assertCompleted();
        subscriber.onNext(true);
        subscriber.assertNoErrors();
    }

    @Test
    public void createHdWallet() throws Exception {
        // Arrange
        TestSubscriber<Payload> subscriber = new TestSubscriber<>();
        Payload payload = new Payload();
        when(mPayloadManager.createHDWallet(anyString(), anyString())).thenReturn(payload);
        // Act
        mSubject.createHdWallet("", "").toBlocking().subscribe(subscriber);
        // Assert
        verify(mPayloadManager).createHDWallet(anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(mAppUtil).setNewlyCreated(true);
        verify(mPrefsUtil).setValue(eq(PrefsUtil.KEY_GUID), anyString());
        subscriber.assertCompleted();
        subscriber.onNext(payload);
        subscriber.assertNoErrors();
    }

    @Test
    public void restoreHdWallet() throws Exception {
        // Arrange
        TestSubscriber<Payload> subscriber = new TestSubscriber<>();
        Payload payload = new Payload();
        when(mPayloadManager.restoreHDWallet(anyString(), anyString(), anyString())).thenReturn(payload);
        // Act
        mSubject.restoreHdWallet("", "", "").toBlocking().subscribe(subscriber);
        // Assert
        verify(mPayloadManager).restoreHDWallet(anyString(), anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(mPrefsUtil).setValue(eq(PrefsUtil.KEY_GUID), anyString());
        subscriber.assertCompleted();
        subscriber.onNext(payload);
        subscriber.assertNoErrors();
    }

    /**
     * Payload returns null, which indicates save failure. Should throw an Exception
     */
    @Test
    public void restoreHdWalletNullPayload() throws Exception {
        // Arrange
        TestSubscriber<Payload> subscriber = new TestSubscriber<>();
        when(mPayloadManager.restoreHDWallet(anyString(), anyString(), anyString())).thenReturn(null);
        // Act
        mSubject.restoreHdWallet("", "", "").toBlocking().subscribe(subscriber);
        // Assert
        verify(mPayloadManager).restoreHDWallet(anyString(), anyString(), anyString());
        verifyZeroInteractions(mAppUtil);
        verifyZeroInteractions(mPrefsUtil);
        subscriber.assertNotCompleted();
        subscriber.assertError(Throwable.class);
    }

    /**
     * Access returns a valid payload, Observable should complete successfully
     */
    @Test
    public void startPollingAuthStatusSuccess() throws Exception {
        // Arrange
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        when(mWalletPayloadService.getSessionId(anyString())).thenReturn(Observable.just(STRING_TO_RETURN));
        when(mWalletPayloadService.getEncryptedPayload(anyString(), anyString())).thenReturn(Observable.just(STRING_TO_RETURN));
        // Act
        mSubject.startPollingAuthStatus("1234567890").toBlocking().subscribe(subscriber);
        // Assert
        verify(mWalletPayloadService).getSessionId(anyString());
        verify(mWalletPayloadService).getEncryptedPayload(anyString(), anyString());
        subscriber.assertCompleted();
        subscriber.onNext(STRING_TO_RETURN);
        subscriber.assertNoErrors();
    }

    /**
     * Getting encrypted payload returns error, should be caught by Observable and transformed into
     * {@link WalletPayload#KEY_AUTH_REQUIRED}
     */
    @Test
    public void startPollingAuthStatusError() throws Exception {
        // Arrange
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        when(mWalletPayloadService.getSessionId(anyString())).thenReturn(Observable.just(STRING_TO_RETURN));
        when(mWalletPayloadService.getEncryptedPayload(anyString(), anyString())).thenThrow(mock(RuntimeException.class));
        // Act
        mSubject.startPollingAuthStatus("1234567890").toBlocking().subscribe(subscriber);
        // Assert
        verify(mWalletPayloadService).getSessionId(anyString());
        verify(mWalletPayloadService).getEncryptedPayload(anyString(), anyString());
        subscriber.assertCompleted();
        subscriber.onNext(WalletPayload.KEY_AUTH_REQUIRED);
        subscriber.assertNoErrors();
    }

    /**
     * Getting encrypted payload returns Auth Required, should be filtered out and emit no values.
     */
    @Test
    public void startPollingAuthStatusAccessRequired() throws Exception {
        // Arrange
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        when(mWalletPayloadService.getSessionId(anyString())).thenReturn(Observable.just(STRING_TO_RETURN));
        when(mWalletPayloadService.getEncryptedPayload(anyString(), anyString())).thenReturn(Observable.just(WalletPayload.KEY_AUTH_REQUIRED));
        // Act
        mSubject.startPollingAuthStatus("1234567890").take(1, TimeUnit.SECONDS).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoValues();
        subscriber.assertNoErrors();
    }

    /**
     * Update payload completes successfully, should set temp password and complete with no errors
     */
    @Test
    public void initiatePayloadSuccess() throws Exception {
        // Arrange
        TestSubscriber<Void> subscriber = new TestSubscriber<>();
        doAnswer(invocation -> {
            ((PayloadManager.InitiatePayloadListener) invocation.getArguments()[3]).onSuccess();
            return null;
        }).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));
        // Act
        mSubject.updatePayload("1234567890", "1234567890", new CharSequenceX("1234567890")).toBlocking().subscribe(subscriber);
        // Assert
        verify(mPayloadManager).setTempPassword(any(CharSequenceX.class));
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
    }

    /**
     * Update payload returns a credential failure, Observable should throw {@link InvalidCredentialsException}
     */
    @Test
    public void initiateCredentialFail() throws Exception {
        // Arrange
        TestSubscriber<Void> subscriber = new TestSubscriber<>();

        doThrow(new InvalidCredentialsException()).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));
        // Act
        mSubject.updatePayload("1234567890", "1234567890", new CharSequenceX("1234567890")).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertNotCompleted();
        subscriber.assertError(Throwable.class);
    }

    /**
     * Update payload returns a Payload exception, Observable should throw {@link PayloadException}
     */
    @Test
    public void initiatePayloadFail() throws Exception {
        // Arrange
        TestSubscriber<Void> subscriber = new TestSubscriber<>();

        doThrow(new PayloadException()).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));
        // Act
        mSubject.updatePayload("1234567890", "1234567890", new CharSequenceX("1234567890")).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertNotCompleted();
        subscriber.assertError(Throwable.class);
    }

    /**
     * Update payload returns a connection failure, Observable should throw {@link ServerConnectionException}
     */
    @Test
    public void initiatePayloadConnectionFail() throws Exception {
        // Arrange
        TestSubscriber<Void> subscriber = new TestSubscriber<>();
        doThrow(new ServerConnectionException()).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));
        // Act
        mSubject.updatePayload("1234567890", "1234567890", new CharSequenceX("1234567890")).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertNotCompleted();
        subscriber.assertError(Throwable.class);
    }

    /**
     * PayloadManager throws exception, should trigger onError
     */
    @Test
    public void initiatePayloadException() throws Exception {
        // Arrange
        TestSubscriber<Void> subscriber = new TestSubscriber<>();
        doThrow(new RuntimeException()).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));
        // Act
        mSubject.updatePayload("1234567890", "1234567890", new CharSequenceX("1234567890")).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertNotCompleted();
        subscriber.assertError(Throwable.class);
    }

    @Test
    public void createCheckEmailTimer() throws Exception {
        // Arrange
        TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        // Act
        mSubject.createCheckEmailTimer().take(1).toBlocking().subscribe(subscriber);
        mSubject.timer = 1;
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
    }

//    @Test
//    public void attemptDecryptPayloadV1Payload() throws Exception {
        // Currently can't be tested in any reasonable way
//    }

    @Test
    public void attemptDecryptPayloadSuccessful() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        doAnswer(invocation -> {
            ((PayloadManager.InitiatePayloadListener) invocation.getArguments()[3]).onSuccess();
            return null;
        }).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenReturn(DECRYPTED_PAYLOAD);
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(mPrefsUtil).setValue(anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(listener).onSuccess();
    }

    @Test
    public void attemptDecryptPayloadNoSharedKey() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        doAnswer(invocation -> {
            ((PayloadManager.InitiatePayloadListener) invocation.getArguments()[3]).onSuccess();
            return null;
        }).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenReturn(DECRYPTED_PAYLOAD_NO_SHARED_KEY);
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(listener).onFatalError();
    }

    @Test
    public void attemptDecryptPayloadInitAuthFail() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        doThrow(new InvalidCredentialsException()).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenReturn(DECRYPTED_PAYLOAD);
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(mPrefsUtil).setValue(anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(listener).onAuthFail();
    }

    @Test
    public void attemptDecryptPayloadFatalError() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        doThrow(new HDWalletException()).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenReturn(DECRYPTED_PAYLOAD);
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(mPrefsUtil).setValue(anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(listener).onFatalError();
    }

    @Test
    public void attemptDecryptPayloadPairFail() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        doThrow(new DecryptionException()).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenReturn(DECRYPTED_PAYLOAD);
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(mPrefsUtil).setValue(anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(listener).onPairFail();
    }

    @Test
    public void attemptDecryptPayloadDecryptionFailed() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenReturn(null);
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(listener).onAuthFail();
    }

    @Test
    public void attemptDecryptPayloadDecryptionThrowsException() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenThrow(mock(RuntimeException.class));
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(listener).onFatalError();
    }

    private static final String TEST_PAYLOAD = "{\n" +
            "  \"payload\": \"test payload\",\n" +
            "  \"pbkdf2_iterations\": 2000\n" +
            "}";

    private static final String DECRYPTED_PAYLOAD = "{\n" +
            "\t\"sharedKey\": \"1234567890\"\n" +
            "}";

    private static final String DECRYPTED_PAYLOAD_NO_SHARED_KEY = "{\n" +
            "\t\"test\": \"1234567890\"\n" +
            "}";

}