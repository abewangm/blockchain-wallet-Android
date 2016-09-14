package info.blockchain.wallet.datamanagers;

import info.blockchain.api.WalletPayload;
import info.blockchain.wallet.access.AccessState;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.exceptions.PayloadException;
import info.blockchain.wallet.exceptions.ServerConnectionException;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.AESUtilWrapper;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.TimeUnit;

import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.RxTest;
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
@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class AuthDataManagerTest extends RxTest {

    private static final String STRING_TO_RETURN = "string_to_return";

    @Mock private PayloadManager mPayloadManager;
    @Mock private PrefsUtil mPrefsUtil;
    @Mock private WalletPayload mAccess;
    @Mock private AppUtil mAppUtil;
    @Mock private AESUtilWrapper mAesUtils;
    @Mock private AccessState mAccessState;
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
        when(mAccess.getEncryptedPayload(anyString(), anyString())).thenReturn(STRING_TO_RETURN);
        // Act
        mSubject.getEncryptedPayload("1234567890", "1234567890").toBlocking().subscribe(subscriber);
        // Assert
        verify(mAccess).getEncryptedPayload(anyString(), anyString());
        subscriber.assertCompleted();
        subscriber.onNext(STRING_TO_RETURN);
        subscriber.assertNoErrors();
    }

    @Test
    public void getSessionId() throws Exception {
        // Arrange
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        when(mAccess.getEncryptedPayload(anyString(), anyString())).thenReturn(STRING_TO_RETURN);
        // Act
        mSubject.getSessionId("1234567890").toBlocking().subscribe(subscriber);
        // Assert
        verify(mAccess).getSessionId(anyString());
        subscriber.assertCompleted();
        subscriber.onNext(STRING_TO_RETURN);
        subscriber.assertNoErrors();
    }

    @Test
    public void validatePin() throws Exception {
        // Arrange
        TestSubscriber<CharSequenceX> subscriber = new TestSubscriber<>();
        CharSequenceX charSequenceX = new CharSequenceX("1234567890");
        when(mAccessState.validatePIN(anyString())).thenReturn(charSequenceX);
        // Act
        mSubject.validatePin(anyString()).toBlocking().subscribe(subscriber);
        // Assert
        verify(mAccessState).validatePIN(anyString());
        subscriber.assertCompleted();
        subscriber.onNext(charSequenceX);
        subscriber.assertNoErrors();
    }

    @Test
    public void createPin() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        when(mAccessState.createPIN(any(CharSequenceX.class), anyString())).thenReturn(true);
        // Act
        mSubject.createPin(any(CharSequenceX.class), anyString()).toBlocking().subscribe(subscriber);
        // Assert
        verify(mAccessState).createPIN(any(CharSequenceX.class), anyString());
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
        when(mAccess.getSessionId(anyString())).thenReturn(STRING_TO_RETURN);
        when(mAccess.getEncryptedPayload(anyString(), anyString())).thenReturn(STRING_TO_RETURN);
        // Act
        mSubject.startPollingAuthStatus("1234567890").toBlocking().subscribe(subscriber);
        // Assert
        verify(mAccess).getSessionId(anyString());
        verify(mAccess).getEncryptedPayload(anyString(), anyString());
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
        when(mAccess.getSessionId(anyString())).thenReturn(STRING_TO_RETURN);
        when(mAccess.getEncryptedPayload(anyString(), anyString())).thenThrow(mock(RuntimeException.class));
        // Act
        mSubject.startPollingAuthStatus("1234567890").toBlocking().subscribe(subscriber);
        // Assert
        verify(mAccess).getSessionId(anyString());
        verify(mAccess).getEncryptedPayload(anyString(), anyString());
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
        when(mAccess.getSessionId(anyString())).thenReturn(STRING_TO_RETURN);
        when(mAccess.getEncryptedPayload(anyString(), anyString())).thenReturn(WalletPayload.KEY_AUTH_REQUIRED);
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

    private static final String V1_PAYLOAD_ENCRYPTED = "4gen70mZdnKx0YRnjpPSHTqz2UOMiEUGev9ZaU0L5daWQpqCBqZUgCIqEEbyNzmBQk6DhaQsReAOkzJz4L8q8VMeKk+/h5JXIQTqxOwz0M470ZiyGnmKe7DFtEQGft3oAQAvc/SA89gdu/50SASEQk6fQPyRigkPunIrjmnxzWuO+Mak040Lea3qoScJXBY3xZG4C4ukJaFFhcZUTi+e45JYAg7AtmyONFFdcVLNpGjwRTkDvpAPobGJqMfOfFLHkdxNos+51khXYuyxEp3grU8jvwZbl2pCVgC1Z50IWFSUvSaZGvKkZaK5Ohw0Tn7RF4T4oUA4IrRYGpHY2F8yLUpcSJ47ctC90UPT7GITpDHA/eQNpzdhtIv7Inkkza/Okd5blx+59he+x/AQFTdyc5YZmsEgN+g/RUN06UNibe78iyqEEN4q88RiLDAwFMoHY4cYtzyd25CSza0NP+yBFFLf+NbKA64Ck2pItMgr4JXkCVU8shQmtKnVKfO8mC3MQSF+kZE0mClr7URa8LIbMhmGgQ3o2vGNbRiutzdO7/L52F7GWTHJzKEo5FdWK2K218Spd0L31TpBn3aKXg6BLgw6WiggztP4hP4pVf6LK21KvgPKf3NegF4Or7wfDz1/mP4uiz+xf18trCuFHoityhXduwrrtA9bhhf2SwLE4+Md9R+m1KOQ6q63ynujM0oeIGVj6NIH4uJS6pz/3iIKZTkr/4/5TdEw8uBlKHHxJHBHBuHPsYN3bpd4A+VMiNn3EPV02aD7xCq6anFPvoYbj8BHW0p6kUHP32fcuETciO7NF0NLGSIoE3iTNHRSpXt58BEHIU/p4Tx4KczwLfUiVvARzjnkF8KQMPrgzhZysiXhH8cwdw3LPU1hi0zDuyW3RR7UQxuCTOa/T8v0ZnGZhFNIFkocwSvcDStef807vg6bu2Fe4BewCLzHLQDEudTUHxiFsWWfzo4E4/3pCTNgAkI4N0swvUR7aBTQHminudxrdSxC2f/6pgBnd4TqBv+W16qcWLf31x/yV1wHFyG2rfTpbpTx07W4r3G4zoDq8XHecpfuDy2H+GKgctSyUSDO9BTb6Q93+JyrMDvuw2o4ennAmll8gQ8C5WjxsQ78oES2//yeCqTgKQiEx121Dwa2/MiposMiRg82pwMXZ7lERx74CQnII54z4YAHGA2EqtQwwNdzdCufJqGV8oHaEPAXRNnGuAK6fbDUlM10GiSQoCpvzmDtv5n3RYlXXgmXAjmDb1CodLEFqCps/lPyAvav";



}