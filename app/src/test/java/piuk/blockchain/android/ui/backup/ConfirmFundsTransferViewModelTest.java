package piuk.blockchain.android.ui.backup;

import android.annotation.SuppressLint;
import android.app.Application;

import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payload.data.Wallet;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.datamanagers.SendDataManager;
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.receive.WalletAccountHelper;
import piuk.blockchain.android.ui.send.PendingTransaction;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class ConfirmFundsTransferViewModelTest {

    private ConfirmFundsTransferViewModel subject;
    @Mock private ConfirmFundsTransferViewModel.DataListener activity;
    @Mock private WalletAccountHelper walletAccountHelper;
    @Mock private TransferFundsDataManager transferFundsDataManager;
    @Mock private PayloadDataManager payloadDataManager;
    @Mock private PrefsUtil prefsUtil;
    @Mock private StringUtils stringUtils;
    @Mock private ExchangeRateFactory exchangeRateFactory;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new ApiModule(),
                new MockDataManagerModule());

        subject = new ConfirmFundsTransferViewModel(activity);
    }

    @Test
    public void onViewReady() throws Exception {
        // Arrange
        Wallet mockPayload = mock(Wallet.class, RETURNS_DEEP_STUBS);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        when(mockPayload.getHdWallets().get(0).getDefaultAccountIdx()).thenReturn(0);
        PendingTransaction transaction = new PendingTransaction();
        List<PendingTransaction> transactions = Arrays.asList(transaction, transaction);
        Triple<List<PendingTransaction>, Long, Long> triple = Triple.of(transactions, 100000000L, 10000L);
        when(transferFundsDataManager.getTransferableFundTransactionList(0))
                .thenReturn(Observable.just(triple));
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).setPaymentButtonEnabled(false);
        assertEquals(2, subject.mPendingTransactions.size());
    }

    @Test
    public void accountSelectedError() throws Exception {
        // Arrange
        Wallet mockPayload = mock(Wallet.class, RETURNS_DEEP_STUBS);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        Account account1 = new Account();
        account1.setArchived(true);
        Account account2 = new Account();
        when(mockPayload.getHdWallets().get(0).getAccounts())
                .thenReturn(Arrays.asList(account1, account2));
        when(transferFundsDataManager.getTransferableFundTransactionList(1))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        subject.accountSelected(0);
        // Assert
        verify(activity).setPaymentButtonEnabled(false);
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(activity).dismissDialog();
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void updateUi() throws Exception {
        // Arrange
        when(stringUtils.getQuantityString(anyInt(), anyInt())).thenReturn("test string");
        when(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(MonetaryUtil.UNIT_BTC);
        when(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD");
        when(exchangeRateFactory.getLastPrice(anyString())).thenReturn(100.0D);
        when(exchangeRateFactory.getSymbol(anyString())).thenReturn("$");
        subject.mPendingTransactions = new ArrayList<>();
        // Act
        subject.updateUi(100000000L, 10000L);
        // Assert
        verify(activity).updateFromLabel("test string");
        verify(activity).updateTransferAmountBtc("1.0 BTC");
        verify(activity).updateTransferAmountFiat("$100.00");
        verify(activity).updateFeeAmountBtc("0.0001 BTC");
        verify(activity).updateFeeAmountFiat("$0.01");
        verify(activity).setPaymentButtonEnabled(true);
        verify(activity).onUiUpdated();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void sendPaymentAndArchive() throws Exception {
        // Arrange
        when(transferFundsDataManager.sendPayment(anyList(), anyString())).thenReturn(Observable.just("hash"));
        when(activity.getIfArchiveChecked()).thenReturn(true);
        PendingTransaction transaction = new PendingTransaction();
        transaction.sendingObject = new ItemAccount("", "", null, null, null);
        transaction.sendingObject.accountObject = new LegacyAddress();
        subject.mPendingTransactions = Collections.singletonList(transaction);
        when(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete());
        // Act
        subject.sendPayment("password");
        // Assert
        verify(activity).getIfArchiveChecked();
        verify(activity).setPaymentButtonEnabled(false);
        verify(activity, times(2)).showProgressDialog();
        verify(activity, times(2)).hideProgressDialog();
        //noinspection WrongConstant
        verify(activity, times(2)).showToast(anyInt(), eq(ToastCustom.TYPE_OK));
        verify(activity).dismissDialog();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void sendPaymentNoArchive() throws Exception {
        // Arrange
        subject.mPendingTransactions = new ArrayList<>();
        when(transferFundsDataManager.sendPayment(anyList(), anyString()))
                .thenReturn(Observable.just("hash"));
        when(activity.getIfArchiveChecked()).thenReturn(false);
        // Act
        subject.sendPayment("password");
        // Assert
        verify(activity).getIfArchiveChecked();
        verify(activity).setPaymentButtonEnabled(false);
        verify(activity).showProgressDialog();
        verify(activity).hideProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_OK));
        verify(activity).dismissDialog();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void sendPaymentError() throws Exception {
        // Arrange
        subject.mPendingTransactions = new ArrayList<>();
        when(transferFundsDataManager.sendPayment(anyList(), anyString()))
                .thenReturn(Observable.error(new Throwable()));
        when(activity.getIfArchiveChecked()).thenReturn(false);
        // Act
        subject.sendPayment("password");
        // Assert
        verify(activity).getIfArchiveChecked();
        verify(activity).setPaymentButtonEnabled(false);
        verify(activity).showProgressDialog();
        verify(activity).hideProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(activity).dismissDialog();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void getReceiveToList() throws Exception {
        // Arrange
        when(walletAccountHelper.getAccountItems(anyBoolean())).thenReturn(new ArrayList<>());
        // Act
        List<ItemAccount> value = subject.getReceiveToList();
        // Assert
        assertNotNull(value);
        assertTrue(value.isEmpty());
    }

    @Test
    public void getDefaultAccount() throws Exception {
        // Arrange
        Wallet mockPayload = mock(Wallet.class, RETURNS_DEEP_STUBS);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        when(mockPayload.getHdWallets().get(0).getDefaultAccountIdx()).thenReturn(1);
        Account account1 = new Account();
        account1.setArchived(true);
        Account account2 = new Account();
        when(mockPayload.getHdWallets().get(0).getAccounts())
                .thenReturn(Arrays.asList(account1, account2));
        // Act
        int value = subject.getDefaultAccount();
        // Assert
        assertEquals(0, value);
    }

    @Test
    public void archiveAllSuccessful() throws Exception {
        // Arrange
        PendingTransaction transaction = new PendingTransaction();
        transaction.sendingObject = new ItemAccount("", "", null, null, null);
        transaction.sendingObject.accountObject = new LegacyAddress();
        subject.mPendingTransactions = Collections.singletonList(transaction);
        when(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete());
        // Act
        subject.archiveAll();
        // Assert
        verify(activity).showProgressDialog();
        verify(activity).hideProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_OK));
        verify(activity).dismissDialog();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void archiveAllUnsuccessful() throws Exception {
        // Arrange
        PendingTransaction transaction = new PendingTransaction();
        transaction.sendingObject = new ItemAccount("", "", null, null, null);
        transaction.sendingObject.accountObject = new LegacyAddress();
        subject.mPendingTransactions = Collections.singletonList(transaction);
        when(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.error(new Throwable()));
        // Act
        subject.archiveAll();
        // Assert
        verify(activity).showProgressDialog();
        verify(activity).hideProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(activity).dismissDialog();
        verifyNoMoreInteractions(activity);
    }

    private class MockApplicationModule extends ApplicationModule {

        public MockApplicationModule(Application application) {
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

    private class MockDataManagerModule extends DataManagerModule {
        @Override
        protected TransferFundsDataManager provideTransferFundsDataManager(PayloadDataManager payloadDataManager,
                                                                           SendDataManager sendDataManager,
                                                                           DynamicFeeCache dynamicFeeCache) {
            return transferFundsDataManager;
        }

        @Override
        protected WalletAccountHelper provideWalletAccountHelper(PayloadManager payloadManager,
                                                                 PrefsUtil prefsUtil,
                                                                 StringUtils stringUtils,
                                                                 ExchangeRateFactory exchangeRateFactory) {
            return walletAccountHelper;
        }

        @Override
        protected PayloadDataManager providePayloadDataManager(PayloadManager payloadManager,
                                                               RxBus rxBus) {
            return payloadDataManager;
        }
    }

}