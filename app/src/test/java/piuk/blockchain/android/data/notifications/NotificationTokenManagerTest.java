package piuk.blockchain.android.data.notifications;

import com.google.firebase.iid.FirebaseInstanceId;

import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Wallet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.access.AuthEvent;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.util.PrefsUtil;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class NotificationTokenManagerTest extends RxTest{

    private NotificationTokenManager subject;
    @Mock private NotificationService notificationService;
    @Mock private AccessState accessState;
    @Mock private PayloadManager payloadManager;
    @Mock private PrefsUtil prefsUtil;
    @Mock private FirebaseInstanceId firebaseInstanceId;
    @Mock private RxBus rxBus;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new NotificationTokenManager(notificationService, accessState, payloadManager, prefsUtil, firebaseInstanceId, rxBus);
    }

    @Test
    public void storeAndUpdateTokenLoggedIn_enabled() throws Exception {
        // Arrange
        when(accessState.isLoggedIn()).thenReturn(true);
        when(prefsUtil.getValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true)).thenReturn(true);
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
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    public void storeAndUpdateTokenLoggedIn_disabled() throws Exception {
        // Arrange
        when(accessState.isLoggedIn()).thenReturn(true);
        when(prefsUtil.getValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true)).thenReturn(false);
        // Act
        subject.storeAndUpdateToken("token");
        // Assert
        //noinspection ResultOfMethodCallIgnored
        verify(accessState).isLoggedIn();
        verifyNoMoreInteractions(notificationService);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void storeAndUpdateTokenLoggedOut_enabled_stored() throws Exception {
        // Arrange
        when(accessState.isLoggedIn()).thenReturn(false);

        Observable<AuthEvent> authEventObservable = Observable.just(AuthEvent.LOGIN);
        when(rxBus.register(AuthEvent.class)).thenReturn(authEventObservable);

        when(prefsUtil.getValue(PrefsUtil.KEY_FIREBASE_TOKEN, "")).thenReturn("TOKEN");

        when(prefsUtil.getValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true)).thenReturn(true);

        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.getGuid()).thenReturn("guid");
        when(mockPayload.getSharedKey()).thenReturn("sharedKey");
        when(payloadManager.getPayload()).thenReturn(mockPayload);

        when(notificationService.sendNotificationToken(anyString(), anyString(), anyString())).thenReturn(Completable.complete());
        // Act
        subject.storeAndUpdateToken("irrelevant token");
        // Assert
        verify(accessState).isLoggedIn();
        verify(rxBus).register(AuthEvent.class);
        verify(payloadManager).getPayload();
        verify(notificationService).sendNotificationToken(anyString(), anyString(), anyString());
        verifyNoMoreInteractions(notificationService);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void storeAndUpdateTokenLoggedOut_enabled_requestToken() throws Exception {
        // Arrange
        when(accessState.isLoggedIn()).thenReturn(false);

        Observable<AuthEvent> authEventObservable = Observable.just(AuthEvent.LOGIN);
        when(rxBus.register(AuthEvent.class)).thenReturn(authEventObservable);

        when(prefsUtil.getValue(PrefsUtil.KEY_FIREBASE_TOKEN, "")).thenReturn("");

        when(firebaseInstanceId.getToken()).thenReturn("token");

        // Act
        subject.storeAndUpdateToken("irrelevant token");
        // Assert
        verify(accessState).isLoggedIn();
        verify(rxBus).register(AuthEvent.class);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    public void enableNotifications_requestToken() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_FIREBASE_TOKEN, "")).thenReturn("");

        when(firebaseInstanceId.getToken()).thenReturn("token");

        // Act
        subject.enableNotifications();
        // Assert
        verify(prefsUtil).setValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    public void enableNotifications_storedToken() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_FIREBASE_TOKEN, "")).thenReturn("token");
        when(prefsUtil.getValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true)).thenReturn(true);

        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.getGuid()).thenReturn("guid");
        when(mockPayload.getSharedKey()).thenReturn("sharedKey");
        when(payloadManager.getPayload()).thenReturn(mockPayload);

        when(notificationService.sendNotificationToken(anyString(), anyString(), anyString())).thenReturn(Completable.complete());

        // Act
        TestObserver<Void> testObservable = subject.enableNotifications().test();
        // Assert
        testObservable.assertComplete();
        testObservable.assertNoErrors();
        verify(prefsUtil).setValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true);
        verify(payloadManager).getPayload();
        verify(notificationService).sendNotificationToken(anyString(), anyString(), anyString());
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    public void disableNotifications() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_FIREBASE_TOKEN, "")).thenReturn("");

        // Act
        TestObserver<Void> testObservable = subject.disableNotifications().test();
        // Assert
        testObservable.assertComplete();
        testObservable.assertNoErrors();
        verify(prefsUtil).setValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, false);
        verify(firebaseInstanceId).deleteInstanceId();
        verifyNoMoreInteractions(notificationService);
    }
}