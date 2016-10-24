package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.data.SpendableUnspentOutputs;
import info.blockchain.wallet.payment.data.SuggestedFee;
import info.blockchain.wallet.payment.data.UnspentOutputs;

import org.bitcoinj.core.ECKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.List;

import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.services.PaymentService;
import piuk.blockchain.android.data.services.UnspentService;
import piuk.blockchain.android.ui.send.PendingTransaction;
import rx.Observable;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AccountEditDataManagerTest extends RxTest {

    private AccountEditDataManager subject;
    @Mock PayloadManager payloadManager;
    @Mock UnspentService unspentService;
    @Mock PaymentService paymentService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new AccountEditDataManager(unspentService, paymentService, payloadManager);
    }

    @Test
    public void getPendingTransactionForLegacyAddress() throws Exception {
        // Arrange
        TestSubscriber<PendingTransaction> subscriber = new TestSubscriber<>();
        LegacyAddress legacyAddress = new LegacyAddress();
        Payment payment = new Payment();
        SuggestedFee suggestedFee = new SuggestedFee();
        suggestedFee.defaultFeePerKb = BigInteger.valueOf(100);
        DynamicFeeCache.getInstance().setSuggestedFee(suggestedFee);
        Payload mockPayload = mock(Payload.class, RETURNS_DEEP_STUBS);
        when(mockPayload.getHdWallet().getDefaultIndex()).thenReturn(0);
        when(mockPayload.getHdWallet().getAccounts().get(anyInt())).thenReturn(mock(Account.class));
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        when(payloadManager.getNextReceiveAddress(anyInt())).thenReturn("address");
        when(unspentService.getUnspentOutputs(anyString(), any(Payment.class))).thenReturn(Observable.just(mock(UnspentOutputs.class)));
        // Act
        subject.getPendingTransactionForLegacyAddress(legacyAddress, payment).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(PendingTransaction.class, subscriber.getOnNextEvents().get(0).getClass());
    }

    @Test
    public void submitPayment() throws Exception {
        // Arrange
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        when(paymentService.submitPayment(
                any(SpendableUnspentOutputs.class),
                anyListOf(ECKey.class),
                anyString(),
                anyString(),
                any(BigInteger.class),
                any(BigInteger.class))).thenReturn(Observable.just("hash"));
        // Act
        subject.submitPayment(mock(
                SpendableUnspentOutputs.class),
                mock(List.class),
                "",
                "",
                mock(BigInteger.class),
                mock(BigInteger.class)).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals("hash", subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void syncPayloadWithServer() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        when(payloadManager.savePayloadToServer()).thenReturn(true);
        // Act
        subject.syncPayloadWithServer().toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(true, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void updateBalancesAndTransactions() throws Exception {
        // Arrange
        TestSubscriber subscriber = new TestSubscriber();
        // Act
        subject.updateBalancesAndTransactions().subscribe(subscriber);
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
    }

}