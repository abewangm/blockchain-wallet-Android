package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.stores.TransactionListStore;
import piuk.blockchain.android.ui.account.ConsolidatedAccount;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class TransactionListDataManagerTest extends RxTest {

    @Mock private PayloadManager payloadManager;
    @Mock private RxBus rxBus;
    private TransactionListStore transactionListStore;
    private TransactionListDataManager subject;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        transactionListStore = new TransactionListStore();

        subject = new TransactionListDataManager(
                payloadManager,
                transactionListStore,
                rxBus);
    }

    @Test
    public void fetchTransactionsConsolidatedAccountTagAll() throws Exception {
        // Arrange
        ConsolidatedAccount account = new ConsolidatedAccount();
        account.setType(ConsolidatedAccount.Type.ALL_ACCOUNTS);
        List<TransactionSummary> transactionSummaries = Collections.singletonList(new TransactionSummary());
        when(payloadManager.getAllTransactions(0, 0)).thenReturn(transactionSummaries);
        // Act
        TestObserver<List<TransactionSummary>> testObserver = subject.fetchTransactions(account, 0, 0).test();
        // Assert
        verify(payloadManager).getAllTransactions(0, 0);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(transactionSummaries);
    }

    @Test
    public void fetchTransactionsConsolidatedAccountTagImported() throws Exception {
        // Arrange
        ConsolidatedAccount account = new ConsolidatedAccount();
        account.setType(ConsolidatedAccount.Type.ALL_IMPORTED_ADDRESSES);
        List<TransactionSummary> transactionSummaries = Collections.singletonList(new TransactionSummary());
        when(payloadManager.getImportedAddressesTransactions(0, 0)).thenReturn(transactionSummaries);
        // Act
        TestObserver<List<TransactionSummary>> testObserver = subject.fetchTransactions(account, 0, 0).test();
        // Assert
        verify(payloadManager).getImportedAddressesTransactions(0, 0);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(transactionSummaries);
    }

    @Test
    public void fetchTransactionsConsolidatedAccountNoTag() throws Exception {
        // Arrange
        ConsolidatedAccount account = new ConsolidatedAccount();
        // Act
        TestObserver<List<TransactionSummary>> testObserver = subject.fetchTransactions(account, 0, 0).test();
        // Assert
        verifyZeroInteractions(payloadManager);
        testObserver.assertNotComplete();
        testObserver.assertError(IllegalArgumentException.class);
    }

    @Test
    public void fetchTransactionsAccount() throws Exception {
        // Arrange
        Account account = new Account();
        String xPub = "X_PUB";
        account.setXpub(xPub);
        List<TransactionSummary> transactionSummaries = Collections.singletonList(new TransactionSummary());
        when(payloadManager.getAccountTransactions(xPub, 0, 0)).thenReturn(transactionSummaries);
        // Act
        TestObserver<List<TransactionSummary>> testObserver = subject.fetchTransactions(account, 0, 0).test();
        // Assert
        verify(payloadManager).getAccountTransactions(xPub, 0, 0);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(transactionSummaries);
    }

    @Test
    public void fetchTransactionsWrongObject() throws Exception {
        // Arrange
        Object object = new Object();
        // Act
        TestObserver<List<TransactionSummary>> testObserver = subject.fetchTransactions(object, 0, 0).test();
        // Assert
        verifyZeroInteractions(payloadManager);
        testObserver.assertNotComplete();
        testObserver.assertError(IllegalArgumentException.class);
    }

    @Test
    public void getTransactionList() throws Exception {
        // Arrange

        // Act
        List<TransactionSummary> value = subject.getTransactionList();
        // Assert
        assertEquals(transactionListStore.getList(), value);
        assertEquals(Collections.emptyList(), value);
    }

    @Test
    public void clearTransactionList() throws Exception {
        // Arrange
        transactionListStore.getList().add(new TransactionSummary());
        // Act
        subject.clearTransactionList();
        // Assert
        assertEquals(Collections.emptyList(), subject.getTransactionList());
    }

    @Test
    public void insertTransactionIntoListAndReturnSorted() throws Exception {
        // Arrange
        TransactionSummary tx0 = new TransactionSummary();
        tx0.setTime(0L);
        TransactionSummary tx1 = new TransactionSummary();
        tx1.setTime(500L);
        TransactionSummary tx2 = new TransactionSummary();
        tx2.setTime(1000L);
        transactionListStore.insertTransactions(Arrays.asList(tx1, tx0));
        // Act
        List<TransactionSummary> value = subject.insertTransactionIntoListAndReturnSorted(tx2);
        // Assert
        assertNotNull(value);
        assertEquals(tx2, value.get(0));
        assertEquals(tx1, value.get(1));
        assertEquals(tx0, value.get(2));
    }

    @Test
    public void getBtcBalanceConsolidatedAccountTagAll() throws Exception {
        // Arrange
        ConsolidatedAccount account = new ConsolidatedAccount();
        account.setType(ConsolidatedAccount.Type.ALL_ACCOUNTS);
        BigInteger balance = BigInteger.valueOf(1_000_000_000_000L);
        when(payloadManager.getWalletBalance()).thenReturn(balance);
        // Act
        long value = subject.getBtcBalance(account);
        // Assert
        verify(payloadManager).getWalletBalance();
        assertEquals(1_000_000_000_000L, value);
    }

    @Test
    public void getBtcBalanceConsolidatedAccountTagImported() throws Exception {
        // Arrange
        ConsolidatedAccount account = new ConsolidatedAccount();
        account.setType(ConsolidatedAccount.Type.ALL_IMPORTED_ADDRESSES);
        BigInteger balance = BigInteger.valueOf(1_000_000_000_000L);
        when(payloadManager.getImportedAddressesBalance()).thenReturn(balance);
        // Act
        long value = subject.getBtcBalance(account);
        // Assert
        verify(payloadManager).getImportedAddressesBalance();
        assertEquals(1_000_000_000_000L, value);
    }

    @Test
    public void getBtcBalanceConsolidatedAccountNoTag() throws Exception {
        // Arrange
        ConsolidatedAccount account = new ConsolidatedAccount();
        // Act
        long value = subject.getBtcBalance(account);
        // Assert
        verifyZeroInteractions(payloadManager);
        assertEquals(0L, value);
    }

    @Test
    public void getBtcBalanceAccount() throws Exception {
        // Arrange
        Account account = new Account();
        String xPub = "X_PUB";
        account.setXpub(xPub);
        BigInteger balance = BigInteger.valueOf(1_000_000_000_000L);
        when(payloadManager.getAddressBalance(xPub)).thenReturn(balance);
        // Act
        long value = subject.getBtcBalance(account);
        // Assert
        verify(payloadManager).getAddressBalance(xPub);
        assertEquals(1_000_000_000_000L, value);
    }

    @Test
    public void getBtcBalanceLegacyAddress() throws Exception {
        // Arrange
        LegacyAddress legacyAddress = new LegacyAddress();
        String address = "ADDRESS";
        legacyAddress.setAddress(address);
        BigInteger balance = BigInteger.valueOf(1_000_000_000_000L);
        when(payloadManager.getAddressBalance(address)).thenReturn(balance);
        // Act
        long value = subject.getBtcBalance(legacyAddress);
        // Assert
        verify(payloadManager).getAddressBalance(address);
        assertEquals(1_000_000_000_000L, value);
    }

    @Test
    public void getBtcBalanceWrongObject() throws Exception {
        // Arrange
        Object object = new Object();
        // Act
        long value = subject.getBtcBalance(object);
        // Assert
        verifyZeroInteractions(payloadManager);
        assertEquals(0L, value);
    }

    @Test
    public void getTxFromHashFound() {
        // Arrange
        String txHash = "TX_HASH";
        TransactionSummary tx0 = new TransactionSummary();
        tx0.setHash("");
        TransactionSummary tx1 = new TransactionSummary();
        tx1.setHash("");
        TransactionSummary tx2 = new TransactionSummary();
        tx2.setHash(txHash);
        transactionListStore.insertTransactions(Arrays.asList(tx0, tx1, tx2));
        // Act
        TestObserver<TransactionSummary> testObserver = subject.getTxFromHash(txHash).test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(tx2);
    }

    @Test
    public void getTxFromHashNotFound() {
        // Arrange
        String txHash = "TX_HASH";
        TransactionSummary tx0 = new TransactionSummary();
        tx0.setHash("");
        TransactionSummary tx1 = new TransactionSummary();
        tx1.setHash("");
        TransactionSummary tx2 = new TransactionSummary();
        tx2.setHash("");
        transactionListStore.insertTransactions(Arrays.asList(tx0, tx1, tx2));
        // Act
        TestObserver<TransactionSummary> testObserver = subject.getTxFromHash(txHash).test();
        // Assert
        testObserver.assertTerminated();
        testObserver.assertNoValues();
        testObserver.assertError(NullPointerException.class);
    }

}