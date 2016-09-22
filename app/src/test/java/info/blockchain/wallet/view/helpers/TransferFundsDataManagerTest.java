package info.blockchain.wallet.view.helpers;

import android.support.v4.util.Pair;

import info.blockchain.api.Unspent;
import info.blockchain.wallet.cache.DynamicFeeCache;
import info.blockchain.wallet.model.ItemAccount;
import info.blockchain.wallet.model.PendingTransaction;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.HDWallet;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.data.SpendableUnspentOutputs;
import info.blockchain.wallet.payment.data.SuggestedFee;
import info.blockchain.wallet.payment.data.SweepBundle;
import info.blockchain.wallet.payment.data.UnspentOutputs;
import info.blockchain.wallet.util.CharSequenceX;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import piuk.blockchain.android.RxTest;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransferFundsDataManagerTest extends RxTest {

    private TransferFundsDataManager mSubject;
    @Mock PayloadManager mPayloadManager;
    @Mock Unspent mUnspentApi;
    @Mock Payment mPayment;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        mSubject = new TransferFundsDataManager(mPayloadManager, mUnspentApi, mPayment);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        Mockito.validateMockitoUsage();
    }

    @Test
    public void getTransferableFundTransactionListForDefaultAccount() throws Exception {
        // Arrange
        TestSubscriber<Map<List<PendingTransaction>, Pair<Long, Long>>> subscriber = new TestSubscriber<>();
        Payload mockPayload = mock(Payload.class);
        LegacyAddress legacyAddress1 = new LegacyAddress();
        legacyAddress1.setAddress("address");
        List<LegacyAddress> legacyAddresses = new ArrayList<LegacyAddress>() {{
           add(legacyAddress1);
           add(legacyAddress1);
           add(legacyAddress1);
        }};
        SuggestedFee suggestedFee = new SuggestedFee();
        suggestedFee.defaultFeePerKb = new BigInteger("100");
        DynamicFeeCache.getInstance().setSuggestedFee(suggestedFee);
        MultiAddrFactory.getInstance().setLegacyBalance("address", 1000000L);
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        HDWallet mockHdWallet = mock(HDWallet.class);
        when(mockPayload.getHdWallet()).thenReturn(mockHdWallet);
        when(mockHdWallet.getDefaultIndex()).thenReturn(0);
        when(mockPayload.getLegacyAddresses()).thenReturn(legacyAddresses);
        when(mUnspentApi.getUnspentOutputs(anyString())).thenReturn(mock(JSONObject.class));
        UnspentOutputs mockUnspentOutputs = mock(UnspentOutputs.class);
        when(mPayment.getCoins(any(JSONObject.class))).thenReturn(mockUnspentOutputs);
        SpendableUnspentOutputs mockSpendableUnspentOutputs = mock(SpendableUnspentOutputs.class);
        when(mPayment.getSpendableCoins(any(UnspentOutputs.class), any(BigInteger.class), any(BigInteger.class))).thenReturn(mockSpendableUnspentOutputs);
        when(mockSpendableUnspentOutputs.getAbsoluteFee()).thenReturn(new BigInteger("10"));
        SweepBundle mockSweepBundle = mock(SweepBundle.class);
        when(mockSweepBundle.getSweepAmount()).thenReturn(new BigInteger("5460"));
        when(mPayment.getSweepBundle(any(UnspentOutputs.class), any(BigInteger.class))).thenReturn(mockSweepBundle);
        // Act
        mSubject.getTransferableFundTransactionListForDefaultAccount().toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
    }

        @Test
    public void sendPaymentSuccess() throws Exception {
        // Arrange
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        Payment mockPayment = mock(Payment.class);
        doAnswer(invocation -> {
            ((Payment.SubmitPaymentListener) invocation.getArguments()[10]).onSuccess("hash");
            return null;
        }).when(mockPayment).submitPayment(
                any(SpendableUnspentOutputs.class),
                any(Account.class),
                any(LegacyAddress.class),
                anyString(),
                anyString(),
                anyString(),
                any(BigInteger.class),
                any(BigInteger.class),
                anyBoolean(),
                anyString(),
                any(Payment.SubmitPaymentListener.class));

        PendingTransaction transaction1 = new PendingTransaction();
        transaction1.sendingObject = new ItemAccount("", "", null, null);
        transaction1.sendingObject.accountObject = new LegacyAddress();
        transaction1.bigIntAmount = new BigInteger("1000000");
        transaction1.bigIntFee = new BigInteger("100");

        List<PendingTransaction> pendingTransactions = new ArrayList<PendingTransaction>() {{
            add(transaction1);
            add(transaction1);
            add(transaction1);
        }};
        when(mPayloadManager.savePayloadToServer()).thenReturn(true);

        // Act
        mSubject.sendPayment(mockPayment, pendingTransactions, new CharSequenceX("password")).toBlocking().subscribe(subscriber);
        // Assert
        assertEquals("hash", subscriber.getOnNextEvents().get(0));
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
    }

    @Test
    public void sendPaymentFailed() throws Exception {
        // Arrange
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        Payment mockPayment = mock(Payment.class);
        doAnswer(invocation -> {
            ((Payment.SubmitPaymentListener) invocation.getArguments()[10]).onFail("failed");
            return null;
        }).when(mockPayment).submitPayment(
                any(SpendableUnspentOutputs.class),
                any(Account.class),
                any(LegacyAddress.class),
                anyString(),
                anyString(),
                anyString(),
                any(BigInteger.class),
                any(BigInteger.class),
                anyBoolean(),
                anyString(),
                any(Payment.SubmitPaymentListener.class));

        PendingTransaction transaction1 = new PendingTransaction();
        transaction1.sendingObject = new ItemAccount("", "", null, null);
        transaction1.sendingObject.accountObject = new LegacyAddress();

        List<PendingTransaction> pendingTransactions = new ArrayList<PendingTransaction>() {{
            add(transaction1);
            add(transaction1);
            add(transaction1);
        }};
        when(mPayloadManager.savePayloadToServer()).thenReturn(true);

        // Act
        mSubject.sendPayment(mockPayment, pendingTransactions, new CharSequenceX("password")).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertError(Throwable.class);
        subscriber.assertNotCompleted();
        subscriber.assertNoValues();
    }

    @Test
    public void sendPaymentException() throws Exception {
        // Arrange
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        Payment mockPayment = mock(Payment.class);
        doThrow(new NullPointerException())
                .when(mockPayment)
                .submitPayment(
                        any(SpendableUnspentOutputs.class),
                        any(Account.class),
                        any(LegacyAddress.class),
                        anyString(),
                        anyString(),
                        anyString(),
                        any(BigInteger.class),
                        any(BigInteger.class),
                        anyBoolean(),
                        anyString(),
                        any(Payment.SubmitPaymentListener.class));

        PendingTransaction transaction1 = new PendingTransaction();
        transaction1.sendingObject = new ItemAccount("", "", null, null);
        transaction1.sendingObject.accountObject = new LegacyAddress();

        List<PendingTransaction> pendingTransactions = new ArrayList<PendingTransaction>() {{
            add(transaction1);
            add(transaction1);
            add(transaction1);
        }};
        when(mPayloadManager.savePayloadToServer()).thenReturn(true);

        // Act
        mSubject.sendPayment(mockPayment, pendingTransactions, new CharSequenceX("password")).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertError(Throwable.class);
        subscriber.assertNotCompleted();
        subscriber.assertNoValues();
    }

    @Test
    public void savePayloadToServer() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        when(mPayloadManager.savePayloadToServer()).thenReturn(true);
        // Act
        mSubject.savePayloadToServer().toBlocking().subscribe(subscriber);
        // Assert
        assertEquals(true, subscriber.getOnNextEvents().get(0));
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
    }

}