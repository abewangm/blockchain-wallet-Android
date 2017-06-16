package piuk.blockchain.android.data.settings;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.settings.SettingsManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import okhttp3.ResponseBody;
import piuk.blockchain.android.RxTest;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SettingsServiceTest extends RxTest {

    private SettingsService subject;
    @Mock private SettingsManager settingsManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new SettingsService(settingsManager);
    }

    @Test
    public void initSettings() throws Exception {
        // Arrange
        String guid = "GUID";
        String sharedKey = "SHARED_KEY";
        // Act
        subject.initSettings(guid, sharedKey);
        // Assert
        verify(settingsManager).initSettings(guid, sharedKey);
    }

    @Test
    public void getSettings() throws Exception {
        // Arrange
        Settings mockSettings = mock(Settings.class);
        when(settingsManager.getInfo()).thenReturn(Observable.just(mockSettings));
        // Act
        TestObserver<Settings> testObserver = subject.getSettings().test();
        // Assert
        verify(settingsManager).getInfo();
        verifyNoMoreInteractions(settingsManager);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockSettings, testObserver.values().get(0));
    }

    @Test
    public void updateEmail() throws Exception {
        // Arrange
        ResponseBody mockResponse = mock(ResponseBody.class);
        String email = "EMAIL";
        when(settingsManager.updateSetting(SettingsManager.METHOD_UPDATE_EMAIL, email))
                .thenReturn(Observable.just(mockResponse));
        // Act
        TestObserver<ResponseBody> testObserver = subject.updateEmail(email).test();
        // Assert
        verify(settingsManager).updateSetting(SettingsManager.METHOD_UPDATE_EMAIL, email);
        verifyNoMoreInteractions(settingsManager);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockResponse, testObserver.values().get(0));
    }

    @Test
    public void updateSms() throws Exception {
        // Arrange
        ResponseBody mockResponse = mock(ResponseBody.class);
        String phoneNumber = "PHONE_NUMBER";
        when(settingsManager.updateSetting(SettingsManager.METHOD_UPDATE_SMS, phoneNumber))
                .thenReturn(Observable.just(mockResponse));
        // Act
        TestObserver<ResponseBody> testObserver = subject.updateSms(phoneNumber).test();
        // Assert
        verify(settingsManager).updateSetting(SettingsManager.METHOD_UPDATE_SMS, phoneNumber);
        verifyNoMoreInteractions(settingsManager);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockResponse, testObserver.values().get(0));
    }

    @Test
    public void verifySms() throws Exception {
        // Arrange
        ResponseBody mockResponse = mock(ResponseBody.class);
        String verificationCode = "VERIFICATION_CODE";
        when(settingsManager.updateSetting(SettingsManager.METHOD_VERIFY_SMS, verificationCode))
                .thenReturn(Observable.just(mockResponse));
        // Act
        TestObserver<ResponseBody> testObserver = subject.verifySms(verificationCode).test();
        // Assert
        verify(settingsManager).updateSetting(SettingsManager.METHOD_VERIFY_SMS, verificationCode);
        verifyNoMoreInteractions(settingsManager);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockResponse, testObserver.values().get(0));
    }

    @Test
    public void updateTor() throws Exception {
        // Arrange
        ResponseBody mockResponse = mock(ResponseBody.class);
        when(settingsManager.updateSetting(SettingsManager.METHOD_UPDATE_BLOCK_TOR_IPS, 1))
                .thenReturn(Observable.just(mockResponse));
        // Act
        TestObserver<ResponseBody> testObserver = subject.updateTor(true).test();
        // Assert
        verify(settingsManager).updateSetting(SettingsManager.METHOD_UPDATE_BLOCK_TOR_IPS, 1);
        verifyNoMoreInteractions(settingsManager);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockResponse, testObserver.values().get(0));
    }

    @Test
    public void updateNotifications() throws Exception {
        // Arrange
        ResponseBody mockResponse = mock(ResponseBody.class);
        int notificationType = 1337;
        when(settingsManager.updateSetting(SettingsManager.METHOD_UPDATE_NOTIFICATION_TYPE, notificationType))
                .thenReturn(Observable.just(mockResponse));
        // Act
        TestObserver<ResponseBody> testObserver = subject.updateNotifications(notificationType).test();
        // Assert
        verify(settingsManager).updateSetting(SettingsManager.METHOD_UPDATE_NOTIFICATION_TYPE, notificationType);
        verifyNoMoreInteractions(settingsManager);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockResponse, testObserver.values().get(0));
    }

    @Test
    public void enableNotifications() throws Exception {
        // Arrange
        ResponseBody mockResponse = mock(ResponseBody.class);
        when(settingsManager.updateSetting(SettingsManager.METHOD_UPDATE_NOTIFICATION_ON, SettingsManager.NOTIFICATION_ON))
                .thenReturn(Observable.just(mockResponse));
        // Act
        TestObserver<ResponseBody> testObserver = subject.enableNotifications(true).test();
        // Assert
        verify(settingsManager).updateSetting(SettingsManager.METHOD_UPDATE_NOTIFICATION_ON, SettingsManager.NOTIFICATION_ON);
        verifyNoMoreInteractions(settingsManager);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockResponse, testObserver.values().get(0));
    }

    @Test
    public void updateTwoFactor() throws Exception {
        // Arrange
        ResponseBody mockResponse = mock(ResponseBody.class);
        int authType = 1337;
        when(settingsManager.updateSetting(SettingsManager.METHOD_UPDATE_AUTH_TYPE, authType))
                .thenReturn(Observable.just(mockResponse));
        // Act
        TestObserver<ResponseBody> testObserver = subject.updateTwoFactor(authType).test();
        // Assert
        verify(settingsManager).updateSetting(SettingsManager.METHOD_UPDATE_AUTH_TYPE, authType);
        verifyNoMoreInteractions(settingsManager);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockResponse, testObserver.values().get(0));
    }

}