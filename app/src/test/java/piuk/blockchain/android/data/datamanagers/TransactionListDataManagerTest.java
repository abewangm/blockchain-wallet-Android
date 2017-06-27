package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.stores.TransactionListStore;
import piuk.blockchain.android.ui.account.ItemAccount;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    public void fetchTransactionsAccountTagAll() throws Exception {
        // Arrange
        Account account = new Account();
        List<TransactionSummary> transactionSummaries = Collections.singletonList(new TransactionSummary());
        when(payloadManager.getAllTransactions(0, 0)).thenReturn(transactionSummaries);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(account);
        itemAccount.setType(ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY);
        // Act
        TestObserver<List<TransactionSummary>> testObserver = subject.fetchTransactions(itemAccount, 0, 0).test();
        // Assert
        verify(payloadManager).getAllTransactions(0, 0);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(transactionSummaries);
    }

    @Test
    public void fetchTransactionsAccountTagImported() throws Exception {
        // Arrange
        Account account = new Account();
        List<TransactionSummary> transactionSummaries = Collections.singletonList(new TransactionSummary());
        when(payloadManager.getImportedAddressesTransactions(0, 0)).thenReturn(transactionSummaries);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(account);
        itemAccount.setType(ItemAccount.TYPE.ALL_LEGACY);
        // Act
        TestObserver<List<TransactionSummary>> testObserver = subject.fetchTransactions(itemAccount, 0, 0).test();
        // Assert
        verify(payloadManager).getImportedAddressesTransactions(0, 0);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(transactionSummaries);
    }

    @Test
    public void fetchTransactionsAccount() throws Exception {
        // Arrange
        Account account = new Account();
        String xPub = "xpub6CfLQa8fLgtp8E7tc1khAhrZYPm82okmugxP7TrhMPkPFKANhdCUd4TDJKUYLCxZskG2U7Q689CVBxs2EjJA7dyvjCzN5UYWwZbY2qVpymw";
        List<TransactionSummary> transactionSummaries = Collections.singletonList(new TransactionSummary());
        when(payloadManager.getAccountTransactions(xPub, 0, 0)).thenReturn(transactionSummaries);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(account);
        itemAccount.setAddress(xPub);
        // Act
        TestObserver<List<TransactionSummary>> testObserver =
                subject.fetchTransactions(itemAccount, 0, 0).test();
        // Assert
        verify(payloadManager).getAccountTransactions(xPub, 0, 0);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(transactionSummaries);
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
    public void getBtcBalanceAccountTagAll() throws Exception {
        // Arrange
        Account account = new Account();
        BigInteger balance = BigInteger.valueOf(1_000_000_000_000L);
        when(payloadManager.getWalletBalance()).thenReturn(balance);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(account);
        itemAccount.setType(ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY);
        // Act
        long value = subject.getBtcBalance(itemAccount);
        // Assert
        verify(payloadManager).getWalletBalance();
        assertEquals(1_000_000_000_000L, value);
    }

    @Test
    public void getBtcBalanceAccountTagImported() throws Exception {
        // Arrange
        Account account = new Account();
        BigInteger balance = BigInteger.valueOf(1_000_000_000_000L);
        when(payloadManager.getImportedAddressesBalance()).thenReturn(balance);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(account);
        itemAccount.setType(ItemAccount.TYPE.ALL_LEGACY);
        // Act
        long value = subject.getBtcBalance(itemAccount);
        // Assert
        verify(payloadManager).getImportedAddressesBalance();
        assertEquals(1_000_000_000_000L, value);
    }

    @Test
    public void getBtcBalanceAccount() throws Exception {
        // Arrange
        Account account = new Account();
        String xPub = "X_PUB";
        BigInteger balance = BigInteger.valueOf(1_000_000_000_000L);
        when(payloadManager.getAddressBalance(xPub)).thenReturn(balance);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(account);
        itemAccount.setAddress(xPub);
        // Act
        long value = subject.getBtcBalance(itemAccount);
        // Assert
        verify(payloadManager).getAddressBalance(xPub);
        assertEquals(1_000_000_000_000L, value);
    }

    @Test
    public void getBtcBalanceLegacyAddress() throws Exception {
        // Arrange
        LegacyAddress legacyAddress = new LegacyAddress();
        String address = "ADDRESS";
        BigInteger balance = BigInteger.valueOf(1_000_000_000_000L);
        when(payloadManager.getAddressBalance(address)).thenReturn(balance);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(legacyAddress);
        itemAccount.setAddress(address);
        // Act
        long value = subject.getBtcBalance(itemAccount);
        // Assert
        verify(payloadManager).getAddressBalance(address);
        assertEquals(1_000_000_000_000L, value);
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
        testObserver.assertError(NoSuchElementException.class);
    }

}