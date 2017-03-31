package piuk.blockchain.android.data.datamanagers;

import info.blockchain.api.data.UnspentOutputs;
import info.blockchain.wallet.api.data.Fee;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payment.SpendableUnspentOutputs;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.Collections;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.ui.send.PendingTransaction;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class AccountEditDataManagerTest extends RxTest {

    private AccountEditDataManager subject;
    @Mock private PayloadDataManager payloadDataManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private DynamicFeeCache dynamicFeeCache;
    @Mock private SendDataManager sendDataManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new AccountEditDataManager(payloadDataManager, sendDataManager, dynamicFeeCache);
    }

    @Test
    public void getPendingTransactionForLegacyAddress() throws Exception {
        // Arrange
        LegacyAddress legacyAddress = new LegacyAddress();
        legacyAddress.setAddress("");
        Fee suggestedFee = mock(Fee.class);
        Pair<BigInteger, BigInteger> sweepableCoins = Pair.of(BigInteger.ONE, BigInteger.TEN);
        when(suggestedFee.getFee()).thenReturn(100.0d);
        when(dynamicFeeCache.getCachedDynamicFee().getDefaultFee()).thenReturn(suggestedFee);
        when(payloadDataManager.getDefaultAccount()).thenReturn(mock(Account.class));
        when(payloadDataManager.getNextReceiveAddress(any(Account.class)))
                .thenReturn(Observable.just("address"));
        when(sendDataManager.getUnspentOutputs(anyString()))
                .thenReturn(Observable.just(mock(UnspentOutputs.class)));
        when(sendDataManager.getSweepableCoins(any(UnspentOutputs.class), any(BigInteger.class)))
                .thenReturn(sweepableCoins);
        when(sendDataManager.getSpendableCoins(any(UnspentOutputs.class), any(BigInteger.class), any(BigInteger.class)))
                .thenReturn(mock(SpendableUnspentOutputs.class));
        // Act
        TestObserver<PendingTransaction> testObserver =
                subject.getPendingTransactionForLegacyAddress(legacyAddress).test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(PendingTransaction.class, testObserver.values().get(0).getClass());
    }

    @Test
    public void submitPayment() throws Exception {
        // Arrange
        when(sendDataManager.submitPayment(
                any(SpendableUnspentOutputs.class),
                anyList(),
                anyString(),
                anyString(),
                any(BigInteger.class),
                any(BigInteger.class))).thenReturn(Observable.just("hash"));
        // Act
        TestObserver<String> testObserver = subject.submitPayment(mock(
                SpendableUnspentOutputs.class),
                Collections.emptyList(),
                "",
                "",
                mock(BigInteger.class),
                mock(BigInteger.class)).test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue("hash");
    }

}