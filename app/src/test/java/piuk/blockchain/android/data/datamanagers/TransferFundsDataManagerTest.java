package piuk.blockchain.android.data.datamanagers;

import piuk.blockchain.android.RxTest;

public class TransferFundsDataManagerTest extends RxTest {

//    private TransferFundsDataManager subject;
//    @Mock PayloadManager payloadDataManager;
//    @Mock Unspent unspentApi;
//    @Mock Payment payment;
//    @Mock MultiAddrFactory multiAddrFactory;
//
//    @Before
//    public void setUp() throws Exception {
//        super.setUp();
//        MockitoAnnotations.initMocks(this);
//        subject = new TransferFundsDataManager(payloadDataManager, multiAddrFactory, unspentApi, payment);
//    }
//
//    @Test
//    public void getTransferableFundTransactionListForDefaultAccount() throws Exception {
//        // Arrange
//        Payload mockPayload = mock(Payload.class);
//        LegacyAddress legacyAddress1 = new LegacyAddress();
//        legacyAddress1.setAddress("address");
//        List<LegacyAddress> legacyAddresses = new ArrayList<LegacyAddress>() {{
//           add(legacyAddress1);
//           add(legacyAddress1);
//           add(legacyAddress1);
//        }};
//        SuggestedFee suggestedFee = new SuggestedFee();
//        suggestedFee.defaultFeePerKb = new BigInteger("100");
//        DynamicFeeCache.getInstance().setCachedDynamicFee(suggestedFee);
//        MultiAddrFactory.getInstance().setLegacyBalance("address", 1000000L);
//        when(payloadDataManager.getPayload()).thenReturn(mockPayload);
//        HDWallet mockHdWallet = mock(HDWallet.class);
//        when(mockPayload.getHdWallet()).thenReturn(mockHdWallet);
//        when(mockHdWallet.getDefaultIndex()).thenReturn(0);
//        when(mockPayload.getLegacyAddressList()).thenReturn(legacyAddresses);
//        when(unspentApi.getUnspentOutputs(anyString())).thenReturn(mock(JSONObject.class));
//        UnspentOutputs mockUnspentOutputs = mock(UnspentOutputs.class);
//        when(payment.getCoins(any(JSONObject.class))).thenReturn(mockUnspentOutputs);
//        SpendableUnspentOutputs mockSpendableUnspentOutputs = mock(SpendableUnspentOutputs.class);
//        when(payment.getSpendableCoins(any(UnspentOutputs.class), any(BigInteger.class), any(BigInteger.class))).thenReturn(mockSpendableUnspentOutputs);
//        when(mockSpendableUnspentOutputs.getAbsoluteFee()).thenReturn(new BigInteger("10"));
//        SweepBundle mockSweepBundle = mock(SweepBundle.class);
//        when(mockSweepBundle.getSweepAmount()).thenReturn(new BigInteger("5460"));
//        when(payment.getSweepBundle(any(UnspentOutputs.class), any(BigInteger.class))).thenReturn(mockSweepBundle);
//        // Act
//        TestObserver<Triple<List<PendingTransaction>, Long, Long>> observer = subject.getTransferableFundTransactionListForDefaultAccount().test();
//        // Assert
//        observer.assertComplete();
//        observer.assertNoErrors();
//    }
//
//    @Test
//    public void sendPaymentSuccessNoEncryption() throws Exception {
//        // Arrange
//        Payment mockPayment = mock(Payment.class);
//        doAnswer(invocation -> {
//            ((Payment.SubmitPaymentListener) invocation.getArguments()[6]).onSuccess("hash");
//            return null;
//        }).when(mockPayment).submitPayment(
//                any(SpendableUnspentOutputs.class),
//                anyList(),
//                isNull(),
//                isNull(),
//                any(BigInteger.class),
//                any(BigInteger.class),
//                any(Payment.SubmitPaymentListener.class));
//
//        PendingTransaction transaction1 = new PendingTransaction();
//        transaction1.sendingObject = new ItemAccount("", "", null, null, null);
//        transaction1.sendingObject.accountObject = new LegacyAddress();
//        transaction1.bigIntAmount = new BigInteger("1000000");
//        transaction1.bigIntFee = new BigInteger("100");
//        transaction1.unspentOutputBundle = new SpendableUnspentOutputs();
//
//        List<PendingTransaction> pendingTransactions = new ArrayList<PendingTransaction>() {{
//            add(transaction1);
//            add(transaction1);
//            add(transaction1);
//        }};
//        when(payloadDataManager.savePayloadToServer()).thenReturn(true);
//        Payload mockPayload = mock(Payload.class, RETURNS_DEEP_STUBS);
//        when(mockPayload.getHdWallet().getAccounts().get(anyInt())).thenReturn(mock(Account.class));
//        when(mockPayload.isDoubleEncrypted()).thenReturn(false);
//        when(payloadDataManager.getPayload()).thenReturn(mockPayload);
//
//        // Act
//        TestObserver<String> observer = subject.sendPayment(mockPayment, pendingTransactions, new CharSequenceX("password")).test();
//        // Assert
//        assertEquals("hash", observer.values().get(0));
//        observer.assertComplete();
//        observer.assertNoErrors();
//    }
//
//    @Test
//    public void sendPaymentEncryptionThrowsException() throws Exception {
//        // Arrange
//        Payment mockPayment = mock(Payment.class);
//        doAnswer(invocation -> {
//            ((Payment.SubmitPaymentListener) invocation.getArguments()[6]).onSuccess("hash");
//            return null;
//        }).when(mockPayment).submitPayment(
//                any(SpendableUnspentOutputs.class),
//                anyListOf(ECKey.class),
//                anyString(),
//                anyString(),
//                any(BigInteger.class),
//                any(BigInteger.class),
//                any(Payment.SubmitPaymentListener.class));
//
//        PendingTransaction transaction1 = new PendingTransaction();
//        transaction1.sendingObject = new ItemAccount("", "", null, null, null);
//        transaction1.sendingObject.accountObject = new LegacyAddress();
//        transaction1.bigIntAmount = new BigInteger("1000000");
//        transaction1.bigIntFee = new BigInteger("100");
//
//        List<PendingTransaction> pendingTransactions = new ArrayList<PendingTransaction>() {{
//            add(transaction1);
//            add(transaction1);
//            add(transaction1);
//        }};
//        when(payloadDataManager.savePayloadToServer()).thenReturn(true);
//        Payload mockPayload = mock(Payload.class);
//        // For now, this method will cause an exception to be thrown
//        // In the future, this should be testable and this particular setup should be successful
//        when(mockPayload.isDoubleEncrypted()).thenReturn(true);
//        when(payloadDataManager.getPayload()).thenReturn(mockPayload);
//
//        // Act
//        TestObserver<String> observer = subject.sendPayment(mockPayment, pendingTransactions, new CharSequenceX("password")).test();
//        // Assert
//        observer.assertError(Throwable.class);
//        observer.assertNotComplete();
//        observer.assertNoValues();
//    }
//
//    @Test()
//    public void sendPaymentFailed() throws Exception {
//        // Arrange
//        Payment mockPayment = mock(Payment.class);
//        doAnswer(invocation -> {
//            ((Payment.SubmitPaymentListener) invocation.getArguments()[6]).onFail("failed");
//            return null;
//        }).when(mockPayment).submitPayment(
//                any(SpendableUnspentOutputs.class),
//                anyList(),
//                isNull(),
//                isNull(),
//                isNull(),
//                isNull(),
//                any(Payment.SubmitPaymentListener.class));
//
//        PendingTransaction transaction1 = new PendingTransaction();
//        transaction1.sendingObject = new ItemAccount("", "", null, null, null);
//        transaction1.sendingObject.accountObject = new LegacyAddress();
//        transaction1.unspentOutputBundle = new SpendableUnspentOutputs();
//
//        List<PendingTransaction> pendingTransactions = new ArrayList<PendingTransaction>() {{
//            add(transaction1);
//            add(transaction1);
//            add(transaction1);
//        }};
//        when(payloadDataManager.savePayloadToServer()).thenReturn(true);
//        Payload mockPayload = mock(Payload.class, RETURNS_DEEP_STUBS);
//        when(mockPayload.getHdWallet().getAccounts().get(anyInt())).thenReturn(mock(Account.class));
//        when(mockPayload.isDoubleEncrypted()).thenReturn(false);
//        when(payloadDataManager.getPayload()).thenReturn(mockPayload);
//
//        // Act
//        TestObserver<String> observer = subject.sendPayment(mockPayment, pendingTransactions, new CharSequenceX("password")).test();
//        // Assert
//        observer.assertError(Throwable.class);
//        observer.assertNotComplete();
//        observer.assertNoValues();
//    }
//
//    @Test
//    public void sendPaymentException() throws Exception {
//        // Arrange
//        Payment mockPayment = mock(Payment.class);
//        doThrow(new NullPointerException())
//                .when(mockPayment)
//                .submitPayment(
//                        any(SpendableUnspentOutputs.class),
//                        anyListOf(ECKey.class),
//                        anyString(),
//                        anyString(),
//                        any(BigInteger.class),
//                        any(BigInteger.class),
//                        any(Payment.SubmitPaymentListener.class));
//
//        PendingTransaction transaction1 = new PendingTransaction();
//        transaction1.sendingObject = new ItemAccount("", "", null, null, null);
//        transaction1.sendingObject.accountObject = new LegacyAddress();
//
//        List<PendingTransaction> pendingTransactions = new ArrayList<PendingTransaction>() {{
//            add(transaction1);
//            add(transaction1);
//            add(transaction1);
//        }};
//        when(payloadDataManager.savePayloadToServer()).thenReturn(true);
//
//        // Act
//        TestObserver<String> observer = subject.sendPayment(mockPayment, pendingTransactions, new CharSequenceX("password")).test();
//        // Assert
//        observer.assertError(Throwable.class);
//        observer.assertNotComplete();
//        observer.assertNoValues();
//    }
//
//    @Test
//    public void savePayloadToServer() throws Exception {
//        // Arrange
//        when(payloadDataManager.savePayloadToServer()).thenReturn(true);
//        // Act
//        TestObserver<Boolean> observer = subject.savePayloadToServer().test();
//        // Assert
//        assertEquals(true, observer.values().get(0));
//        observer.assertComplete();
//        observer.assertNoErrors();
//    }

}