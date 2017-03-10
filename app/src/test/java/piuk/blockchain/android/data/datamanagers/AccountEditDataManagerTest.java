package piuk.blockchain.android.data.datamanagers;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.services.PaymentService;
import piuk.blockchain.android.ui.transactions.PayloadDataManager;

public class AccountEditDataManagerTest extends RxTest {

    private AccountEditDataManager subject;
    @Mock PayloadDataManager payloadDataManager;
//    @Mock UnspentService unspentService;
    @Mock PaymentService paymentService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

//        subject = new AccountEditDataManager(unspentService, paymentService, payloadDataManager);
    }

//    @Test
//    public void getPendingTransactionForLegacyAddress() throws Exception {
//        // Arrange
//        LegacyAddress legacyAddress = new LegacyAddress();
//        legacyAddress.setAddress("");
//        Payment payment = new Payment();
//        SuggestedFee suggestedFee = new SuggestedFee();
//        suggestedFee.defaultFeePerKb = BigInteger.valueOf(100);
//        DynamicFeeCache.getInstance().setCachedDynamicFee(suggestedFee);
//        Payload mockPayload = mock(Payload.class, RETURNS_DEEP_STUBS);
//        when(mockPayload.getHdWallet().getDefaultIndex()).thenReturn(0);
//        when(mockPayload.getHdWallet().getAccounts().get(anyInt())).thenReturn(mock(Account.class));
//        when(payloadDataManager.getPayload()).thenReturn(mockPayload);
//        when(payloadDataManager.getNextReceiveAddress(anyInt())).thenReturn("address");
//        when(unspentService.getUnspentOutputs(anyString(), any(Payment.class))).thenReturn(Observable.just(mock(UnspentOutputs.class)));
//        // Act
//        TestObserver<PendingTransaction> observer = subject.getPendingTransactionForLegacyAddress(legacyAddress, payment).test();
//        // Assert
//        observer.assertComplete();
//        observer.assertNoErrors();
//        assertEquals(PendingTransaction.class, observer.values().get(0).getClass());
//    }
//
//    @Test
//    public void submitPayment() throws Exception {
//        // Arrange
//        when(paymentService.submitPayment(
//                any(SpendableUnspentOutputs.class),
//                anyListOf(ECKey.class),
//                anyString(),
//                anyString(),
//                any(BigInteger.class),
//                any(BigInteger.class))).thenReturn(Observable.just("hash"));
//        // Act
//        TestObserver<String> observer = subject.submitPayment(mock(
//                SpendableUnspentOutputs.class),
//                mock(List.class),
//                "",
//                "",
//                mock(BigInteger.class),
//                mock(BigInteger.class)).test();
//        // Assert
//        observer.assertComplete();
//        observer.assertNoErrors();
//        assertEquals("hash", observer.values().get(0));
//    }
//
//    @Test
//    public void syncPayloadWithServer() throws Exception {
//        // Arrange
//        when(payloadDataManager.savePayloadToServer()).thenReturn(true);
//        // Act
//        TestObserver<Boolean> observer = subject.syncPayloadWithServer().test();
//        // Assert
//        observer.assertComplete();
//        observer.assertNoErrors();
//        assertEquals(true, observer.values().get(0).booleanValue());
//    }

}