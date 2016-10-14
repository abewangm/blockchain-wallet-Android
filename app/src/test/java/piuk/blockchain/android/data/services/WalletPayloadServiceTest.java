package piuk.blockchain.android.data.services;

import info.blockchain.api.WalletPayload;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import piuk.blockchain.android.RxTest;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class WalletPayloadServiceTest extends RxTest {

    private static final String STRING_TO_RETURN = "string_to_return";

    private WalletPayloadService subject;
    @Mock WalletPayload walletPayload;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new WalletPayloadService(walletPayload);
    }

    @Test
    public void getEncryptedPayload() throws Exception {
        // Arrange
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        when(walletPayload.getEncryptedPayload(anyString(), anyString())).thenReturn(STRING_TO_RETURN);
        // Act
        subject.getEncryptedPayload("guid", "1234567890").toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(STRING_TO_RETURN, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void getSessionId() throws Exception {
        // Arrange
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        when(walletPayload.getSessionId(anyString())).thenReturn(STRING_TO_RETURN);
        // Act
        subject.getSessionId("guid").toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(STRING_TO_RETURN, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void getPairingEncryptionPassword() throws Exception {
        // Arrange
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        when(walletPayload.getPairingEncryptionPassword(anyString())).thenReturn(STRING_TO_RETURN);
        // Act
        subject.getPairingEncryptionPassword("guid").toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(STRING_TO_RETURN, subscriber.getOnNextEvents().get(0));
    }

}