package piuk.blockchain.android.data.services;

import info.blockchain.api.Notifications;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import piuk.blockchain.android.RxTest;
import rx.observers.TestSubscriber;

public class NotificationServiceTest extends RxTest {

    private NotificationService subject;
    @Mock Notifications notifications;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new NotificationService(notifications);
    }

    @Test
    public void sendNotificationToken() throws Exception {
        // Arrange
        TestSubscriber<Void> subscriber = new TestSubscriber<>();
        // Act
        subject.sendNotificationToken("", "", "").subscribe(subscriber);
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
    }

}