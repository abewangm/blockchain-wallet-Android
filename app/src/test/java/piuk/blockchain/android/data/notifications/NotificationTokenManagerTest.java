package piuk.blockchain.android.data.notifications;

import info.blockchain.wallet.payload.PayloadManager;

import info.blockchain.wallet.payload.data.Wallet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import io.reactivex.Completable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.access.AccessState.AuthEvent;
import piuk.blockchain.android.data.services.NotificationService;
import piuk.blockchain.android.util.PrefsUtil;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class NotificationTokenManagerTest {

    private NotificationTokenManager subject;
    @Mock private NotificationService notificationService;
    @Mock private AccessState accessState;
    @Mock private PayloadManager payloadManager;
    @Mock private PrefsUtil prefsUtil;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        subject = new NotificationTokenManager(notificationService, accessState, payloadManager, prefsUtil);
    }

    @Test
    public void storeAndUpdateTokenLoggedIn() throws Exception {
        // Arrange
        when(accessState.isLoggedIn()).thenReturn(true);
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.getGuid()).thenReturn("guid");
        when(mockPayload.getSharedKey()).thenReturn("sharedKey");
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        when(notificationService.sendNotificationToken(anyString(), anyString(), anyString())).thenReturn(Completable.complete());
        // Act
        subject.storeAndUpdateToken("token");
        // Assert
        //noinspection ResultOfMethodCallIgnored
        verify(accessState).isLoggedIn();
        verify(payloadManager).getPayload();
        verify(notificationService).sendNotificationToken(anyString(), anyString(), anyString());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void storeAndUpdateTokenLoggedOut() throws Exception {
        // Arrange
        when(accessState.isLoggedIn()).thenReturn(false);
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.getGuid()).thenReturn("guid");
        when(mockPayload.getSharedKey()).thenReturn("sharedKey");
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        Subject<AuthEvent> testSubject = PublishSubject.create();
        when(accessState.getAuthEventSubject()).thenReturn(testSubject);
        when(notificationService.sendNotificationToken(anyString(), anyString(), anyString())).thenReturn(Completable.complete());
        // Act
        subject.storeAndUpdateToken("token");
        testSubject.onNext(AuthEvent.LOGIN);
        // Assert
        verify(accessState).isLoggedIn();
        verify(accessState).getAuthEventSubject();
        verify(payloadManager).getPayload();
        verify(notificationService).sendNotificationToken(anyString(), anyString(), anyString());
    }

}