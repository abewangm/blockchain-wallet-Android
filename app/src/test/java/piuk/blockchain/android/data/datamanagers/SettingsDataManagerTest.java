package piuk.blockchain.android.data.datamanagers;

import info.blockchain.api.Settings;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.services.SettingsService;
import rx.Observable;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SettingsDataManagerTest extends RxTest {

    private SettingsDataManager subject;
    @Mock SettingsService settingsService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new SettingsDataManager(settingsService);
    }

    @Test
    public void updateSettings() throws Exception {
        // Arrange
        TestSubscriber<Settings> subscriber = new TestSubscriber<>();
        Settings settings = new Settings();
        when(settingsService.updateSettings(anyString(), anyString())).thenReturn(Observable.just(settings));
        // Act
        subject.updateSettings("guid", "sharedKey").toBlocking().subscribe(subscriber);
        // Assert
        verify(settingsService).updateSettings(anyString(), anyString());
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(settings, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void updateEmail() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        when(settingsService.updateEmail(anyString())).thenReturn(Observable.just(true));
        // Act
        subject.updateEmail("email").toBlocking().subscribe(subscriber);
        // Assert
        verify(settingsService).updateEmail(anyString());
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(true, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void updateSms() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        when(settingsService.updateSms(anyString())).thenReturn(Observable.just(true));
        // Act
        subject.updateSms("sms").toBlocking().subscribe(subscriber);
        // Assert
        verify(settingsService).updateSms(anyString());
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(true, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void verifySms() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        when(settingsService.verifySms(anyString())).thenReturn(Observable.just(true));
        // Act
        subject.verifySms("code").toBlocking().subscribe(subscriber);
        // Assert
        verify(settingsService).verifySms(anyString());
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(true, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void updateTor() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        when(settingsService.updateTor(anyBoolean())).thenReturn(Observable.just(true));
        // Act
        subject.updateTor(true).toBlocking().subscribe(subscriber);
        // Assert
        verify(settingsService).updateTor(anyBoolean());
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(true, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void updatePasswordHint() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        when(settingsService.updatePasswordHint(anyString())).thenReturn(Observable.just(true));
        // Act
        subject.updatePasswordHint("hint").toBlocking().subscribe(subscriber);
        // Assert
        verify(settingsService).updatePasswordHint(anyString());
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(true, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void updateNotificationsEnabled() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        when(settingsService.enableNotifications(anyInt())).thenReturn(Observable.just(true));
        // Act
        subject.updateNotifications(0, true).toBlocking().subscribe(subscriber);
        // Assert
        verify(settingsService).enableNotifications(anyInt());
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(true, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void updateNotificationsDisabled() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        when(settingsService.disableNotifications(anyInt())).thenReturn(Observable.just(true));
        // Act
        subject.updateNotifications(0, false).toBlocking().subscribe(subscriber);
        // Assert
        verify(settingsService).disableNotifications(anyInt());
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(true, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void updateTwoFactor() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        when(settingsService.updateTwoFactor(anyInt())).thenReturn(Observable.just(true));
        // Act
        subject.updateTwoFactor(0).toBlocking().subscribe(subscriber);
        // Assert
        verify(settingsService).updateTwoFactor(anyInt());
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(true, subscriber.getOnNextEvents().get(0));
    }

}