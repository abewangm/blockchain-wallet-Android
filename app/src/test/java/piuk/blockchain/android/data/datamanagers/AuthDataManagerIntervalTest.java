package piuk.blockchain.android.data.datamanagers;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import piuk.blockchain.android.RxIntervalTest;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.services.WalletService;
import piuk.blockchain.android.ui.transactions.PayloadDataManager;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;
import retrofit2.Response;

import static com.nhaarman.mockito_kotlin.MockitoKt.verify;
import static org.mockito.Mockito.when;


public class AuthDataManagerIntervalTest extends RxIntervalTest {

    private static final String ERROR_BODY = "{\n" +
            "\t\"message\": \"Authorization Required\"\n" +
            "}";

    @Mock private PayloadDataManager payloadDataManager;
    @Mock private PrefsUtil prefsUtil;
    @Mock private WalletService walletService;
    @Mock private AppUtil appUtil;
    @Mock private AccessState accessState;
    @Mock private StringUtils stringUtils;
    @InjectMocks AuthDataManager subject;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        testScheduler = new TestScheduler();
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
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS);
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
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
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
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(1);
    }

}
