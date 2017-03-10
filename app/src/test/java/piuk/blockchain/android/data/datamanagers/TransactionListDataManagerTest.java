package piuk.blockchain.android.data.datamanagers;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.RxTest;

@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class TransactionListDataManagerTest extends RxTest {

//    @Mock PayloadManager payloadDataManager;
//    @Mock TransactionDetailsService transactionDetailsService;
//    @Mock MultiAddrFactory multiAddrFactory;
//    private TransactionListStore transactionListStore;
//    private TransactionListDataManager subject;
//
//    @Before
//    public void setUp() throws Exception {
//        super.setUp();
//        MockitoAnnotations.initMocks(this);
//
//        transactionListStore = new TransactionListStore();
//
//        subject = new TransactionListDataManager(
//                payloadDataManager,
//                transactionDetailsService,
//                transactionListStore,
//                multiAddrFactory);
//    }
//
//    @Test
//    public void generateTransactionListAccountTagAllPayloadUpgraded() throws Exception {
//        // Arrange
//        Account account = new Account();
//        account.setRealIdx(TransactionListDataManager.INDEX_ALL_REAL);
//        Payload mockPayload = mock(Payload.class);
//        when(mockPayload.isUpgraded()).thenReturn(true);
//        when(payloadDataManager.getPayload()).thenReturn(mockPayload);
//        // Act
//        subject.generateTransactionList(account);
//        // Assert
//
//    }
//
//    @Test
//    public void generateTransactionListAccountTagAllPayloadNotUpgraded() throws Exception {
//        // Arrange
//        Account account = new Account();
//        account.setRealIdx(TransactionListDataManager.INDEX_ALL_REAL);
//        Payload mockPayload = mock(Payload.class);
//        when(mockPayload.isUpgraded()).thenReturn(false);
//        when(payloadDataManager.getPayload()).thenReturn(mockPayload);
//        // Act
//        subject.generateTransactionList(account);
//        // Assert
//
//    }
//
//    @Test
//    public void generateTransactionListAccountImportedAddresses() throws Exception {
//        // Arrange
//        Account account = new Account();
//        account.setRealIdx(TransactionListDataManager.INDEX_IMPORTED_ADDRESSES);
//        // Act
//        subject.generateTransactionList(account);
//        // Assert
//
//    }
//
//    @Test
//    public void generateTransactionAccountListNoTags() throws Exception {
//        // Arrange
//        Account account = new Account();
//        account.setXpub("test");
//        MultiAddrFactory.getInstance().setXpubAmount("test", 0L);
//        // Act
//        subject.generateTransactionList(account);
//        // Assert
//
//    }
//
//    @Test
//    public void generateTransactionLegacyAddress() throws Exception {
//        // Arrange
//        LegacyAddress legacyAddress = new LegacyAddress();
//        legacyAddress.setAddress("addr");
//        // Act
//        subject.generateTransactionList(legacyAddress);
//        // Assert
//
//    }
//
//    @Test
//    public void getTransactionList() throws Exception {
//        // Arrange
//
//        // Act
//        List<Tx> value = subject.getTransactionList();
//        // Assert
//        assertEquals(transactionListStore.getList(), value);
//        assertEquals(Collections.emptyList(), value);
//    }
//
//    @Test
//    public void clearTransactionList() throws Exception {
//        // Arrange
//        transactionListStore.getList().add(new Tx("", "", "", 0D, 0L, new HashMap<>()));
//        // Act
//        subject.clearTransactionList();
//        // Assert
//        assertEquals(Collections.emptyList(), subject.getTransactionList());
//    }
//
//    @Test
//    public void getListUpdateSubject() throws Exception {
//        // Arrange
//
//        // Act
//        Subject value = subject.getListUpdateSubject();
//        // Assert
//        assertNotNull(value);
//    }
//
//    @Test
//    public void insertTransactionIntoListAndReturnSorted() throws Exception {
//        // Arrange
//        Tx tx0 = new Tx("", "", "", 0D, 0L, new HashMap<>());
//        Tx tx1 = new Tx("", "", "", 0D, 500L, new HashMap<>());
//        Tx tx2 = new Tx("", "", "", 0D, 1000L, new HashMap<>());
//
//        transactionListStore.insertTransactions(Arrays.asList(tx1, tx0));
//        // Act
//        List<Tx> value = subject.insertTransactionIntoListAndReturnSorted(tx2);
//        // Assert
//        assertNotNull(value);
//        assertEquals(tx2, value.get(0));
//        assertEquals(tx1, value.get(1));
//        assertEquals(tx0, value.get(2));
//    }
//
//    @Test
//    public void getAllXpubAndLegacyTxs() {
//        // Arrange
//        Tx tx0 = new Tx("", "", "", 0D, 0L, new HashMap<>());
//        Tx tx1 = new Tx("", "", "", 0D, 0L, new HashMap<>());
//        tx1.setHash("hash");
//        HashMap<String, List<Tx>> map = new HashMap<>();
//        map.put("test", Arrays.asList(tx0, tx0));
//        when(multiAddrFactory.getXpubTxs()).thenReturn(map);
//        when(multiAddrFactory.getLegacyTxs()).thenReturn(Arrays.asList(tx0, tx1));
//        // Act
//        List<Tx> value = subject.getAllXpubAndLegacyTxs();
//        // Assert
//        assertNotNull(value);
//        assertEquals(2, value.size());
//    }
//
//    @Test
//    public void getBtcBalanceInvalidObject() throws Exception {
//        // Arrange
//
//        // Act
//        double value = subject.getBtcBalance(new Object());
//        // Assert
//        assertEquals(0D, value, 0D);
//    }
//
//    @Test
//    public void getBtcBalanceAccountTagAllUpgraded() throws Exception {
//        // Arrange
//        Account account = new Account();
//        account.setRealIdx(TransactionListDataManager.INDEX_ALL_REAL);
//        Payload mockPayload = mock(Payload.class);
//        when(mockPayload.isUpgraded()).thenReturn(true);
//        when(payloadDataManager.getPayload()).thenReturn(mockPayload);
//        // Act
//        double value = subject.getBtcBalance(account);
//        // Assert
//        assertEquals(0D, value, 0D);
//    }
//
//    @Test
//    public void getBtcBalanceAccountTagAllNotUpgraded() throws Exception {
//        // Arrange
//        Account account = new Account();
//        account.setRealIdx(TransactionListDataManager.INDEX_ALL_REAL);
//        Payload mockPayload = mock(Payload.class);
//        when(mockPayload.isUpgraded()).thenReturn(false);
//        when(payloadDataManager.getPayload()).thenReturn(mockPayload);
//        // Act
//        double value = subject.getBtcBalance(account);
//        // Assert
//        assertEquals(0D, value, 0D);
//    }
//
//    @Test
//    public void getBtcBalanceAccountTagImported() throws Exception {
//        // Arrange
//        Account account = new Account();
//        account.setRealIdx(TransactionListDataManager.INDEX_IMPORTED_ADDRESSES);
//        // Act
//        double value = subject.getBtcBalance(account);
//        // Assert
//        assertEquals(0D, value, 0D);
//    }
//
//    @Test
//    public void getBtcBalanceAccountV3Individual() throws Exception {
//        // Arrange
//        Account account = new Account();
//        account.setXpub("test");
//        MultiAddrFactory.getInstance().getXpubAmounts().put("test", 0L);
//        // Act
//        double value = subject.getBtcBalance(account);
//        // Assert
//        assertEquals(0D, value, 0D);
//    }
//
//    @Test
//    public void getBtcBalanceLegacyAddress() throws Exception {
//        // Arrange
//        LegacyAddress legacyAddress = new LegacyAddress();
//        // Act
//        double value = subject.getBtcBalance(legacyAddress);
//        // Assert
//        assertEquals(0D, value, 0D);
//    }
//
//    @Test
//    public void getTxFromHashFound() {
//        // Arrange
//        String txHash = "TX_HASH";
//        Tx tx0 = new Tx("", null, null, 0L, 0, null);
//        Tx tx1 = new Tx("", null, null, 0L, 0, null);
//        Tx tx2 = new Tx(txHash, null, null, 0L, 0, null);
//        transactionListStore.insertTransactions(Arrays.asList(tx0, tx1, tx2));
//        // Act
//        TestObserver<Tx> testObserver = subject.getTxFromHash(txHash).test();
//        // Assert
//        testObserver.assertComplete();
//        testObserver.assertNoErrors();
//        assertEquals(tx2, testObserver.values().get(0));
//    }
//
//    @Test
//    public void getTxFromHashNotFound() {
//        // Arrange
//        String txHash = "TX_HASH";
//        Tx tx0 = new Tx("", null, null, 0L, 0, null);
//        Tx tx1 = new Tx("", null, null, 0L, 0, null);
//        Tx tx2 = new Tx("", null, null, 0L, 0, null);
//        transactionListStore.insertTransactions(Arrays.asList(tx0, tx1, tx2));
//        // Act
//        TestObserver<Tx> testObserver = subject.getTxFromHash(txHash).test();
//        // Assert
//        testObserver.assertTerminated();
//        testObserver.assertNoValues();
//        testObserver.assertError(NullPointerException.class);
//    }
//
//    @Test
//    public void getTransactionFromHash() throws Exception {
//        // Arrange
//        Transaction mockTransaction = mock(Transaction.class);
//        when(transactionDetailsService.getTransactionDetailsFromHash(anyString()))
//                .thenReturn(Observable.just(mockTransaction));
//        // Act
//        TestObserver<Transaction> observer = subject.getTransactionFromHash("hash").test();
//        // Assert
//        assertEquals(mockTransaction, observer.values().get(0));
//        observer.onComplete();
//        observer.assertNoErrors();
//    }
//
//    @Test
//    public void updateTransactionNotes() throws Exception {
//        // Arrange
//        Payload mockPayload = mock(Payload.class);
//        when(mockPayload.getTransactionNotesMap()).thenReturn(new HashMap<>());
//        when(payloadDataManager.getPayload()).thenReturn(mockPayload);
//        when(payloadDataManager.savePayloadToServer()).thenReturn(true);
//        // Act
//        TestObserver<Boolean> observer = subject.updateTransactionNotes("hash", "notes").test();
//        // Assert
//        assertEquals(true, observer.values().get(0));
//        observer.assertComplete();
//        observer.assertNoErrors();
//    }

}