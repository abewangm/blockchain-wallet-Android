package piuk.blockchain.android.data.settings;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.settings.SettingsManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import okhttp3.ResponseBody;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.settings.datastore.SettingsDataStore;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SettingsDataManagerTest extends RxTest {

    private SettingsDataManager subject;
    @Mock private SettingsService settingsService;
    @Mock private SettingsDataStore settingsDataStore;
    @Mock private RxBus rxBus;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new SettingsDataManager(settingsService, settingsDataStore, rxBus);
    }

    @Test
    public void initSettings() throws Exception {
        // Arrange
        Settings mockSettings = mock(Settings.class);
        String guid = "GUID";
        String sharedKey = "SHARED_KEY";
        when(settingsService.getSettings()).thenReturn(Observable.just(mockSettings));
        // Act
        TestObserver<Settings> testObserver = subject.initSettings(guid, sharedKey).test();
        // Assert
        verify(settingsService).initSettings(guid, sharedKey);
        verify(settingsService).getSettings();
        verifyNoMoreInteractions(settingsService);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockSettings, testObserver.values().get(0));
    }

    @Test
    public void updateEmail() throws Exception {
        // Arrange
        String email = "EMAIL";
        ResponseBody mockResponse = mock(ResponseBody.class);
        Settings mockSettings = mock(Settings.class);
        when(settingsService.updateEmail(email)).thenReturn(Observable.just(mockResponse));
        when(settingsService.getSettings()).thenReturn(Observable.just(mockSettings));
        // Act
        TestObserver<Settings> testObserver = subject.updateEmail(email).test();
        // Assert
        verify(settingsService).updateEmail(email);
        verify(settingsService).getSettings();
        verifyNoMoreInteractions(settingsService);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockSettings, testObserver.values().get(0));
    }

    @Test
    public void updateSms() throws Exception {
        // Arrange
        String phoneNumber = "PHONE_NUMBER";
        ResponseBody mockResponse = mock(ResponseBody.class);
        Settings mockSettings = mock(Settings.class);
        when(settingsService.updateSms(phoneNumber)).thenReturn(Observable.just(mockResponse));
        when(settingsService.getSettings()).thenReturn(Observable.just(mockSettings));
        // Act
        TestObserver<Settings> testObserver = subject.updateSms(phoneNumber).test();
        // Assert
        verify(settingsService).updateSms(phoneNumber);
        verify(settingsService).getSettings();
        verifyNoMoreInteractions(settingsService);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockSettings, testObserver.values().get(0));
    }

    @Test
    public void verifySms() throws Exception {
        // Arrange
        String verificationCode = "VERIFICATION_CODE";
        ResponseBody mockResponse = mock(ResponseBody.class);
        Settings mockSettings = mock(Settings.class);
        when(settingsService.verifySms(verificationCode)).thenReturn(Observable.just(mockResponse));
        when(settingsService.getSettings()).thenReturn(Observable.just(mockSettings));
        // Act
        TestObserver<Settings> testObserver = subject.verifySms(verificationCode).test();
        // Assert
        verify(settingsService).verifySms(verificationCode);
        verify(settingsService).getSettings();
        verifyNoMoreInteractions(settingsService);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockSettings, testObserver.values().get(0));
    }

    @Test
    public void updateTor() throws Exception {
        // Arrange
        ResponseBody mockResponse = mock(ResponseBody.class);
        Settings mockSettings = mock(Settings.class);
        when(settingsService.updateTor(true)).thenReturn(Observable.just(mockResponse));
        when(settingsService.getSettings()).thenReturn(Observable.just(mockSettings));
        // Act
        TestObserver<Settings> testObserver = subject.updateTor(true).test();
        // Assert
        verify(settingsService).updateTor(true);
        verify(settingsService).getSettings();
        verifyNoMoreInteractions(settingsService);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockSettings, testObserver.values().get(0));
    }

    @Test
    public void updateTwoFactor() throws Exception {
        // Arrange
        ResponseBody mockResponse = mock(ResponseBody.class);
        Settings mockSettings = mock(Settings.class);
        int authType = 1337;
        when(settingsService.updateTwoFactor(authType)).thenReturn(Observable.just(mockResponse));
        when(settingsService.getSettings()).thenReturn(Observable.just(mockSettings));
        // Act
        TestObserver<Settings> testObserver = subject.updateTwoFactor(authType).test();
        // Assert
        verify(settingsService).updateTwoFactor(authType);
        verify(settingsService).getSettings();
        verifyNoMoreInteractions(settingsService);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockSettings, testObserver.values().get(0));
    }

    @Test
    public void enableNotificationNoneRegistered() throws Exception {
        // Arrange
        List<Integer> notifications = Collections.emptyList();
        int notificationType = SettingsManager.NOTIFICATION_TYPE_SMS;
        ResponseBody mockResponse = mock(ResponseBody.class);
        Settings mockSettings = mock(Settings.class);
        when(settingsService.enableNotifications(true)).thenReturn(Observable.just(mockResponse));
        when(settingsService.updateNotifications(notificationType)).thenReturn(Observable.just(mockResponse));
        when(settingsService.getSettings()).thenReturn(Observable.just(mockSettings));
        // Act
        TestObserver<Settings> testObserver = subject.enableNotification(notificationType, notifications).test();
        // Assert
        verify(settingsService).enableNotifications(true);
        verify(settingsService).updateNotifications(notificationType);
        verify(settingsService).getSettings();
        verifyNoMoreInteractions(settingsService);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockSettings, testObserver.values().get(0));
    }

    @Test
    public void enableNotificationEmailRegistered() throws Exception {
        // Arrange
        List<Integer> notifications = Collections.singletonList(SettingsManager.NOTIFICATION_TYPE_EMAIL);
        int notificationType = SettingsManager.NOTIFICATION_TYPE_SMS;
        ResponseBody mockResponse = mock(ResponseBody.class);
        Settings mockSettings = mock(Settings.class);
        when(settingsService.enableNotifications(true)).thenReturn(Observable.just(mockResponse));
        when(settingsService.updateNotifications(SettingsManager.NOTIFICATION_TYPE_ALL))
                .thenReturn(Observable.just(mockResponse));
        when(settingsService.getSettings()).thenReturn(Observable.just(mockSettings));
        // Act
        TestObserver<Settings> testObserver = subject.enableNotification(notificationType, notifications).test();
        // Assert
        verify(settingsService).enableNotifications(true);
        verify(settingsService).updateNotifications(SettingsManager.NOTIFICATION_TYPE_ALL);
        verify(settingsService).getSettings();
        verifyNoMoreInteractions(settingsService);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockSettings, testObserver.values().get(0));
    }

    @Test
    public void enableNotificationSmsRegistered() throws Exception {
        // Arrange
        List<Integer> notifications = Collections.singletonList(SettingsManager.NOTIFICATION_TYPE_SMS);
        int notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL;
        ResponseBody mockResponse = mock(ResponseBody.class);
        Settings mockSettings = mock(Settings.class);
        when(settingsService.enableNotifications(true)).thenReturn(Observable.just(mockResponse));
        when(settingsService.updateNotifications(SettingsManager.NOTIFICATION_TYPE_ALL))
                .thenReturn(Observable.just(mockResponse));
        when(settingsService.getSettings()).thenReturn(Observable.just(mockSettings));
        // Act
        TestObserver<Settings> testObserver = subject.enableNotification(notificationType, notifications).test();
        // Assert
        verify(settingsService).enableNotifications(true);
        verify(settingsService).updateNotifications(SettingsManager.NOTIFICATION_TYPE_ALL);
        verify(settingsService).getSettings();
        verifyNoMoreInteractions(settingsService);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockSettings, testObserver.values().get(0));
    }

    @Test
    public void enableNotificationAllEnabled() throws Exception {
        // Arrange
        List<Integer> notifications = Collections.singletonList(SettingsManager.NOTIFICATION_TYPE_ALL);
        int notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL;
        ResponseBody mockResponse = mock(ResponseBody.class);
        Settings mockSettings = mock(Settings.class);
        when(settingsService.enableNotifications(true)).thenReturn(Observable.just(mockResponse));
        when(settingsService.getSettings()).thenReturn(Observable.just(mockSettings));
        // Act
        TestObserver<Settings> testObserver = subject.enableNotification(notificationType, notifications).test();
        // Assert
        verify(settingsService).enableNotifications(true);
        verify(settingsService).getSettings();
        verifyNoMoreInteractions(settingsService);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockSettings, testObserver.values().get(0));
    }

    @Test
    public void disableNotificationNoneRegistered() throws Exception {
        // Arrange
        List<Integer> notifications = Collections.emptyList();
        int notificationType = SettingsManager.NOTIFICATION_TYPE_SMS;
        Settings mockSettings = mock(Settings.class);
        when(settingsService.getSettings()).thenReturn(Observable.just(mockSettings));
        // Act
        TestObserver<Settings> testObserver = subject.disableNotification(notificationType, notifications).test();
        // Assert
        verify(settingsService).getSettings();
        verifyNoMoreInteractions(settingsService);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockSettings, testObserver.values().get(0));
    }

    @Test
    public void disableNotificationAllRegistered() throws Exception {
        // Arrange
        List<Integer> notifications = Collections.singletonList(SettingsManager.NOTIFICATION_TYPE_ALL);
        int notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL;
        ResponseBody mockResponse = mock(ResponseBody.class);
        Settings mockSettings = mock(Settings.class);
        when(settingsService.updateNotifications(SettingsManager.NOTIFICATION_TYPE_SMS))
                .thenReturn(Observable.just(mockResponse));
        when(settingsService.getSettings()).thenReturn(Observable.just(mockSettings));
        // Act
        TestObserver<Settings> testObserver = subject.disableNotification(notificationType, notifications).test();
        // Assert
        verify(settingsService).updateNotifications(SettingsManager.NOTIFICATION_TYPE_SMS);
        verify(settingsService).getSettings();
        verifyNoMoreInteractions(settingsService);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockSettings, testObserver.values().get(0));
    }

    @Test
    public void disableNotificationBothRegistered() throws Exception {
        // Arrange
        List<Integer> notifications = Arrays.asList(
                SettingsManager.NOTIFICATION_TYPE_EMAIL,
                SettingsManager.NOTIFICATION_TYPE_SMS);
        int notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL;
        ResponseBody mockResponse = mock(ResponseBody.class);
        Settings mockSettings = mock(Settings.class);
        when(settingsService.updateNotifications(SettingsManager.NOTIFICATION_TYPE_SMS))
                .thenReturn(Observable.just(mockResponse));
        when(settingsService.getSettings()).thenReturn(Observable.just(mockSettings));
        // Act
        TestObserver<Settings> testObserver = subject.disableNotification(notificationType, notifications).test();
        // Assert
        verify(settingsService).updateNotifications(SettingsManager.NOTIFICATION_TYPE_SMS);
        verify(settingsService).getSettings();
        verifyNoMoreInteractions(settingsService);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockSettings, testObserver.values().get(0));
    }

    @Test
    public void disableNotificationOneRegisteredMatchesPassed() throws Exception {
        // Arrange
        List<Integer> notifications = Collections.singletonList(SettingsManager.NOTIFICATION_TYPE_EMAIL);
        int notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL;
        ResponseBody mockResponse = mock(ResponseBody.class);
        Settings mockSettings = mock(Settings.class);
        when(settingsService.enableNotifications(false)).thenReturn(Observable.just(mockResponse));
        when(settingsService.updateNotifications(SettingsManager.NOTIFICATION_TYPE_NONE))
                .thenReturn(Observable.just(mockResponse));
        when(settingsService.getSettings()).thenReturn(Observable.just(mockSettings));
        // Act
        TestObserver<Settings> testObserver = subject.disableNotification(notificationType, notifications).test();
        // Assert
        verify(settingsService).enableNotifications(false);
        verify(settingsService).updateNotifications(SettingsManager.NOTIFICATION_TYPE_NONE);
        verify(settingsService).getSettings();
        verifyNoMoreInteractions(settingsService);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockSettings, testObserver.values().get(0));
    }

    @Test
    public void disableNotificationOneRegisteredDoesNotMatchPassed() throws Exception {
        // Arrange
        List<Integer> notifications = Collections.singletonList(SettingsManager.NOTIFICATION_TYPE_SMS);
        int notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL;
        Settings mockSettings = mock(Settings.class);
        when(settingsService.getSettings()).thenReturn(Observable.just(mockSettings));
        // Act
        TestObserver<Settings> testObserver = subject.disableNotification(notificationType, notifications).test();
        // Assert
        verify(settingsService).getSettings();
        verifyNoMoreInteractions(settingsService);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockSettings, testObserver.values().get(0));
    }

    @Test
    public void disableNotificationEdgeCase() throws Exception {
        // Arrange
        List<Integer> notifications = Arrays.asList(1337, 1338);
        int notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL;
        Settings mockSettings = mock(Settings.class);
        when(settingsService.getSettings()).thenReturn(Observable.just(mockSettings));
        // Act
        TestObserver<Settings> testObserver = subject.disableNotification(notificationType, notifications).test();
        // Assert
        verify(settingsService).getSettings();
        verifyNoMoreInteractions(settingsService);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockSettings, testObserver.values().get(0));
    }
}