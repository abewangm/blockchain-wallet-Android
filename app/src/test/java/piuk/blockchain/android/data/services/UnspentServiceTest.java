package piuk.blockchain.android.data.services;

import info.blockchain.api.Unspent;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.data.UnspentOutputs;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import piuk.blockchain.android.RxTest;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnspentServiceTest extends RxTest {

    private UnspentService subject;
    @Mock Unspent unspent;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new UnspentService(unspent);
    }

    @Test
    public void getUnspentOutputsSuccess() throws Exception {
        // Arrange
        TestSubscriber<UnspentOutputs> subscriber = new TestSubscriber<>();
        when(unspent.getUnspentOutputs(anyString())).thenReturn(new JSONObject());
        Payment mockPayment = mock(Payment.class);
        UnspentOutputs mockOutputs = mock(UnspentOutputs.class);
        when(mockPayment.getCoins(any(JSONObject.class))).thenReturn(mockOutputs);
        // Act
        subject.getUnspentOutputs("legacy", mockPayment).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(mockOutputs, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void getUnspentOutputsFailure() throws Exception {
        // Arrange
        TestSubscriber<UnspentOutputs> subscriber = new TestSubscriber<>();
        when(unspent.getUnspentOutputs(anyString())).thenReturn(null);
        Payment mockPayment = mock(Payment.class);
        // Act
        subject.getUnspentOutputs("legacy", mockPayment).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertNotCompleted();
        subscriber.assertNoValues();
        subscriber.assertError(Throwable.class);
    }

}