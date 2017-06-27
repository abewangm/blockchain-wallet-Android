package piuk.blockchain.android.ui.transactions;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;

import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Wallet;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.R;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.stores.PendingTransactionListStore;
import piuk.blockchain.android.data.stores.TransactionListStore;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static piuk.blockchain.android.ui.balance.BalanceFragment.KEY_TRANSACTION_HASH;
import static piuk.blockchain.android.ui.balance.BalanceFragment.KEY_TRANSACTION_LIST_POSITION;

@SuppressWarnings({"PrivateMemberAccessBetweenOuterAndInnerClass", "WeakerAccess"})
public class TransactionDetailViewModelTest extends RxTest {

    private TransactionDetailViewModel subject;
    @Mock TransactionHelper transactionHelper;
    @Mock PrefsUtil prefsUtil;
    /**
     * This can be removed once providesPayloadManager is removed from the API module
     */
    @Mock PayloadManager mockPayloadManager;
    @Mock PayloadDataManager payloadDataManager;
    @Mock StringUtils stringUtils;
    @Mock TransactionListDataManager transactionListDataManager;
    @Mock TransactionDetailViewModel.DataListener activity;
    @Mock ExchangeRateFactory exchangeRateFactory;
    @Mock ContactsDataManager contactsDataManager;

    // Transactions
    private TransactionSummary txMoved = new TransactionSummary();
    private TransactionSummary txSent = new TransactionSummary();
    private TransactionSummary txReceived = new TransactionSummary();
    private List<TransactionSummary> txList;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        when(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(MonetaryUtil.UNIT_BTC);
        when(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)).thenReturn(PrefsUtil.DEFAULT_CURRENCY);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new MockApiModule(),
                new MockDataManagerModule());

        // Fees are realistic for current block size
        txMoved.setDirection(TransactionSummary.Direction.TRANSFERRED);
        txMoved.setTotal(BigInteger.TEN);
        txMoved.setFee(BigInteger.ONE);
        txMoved.setHash("txMoved_hash");
        txSent.setDirection(TransactionSummary.Direction.SENT);
        txSent.setTotal(BigInteger.TEN);
        txSent.setFee(BigInteger.ONE);
        txSent.setHash("txSent_hash");
        txReceived.setDirection(TransactionSummary.Direction.RECEIVED);
        txReceived.setTotal(BigInteger.TEN);
        txReceived.setFee(BigInteger.ONE);
        txReceived.setHash("txReceived_hash");
        txList = Arrays.asList(txMoved, txSent, txReceived);
        Locale.setDefault(new Locale("EN", "US"));
        subject = new TransactionDetailViewModel(activity);
    }

    @Test
    public void onViewReadyNoIntent() throws Exception {
        // Arrange
        when(activity.getPageIntent()).thenReturn(null);
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).pageFinish();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onViewReadyNoKey() throws Exception {
        // Arrange
        Intent mockIntent = mock(Intent.class);
        when(mockIntent.hasExtra(KEY_TRANSACTION_LIST_POSITION)).thenReturn(false);
        when(activity.getPageIntent()).thenReturn(mockIntent);
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).pageFinish();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onViewReadyKeyOutOfBounds() throws Exception {
        // Arrange
        Intent mockIntent = mock(Intent.class);
        when(mockIntent.hasExtra(KEY_TRANSACTION_LIST_POSITION)).thenReturn(true);
        when(mockIntent.getIntExtra(KEY_TRANSACTION_LIST_POSITION, -1)).thenReturn(-1);
        when(activity.getPageIntent()).thenReturn(mockIntent);
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).pageFinish();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onViewReadyNullIntent() throws Exception {
        // Arrange
        when(activity.getPageIntent()).thenReturn(null);
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).pageFinish();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onViewReadyIntentPositionInvalid() throws Exception {
        // Arrange
        Intent intent = mock(Intent.class);
        when(intent.getIntExtra(KEY_TRANSACTION_LIST_POSITION, -1)).thenReturn(-1);
        when(activity.getPageIntent()).thenReturn(intent);
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).pageFinish();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onViewReadyIntentHashNotFound() throws Exception {
        // Arrange
        Intent intent = mock(Intent.class);
        String txHash = "TX_HASH";
        when(intent.getStringExtra(KEY_TRANSACTION_HASH)).thenReturn(txHash);
        when(activity.getPageIntent()).thenReturn(intent);
        when(transactionListDataManager.getTxFromHash(txHash))
                .thenReturn(Single.error(new Throwable()));
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).pageFinish();
        verifyNoMoreInteractions(activity);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onViewReadyTransactionFoundInList() throws Exception {
        // Arrange
        Intent mockIntent = mock(Intent.class);
        Wallet mockPayload = mock(Wallet.class);
        when(mockIntent.hasExtra(KEY_TRANSACTION_LIST_POSITION)).thenReturn(true);
        when(mockIntent.getIntExtra(KEY_TRANSACTION_LIST_POSITION, -1)).thenReturn(0);
        when(mockPayload.getTxNotes()).thenReturn(new HashMap<>());
        when(activity.getPageIntent()).thenReturn(mockIntent);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        when(transactionListDataManager.getTransactionList()).thenReturn(txList);
        when(stringUtils.getString(R.string.transaction_detail_pending)).thenReturn("Pending (%1$s/%2$s Confirmations)");
        HashMap<String, BigInteger> inputs = new HashMap<>();
        HashMap<String, BigInteger> outputs = new HashMap<>();
        inputs.put("addr1", BigInteger.valueOf(1000L));
        outputs.put("addr2", BigInteger.valueOf(2000L));
        Pair pair = Pair.of(inputs, outputs);
        when(transactionHelper.filterNonChangeAddresses(any(TransactionSummary.class))).thenReturn(pair);
        when(payloadDataManager.addressToLabel("addr1")).thenReturn("account1");
        when(payloadDataManager.addressToLabel("addr2")).thenReturn("account2");
        double price = 1000.00D;
        when(exchangeRateFactory.getHistoricPrice(anyLong(), anyString(), anyLong())).thenReturn(Observable.just(price));
        when(stringUtils.getString(R.string.transaction_detail_value_at_time_transferred)).thenReturn("Value when moved: ");
        when(exchangeRateFactory.getSymbol(anyString())).thenReturn("$");
        HashMap<String, String> notesMap = new HashMap<>();
        notesMap.put("txMoved_hash", "transaction_note");
        when(contactsDataManager.getNotesTransactionMap()).thenReturn(notesMap);
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).setStatus("Pending (0/3 Confirmations)", "txMoved_hash");
        verify(activity).setTransactionType(TransactionSummary.Direction.TRANSFERRED);
        verify(activity).setTransactionColour(R.color.product_gray_transferred_50);
        verify(activity).setDescription(null);
        verify(activity).setDate(anyString());
        verify(activity).setToAddresses(any());
        verify(activity).setFromAddress(any());
        verify(activity).setFee(anyString());
        verify(activity).setTransactionValueBtc(anyString());
        verify(activity).setTransactionValueFiat(anyString());
        verify(activity).setTransactionNote(anyString());
        verify(activity).onDataLoaded();
        verify(activity).setIsDoubleSpend(anyBoolean());
        verifyNoMoreInteractions(activity);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onViewReadyTransactionFoundViaHash() throws Exception {
        // Arrange
        Intent mockIntent = mock(Intent.class);
        Wallet mockPayload = mock(Wallet.class);
        when(mockIntent.hasExtra(KEY_TRANSACTION_HASH)).thenReturn(true);
        when(mockIntent.getStringExtra(KEY_TRANSACTION_HASH)).thenReturn("txMoved_hash");
        when(activity.getPageIntent()).thenReturn(mockIntent);
        when(mockPayload.getTxNotes()).thenReturn(new HashMap<>());
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        when(activity.getPageIntent()).thenReturn(mockIntent);
        when(transactionListDataManager.getTxFromHash("txMoved_hash"))
                .thenReturn(Single.just(txMoved));
        when(stringUtils.getString(R.string.transaction_detail_pending)).thenReturn("Pending (%1$s/%2$s Confirmations)");
        HashMap<String, BigInteger> inputs = new HashMap<>();
        HashMap<String, BigInteger> outputs = new HashMap<>();
        inputs.put("addr1", BigInteger.valueOf(1000L));
        outputs.put("addr2", BigInteger.valueOf(2000L));
        Pair pair = Pair.of(inputs, outputs);
        when(transactionHelper.filterNonChangeAddresses(any(TransactionSummary.class))).thenReturn(pair);
        when(payloadDataManager.addressToLabel("addr1")).thenReturn("account1");
        when(payloadDataManager.addressToLabel("addr2")).thenReturn("account2");
        double price = 1000.00D;
        when(exchangeRateFactory.getHistoricPrice(anyLong(), anyString(), anyLong())).thenReturn(Observable.just(price));
        when(stringUtils.getString(R.string.transaction_detail_value_at_time_transferred)).thenReturn("Value when moved: ");
        when(exchangeRateFactory.getSymbol(anyString())).thenReturn("$");
        HashMap contactsMap = new HashMap<String, String>();
        contactsMap.put("txMoved_hash", "Adam");
        when(contactsDataManager.getContactsTransactionMap()).thenReturn(contactsMap);
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).setStatus("Pending (0/3 Confirmations)", "txMoved_hash");
        verify(activity).setTransactionType(TransactionSummary.Direction.TRANSFERRED);
        verify(activity).setTransactionColour(R.color.product_gray_transferred_50);
        verify(activity).setDescription(null);
        verify(activity).setDate(anyString());
        verify(activity).setToAddresses(any());
        verify(activity).setFromAddress(any());
        verify(activity).setFee(anyString());
        verify(activity).setTransactionValueBtc(anyString());
        verify(activity).setTransactionValueFiat(anyString());
        verify(activity).onDataLoaded();
        verify(activity).setIsDoubleSpend(anyBoolean());
        verifyNoMoreInteractions(activity);
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void getTransactionValueStringUsd() {
        // Arrange
        double price = 1000.00D;
        when(exchangeRateFactory.getHistoricPrice(anyLong(), anyString(), anyLong())).thenReturn(Observable.just(price));
        when(stringUtils.getString(anyInt())).thenReturn("Value when sent: ");
        when(exchangeRateFactory.getSymbol(anyString())).thenReturn("$");
        // Act
        TestObserver<String> observer =
                subject.getTransactionValueString("USD", txSent).test();
        // Assert
        assertEquals("Value when sent: $1000.00", observer.values().get(0));
        observer.onComplete();
        observer.assertNoErrors();
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void getTransactionValueStringReceived() {
        // Arrange
        double price = 1000.00D;
        when(exchangeRateFactory.getHistoricPrice(anyLong(), anyString(), anyLong()))
                .thenReturn(Observable.just(price));
        when(stringUtils.getString(anyInt())).thenReturn("Value when received: ");
        when(exchangeRateFactory.getSymbol(anyString())).thenReturn("$");
        // Act
        TestObserver<String> observer = subject.getTransactionValueString("USD", txReceived).test();
        // Assert
        assertEquals("Value when received: $1000.00", observer.values().get(0));
        observer.onComplete();
        observer.assertNoErrors();
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void getTransactionValueStringTransferred() {
        // Arrange
        double price = 1000.00D;
        when(exchangeRateFactory.getHistoricPrice(anyLong(), anyString(), anyLong()))
                .thenReturn(Observable.just(price));
        when(stringUtils.getString(anyInt())).thenReturn("Value when transferred: ");
        when(exchangeRateFactory.getSymbol(anyString())).thenReturn("$");
        // Act
        TestObserver<String> observer = subject.getTransactionValueString("USD", txSent).test();
        // Assert
        assertEquals("Value when transferred: $1000.00", observer.values().get(0));
        observer.onComplete();
        observer.assertNoErrors();
    }

    @Test
    public void updateTransactionNoteSuccess() throws Exception {
        // Arrange
        when(payloadDataManager.updateTransactionNotes(anyString(), anyString()))
                .thenReturn(Completable.complete());
        subject.mTransaction = txMoved;
        // Act
        subject.updateTransactionNote("note");
        // Assert
        verify(payloadDataManager).updateTransactionNotes(txMoved.getHash(), "note");
        //noinspection WrongConstant
        verify(activity).showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK);
        verify(activity).setDescription("note");
    }

    @Test
    public void updateTransactionNoteFailure() throws Exception {
        // Arrange
        when(payloadDataManager.updateTransactionNotes(anyString(), anyString()))
                .thenReturn(Completable.error(new Throwable()));
        subject.mTransaction = txMoved;
        // Act
        subject.updateTransactionNote("note");
        // Assert
        verify(payloadDataManager).updateTransactionNotes(txMoved.getHash(), "note");
        //noinspection WrongConstant
        verify(activity).showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
    }

    @Test
    public void getTransactionNote() throws Exception {
        // Arrange
        when(payloadDataManager.getTransactionNotes(txSent.getHash())).thenReturn("note");
        subject.mTransaction = txSent;
        // Act
        String value = subject.getTransactionNote();
        // Assert
        assertEquals("note", value);
    }

    @Test
    public void getTransactionHash() throws Exception {
        // Arrange
        subject.mTransaction = txSent;
        // Act
        String value = subject.getTransactionHash();
        // Assert
        assertEquals(txSent.getHash(), value);
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void setTransactionStatusNoConfirmations() {
        // Arrange
        when(stringUtils.getString(R.string.transaction_detail_pending))
                .thenReturn("Pending (%1$s/%2$s Confirmations)");
        // Act
        subject.setConfirmationStatus("hash", 0);
        // Assert
        verify(activity).setStatus("Pending (0/3 Confirmations)", "hash");
        verifyNoMoreInteractions(activity);
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void setTransactionStatusConfirmed() {
        // Arrange
        when(stringUtils.getString(R.string.transaction_detail_confirmed)).thenReturn("Confirmed");
        txMoved.setConfirmations(3);
        // Act
        subject.setConfirmationStatus("hash", 3);
        // Assert
        verify(activity).setStatus("Confirmed", "hash");
        verifyNoMoreInteractions(activity);
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void setTransactionColorMove() {
        // Arrange

        // Act
        subject.setTransactionColor(txMoved);
        // Assert
        verify(activity).setTransactionColour(R.color.product_gray_transferred_50);
        verifyNoMoreInteractions(activity);
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void setTransactionColorMoveConfirmed() {
        // Arrange
        txMoved.setConfirmations(3);
        // Act
        subject.setTransactionColor(txMoved);
        // Assert
        verify(activity).setTransactionColour(R.color.product_gray_transferred);
        verifyNoMoreInteractions(activity);
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void setTransactionColorSent() {
        // Arrange

        // Act
        subject.setTransactionColor(txSent);
        // Assert
        verify(activity).setTransactionColour(R.color.product_red_sent_50);
        verifyNoMoreInteractions(activity);
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void setTransactionColorSentConfirmed() {
        // Arrange
        txSent.setConfirmations(3);
        // Act
        subject.setTransactionColor(txSent);
        // Assert
        verify(activity).setTransactionColour(R.color.product_red_sent);
        verifyNoMoreInteractions(activity);
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void setTransactionColorReceived() {
        // Arrange

        // Act
        subject.setTransactionColor(txReceived);
        // Assert
        verify(activity).setTransactionColour(R.color.product_green_received_50);
        verifyNoMoreInteractions(activity);
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void setTransactionColorReceivedConfirmed() {
        // Arrange
        txReceived.setConfirmations(3);
        // Act
        subject.setTransactionColor(txReceived);
        // Assert
        verify(activity).setTransactionColour(R.color.product_green_received);
        verifyNoMoreInteractions(activity);
    }

    private class MockApplicationModule extends ApplicationModule {
        MockApplicationModule(Application application) {
            super(application);
        }

        @Override
        protected PrefsUtil providePrefsUtil() {
            return prefsUtil;
        }

        @Override
        protected StringUtils provideStringUtils() {
            return stringUtils;
        }

        @Override
        protected ExchangeRateFactory provideExchangeRateFactory() {
            return exchangeRateFactory;
        }
    }

    private class MockApiModule extends ApiModule {
        @Override
        protected PayloadManager providePayloadManager() {
            return mockPayloadManager;
        }

        @Override
        protected ContactsDataManager provideContactsManager(PendingTransactionListStore pendingTransactionListStore,
                                                             RxBus rxBus) {
            return contactsDataManager;
        }
    }

    private class MockDataManagerModule extends DataManagerModule {
        @Override
        protected TransactionListDataManager provideTransactionListDataManager(PayloadManager payloadManager,
                                                                               TransactionListStore transactionListStore,
                                                                               RxBus rxBus) {
            return transactionListDataManager;
        }

        @Override
        protected TransactionHelper provideTransactionHelper(PayloadDataManager payloadDataManager) {
            return transactionHelper;
        }

        @Override
        protected PayloadDataManager providePayloadDataManager(PayloadManager payloadManager, RxBus rxBus) {
            return payloadDataManager;
        }
    }

}