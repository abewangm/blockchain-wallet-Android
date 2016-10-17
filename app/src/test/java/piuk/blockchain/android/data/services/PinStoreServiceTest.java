package piuk.blockchain.android.data.services;

import info.blockchain.api.PinStore;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import piuk.blockchain.android.RxTest;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class PinStoreServiceTest extends RxTest {

    private PinStoreService subject;
    @Mock PinStore pinStore;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new PinStoreService(pinStore);
    }

    @Test
    public void setAccessKeySuccess() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        when(pinStore.setAccess(anyString(), anyString(), anyString())).thenReturn(new JSONObject(SUCCESS_RESPONSE));
        // Act
        subject.setAccessKey("", "", "").toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(true, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void setAccessKeyFailed() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        when(pinStore.setAccess(anyString(), anyString(), anyString())).thenReturn(new JSONObject());
        // Act
        subject.setAccessKey("", "", "").toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(false, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void validateAccess() throws Exception {
        // Arrange
        TestSubscriber<JSONObject> subscriber = new TestSubscriber<>();
        JSONObject value = new JSONObject(SUCCESS_RESPONSE);
        when(pinStore.validateAccess(anyString(), anyString())).thenReturn(value);
        // Act
        subject.validateAccess("", "").toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(value, subscriber.getOnNextEvents().get(0));
    }

    private static final String SUCCESS_RESPONSE = "{\n" +
            "  \"success\": true\n" +
            "}";

}