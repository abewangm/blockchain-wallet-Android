package piuk.blockchain.android.ui.account;

import android.annotation.SuppressLint;
import android.content.Intent;

import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.PayloadException;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payload.data.Wallet;
import info.blockchain.wallet.util.PrivateKeyFactory;

import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.ECKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.bitcoincash.BchDataManager;
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.send.PendingTransaction;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static piuk.blockchain.android.ui.account.AccountPresenter.KEY_WARN_TRANSFER_ALL;

@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class AccountPresenterTest {

    private AccountPresenter subject;
    @Mock private AccountView activity;
    @Mock private PayloadDataManager payloadDataManager;
    @Mock private BchDataManager bchDataManager;
    @Mock private TransferFundsDataManager fundsDataManager;
    @Mock private PrefsUtil prefsUtil;
    @Mock private AppUtil appUtil;
    @Mock private EnvironmentSettings environmentSettings;
    private PrivateKeyFactory privateKeyFactory = new PrivateKeyFactory();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        subject = new AccountPresenter(payloadDataManager,
                bchDataManager,
                fundsDataManager,
                prefsUtil,
                appUtil,
                privateKeyFactory,
                environmentSettings);

        subject.initView(activity);
    }

    @Test
    public void onViewReady() throws Exception {
        // Arrange

        // Act
        subject.onViewReady();
        // Assert
        assertTrue(true);
    }

    @Test
    public void setDoubleEncryptionPassword() throws Exception {
        // Arrange
        String password = "password";
        // Act
        subject.setDoubleEncryptionPassword(password);
        // Assert
        assertEquals(password, subject.doubleEncryptionPassword);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void checkTransferableLegacyFundsWarnTransferAllTrue() throws Exception {
        // Arrange
        Triple triple = Triple.of(Collections.singletonList(new PendingTransaction()), 1L, 2L);
        when(fundsDataManager.getTransferableFundTransactionListForDefaultAccount())
                .thenReturn(Observable.just(triple));
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.isUpgraded()).thenReturn(true);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        when(prefsUtil.getValue(KEY_WARN_TRANSFER_ALL, true)).thenReturn(true);
        // Act
        subject.checkTransferableLegacyFunds(false, true);
        // Assert
        verify(activity).onSetTransferLegacyFundsMenuItemVisible(true);
        verify(activity).onShowTransferableLegacyFundsWarning(false);
        verify(activity).dismissProgressDialog();
        verifyNoMoreInteractions(activity);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void checkTransferableLegacyFundsWarnTransferAllTrueDontShowDialog() throws Exception {
        // Arrange
        Triple triple = Triple.of(Collections.singletonList(new PendingTransaction()), 1L, 2L);
        when(fundsDataManager.getTransferableFundTransactionListForDefaultAccount())
                .thenReturn(Observable.just(triple));
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.isUpgraded()).thenReturn(true);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        when(prefsUtil.getValue(KEY_WARN_TRANSFER_ALL, true)).thenReturn(true);
        // Act
        subject.checkTransferableLegacyFunds(false, false);
        // Assert
        verify(activity).onSetTransferLegacyFundsMenuItemVisible(true);
        verify(activity).dismissProgressDialog();
        verifyNoMoreInteractions(activity);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void checkTransferableLegacyFundsNoFundsAvailable() throws Exception {
        // Arrange
        Triple triple = Triple.of(Collections.emptyList(), 1L, 2L);
        when(fundsDataManager.getTransferableFundTransactionListForDefaultAccount())
                .thenReturn(Observable.just(triple));
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.isUpgraded()).thenReturn(true);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        // Act
        subject.checkTransferableLegacyFunds(true, true);
        // Assert
        verify(activity).onSetTransferLegacyFundsMenuItemVisible(false);
        verify(activity).dismissProgressDialog();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void checkTransferableLegacyFundsThrowsException() throws Exception {
        // Arrange
        when(fundsDataManager.getTransferableFundTransactionListForDefaultAccount())
                .thenReturn(Observable.error(new Throwable()));
        // Act
        subject.checkTransferableLegacyFunds(true, true);
        // Assert
        verify(activity).onSetTransferLegacyFundsMenuItemVisible(false);
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void createNewAccountSuccessful() throws Exception {
        // Arrange
        when(payloadDataManager.createNewAccount(anyString(), isNull()))
                .thenReturn(Observable.just(new Account()));
        // Act
        subject.createNewAccount("");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_OK));
        verify(activity).broadcastIntent(any(Intent.class));
        verify(activity).onUpdateAccountsList();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void createNewAccountDecryptionException() throws Exception {
        // Arrange
        when(payloadDataManager.createNewAccount(anyString(), isNull()))
                .thenReturn(Observable.error(new DecryptionException()));
        // Act
        subject.createNewAccount("");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void createNewAccountPayloadException() throws Exception {
        // Arrange
        when(payloadDataManager.createNewAccount(anyString(), isNull()))
                .thenReturn(Observable.error(new PayloadException()));
        // Act
        subject.createNewAccount("");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void createNewAccountUnknownException() throws Exception {
        // Arrange
        when(payloadDataManager.createNewAccount(anyString(), isNull()))
                .thenReturn(Observable.error(new Exception()));
        // Act
        subject.createNewAccount("");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void updateLegacyAddressSuccessful() throws Exception {
        // Arrange
        LegacyAddress legacyAddress = new LegacyAddress();
        when(payloadDataManager.updateLegacyAddress(legacyAddress))
                .thenReturn(Completable.complete());
        // Act
        subject.updateLegacyAddress(legacyAddress);
        // Assert
        verify(payloadDataManager).updateLegacyAddress(legacyAddress);
        verifyNoMoreInteractions(payloadDataManager);
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_OK));
        verify(activity).broadcastIntent(any(Intent.class));
        verify(activity).onUpdateAccountsList();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void updateLegacyAddressFailed() throws Exception {
        // Arrange
        LegacyAddress legacyAddress = new LegacyAddress();
        when(payloadDataManager.updateLegacyAddress(legacyAddress))
                .thenReturn(Completable.error(new Throwable()));
        // Act
        subject.updateLegacyAddress(legacyAddress);
        // Assert
        verify(payloadDataManager).updateLegacyAddress(legacyAddress);
        verifyNoMoreInteractions(payloadDataManager);
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onScanButtonClickedCameraInUse() throws Exception {
        // Arrange
        when(appUtil.isCameraOpen()).thenReturn(true);
        // Act
        subject.onScanButtonClicked();
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onScanButtonClickedCameraAvailable() throws Exception {
        // Arrange
        when(appUtil.isCameraOpen()).thenReturn(false);
        // Act
        subject.onScanButtonClicked();
        // Assert
        //noinspection WrongConstant
        verify(activity).startScanForResult();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void importBip38AddressWithValidPassword() throws Exception {
        // Arrange

        // Act
        subject.importBip38Address(
                "6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS",
                "password");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
    }

    @Test
    public void importBip38AddressWithIncorrectPassword() throws Exception {
        // Arrange

        // Act
        subject.importBip38Address(
                "6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS",
                "notthepassword");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(activity).dismissProgressDialog();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onAddressScannedBip38() throws Exception {
        // Arrange

        // Act
        subject.onAddressScanned("6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS");
        // Assert
        verify(activity).showBip38PasswordDialog("6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS");
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onAddressScannedNonBip38() throws Exception {
        // Arrange
        when(payloadDataManager.getKeyFromImportedData(anyString(), anyString()))
                .thenReturn(Observable.just(mock(ECKey.class)));
        // Act
        subject.onAddressScanned("L1FQxC7wmmRNNe2YFPNXscPq3kaheiA4T7SnTr7vYSBW7Jw1A7PD");
        // Assert
        verify(payloadDataManager).getKeyFromImportedData(anyString(), anyString());
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
    }

    @Test
    public void onAddressScannedNonBip38Failure() throws Exception {
        // Arrange
        when(payloadDataManager.getKeyFromImportedData(anyString(), anyString()))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        subject.onAddressScanned("L1FQxC7wmmRNNe2YFPNXscPq3kaheiA4T7SnTr7vYSBW7Jw1A7PD");
        when(payloadDataManager.getKeyFromImportedData(anyString(), anyString()))
                .thenReturn(Observable.just(mock(ECKey.class)));
        // Assert
        verify(payloadDataManager).getKeyFromImportedData(anyString(), anyString());
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
    }

    @Test
    public void onAddressScannedWatchOnlyInvalidAddress() throws Exception {
        // Arrange

        // Act
        subject.onAddressScanned("test");
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onAddressScannedWatchOnlyNullAddress() throws Exception {
        // Arrange

        // Act
        subject.onAddressScanned(null);
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onAddressScannedWatchAddressAlreadyInWallet() throws Exception {
        // Arrange
        Wallet mockPayload = mock(Wallet.class, RETURNS_DEEP_STUBS);
        //noinspection SuspiciousMethodCalls
        when(mockPayload.getLegacyAddressStringList().contains(any())).thenReturn(true);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        // Act
        subject.onAddressScanned("17UovdU9ZvepPe75igTQwxqNME1HbnvMB7");
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onAddressScannedWatchAddressNotInWallet() throws Exception {
        // Arrange
        Wallet mockPayload = mock(Wallet.class, RETURNS_DEEP_STUBS);
        //noinspection SuspiciousMethodCalls
        when(mockPayload.getLegacyAddressStringList().contains(any())).thenReturn(false);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        // Act
        subject.onAddressScanned("17UovdU9ZvepPe75igTQwxqNME1HbnvMB7");
        // Assert
        //noinspection WrongConstant
        verify(activity).showWatchOnlyWarningDialog("17UovdU9ZvepPe75igTQwxqNME1HbnvMB7");
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void confirmImportWatchOnlySuccess() throws Exception {
        // Arrange
        String address = "17UovdU9ZvepPe75igTQwxqNME1HbnvMB7";
        when(payloadDataManager.addLegacyAddress(any(LegacyAddress.class)))
                .thenReturn(Completable.complete());
        // Act
        subject.confirmImportWatchOnly(address);
        // Assert
        //noinspection WrongConstant
        verify(payloadDataManager).addLegacyAddress(any(LegacyAddress.class));
        verifyNoMoreInteractions(payloadDataManager);
        verify(activity).showRenameImportedAddressDialog(any(LegacyAddress.class));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void confirmImportWatchOnlyFailure() throws Exception {
        // Arrange
        String address = "17UovdU9ZvepPe75igTQwxqNME1HbnvMB7";
        when(payloadDataManager.addLegacyAddress(any(LegacyAddress.class)))
                .thenReturn(Completable.error(new Throwable()));
        // Act
        subject.confirmImportWatchOnly(address);
        // Assert
        verify(payloadDataManager).addLegacyAddress(any(LegacyAddress.class));
        verifyNoMoreInteractions(payloadDataManager);
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void getAccounts() throws Exception {
        // Arrange
        List<Account> list = Collections.emptyList();
        when(payloadDataManager.getAccounts()).thenReturn(list);
        // Act
        List<Account> result = subject.getAccounts();
        // Assert
        verify(payloadDataManager).getAccounts();
        assertEquals(list, result);
    }

    @Test
    public void getLegacyAddressList() throws Exception {
        // Arrange
        List<LegacyAddress> list = Collections.emptyList();
        when(payloadDataManager.getLegacyAddresses()).thenReturn(list);
        // Act
        List<LegacyAddress> result = subject.getLegacyAddressList();
        // Assert
        verify(payloadDataManager).getLegacyAddresses();
        assertEquals(list, result);
    }

    @Test
    public void getDefaultAccountIndex() throws Exception {
        // Arrange
        when(payloadDataManager.getDefaultAccountIndex()).thenReturn(-1);
        // Act
        int result = subject.getDefaultAccountIndex();
        // Assert
        verify(payloadDataManager).getDefaultAccountIndex();
        assertEquals(-1, result);
    }

    @Test
    public void getXpubFromIndex() throws Exception {
        // Arrange
        int index = 1337;
        String xPub = "X_PUB";
        when(payloadDataManager.getXpubFromIndex(index)).thenReturn(xPub);
        // Act
        String result = subject.getXpubFromIndex(index);
        // Assert
        verify(payloadDataManager).getXpubFromIndex(index);
        assertEquals(xPub, result);
    }

    @Test
    public void getBalanceFromAddress() throws Exception {
        // Arrange
        String address = "ADDRESS";
        BigInteger balance = BigInteger.TEN;
        when(payloadDataManager.getAddressBalance(address)).thenReturn(balance);
        // Act
        BigInteger result = subject.getBalanceFromAddress(address);
        // Assert
        verify(payloadDataManager).getAddressBalance(address);
        assertEquals(balance, result);
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void handlePrivateKeyWhenKeyIsNull() throws Exception {
        // Arrange

        // Act
        subject.handlePrivateKey(null, null);
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void handlePrivateKeyExistingAddressSuccess() throws Exception {
        // Arrange
        ECKey mockECKey = mock(ECKey.class);
        when(mockECKey.hasPrivKey()).thenReturn(true);
        LegacyAddress legacyAddress = new LegacyAddress();
        when(payloadDataManager.setKeyForLegacyAddress(mockECKey, null))
                .thenReturn(Observable.just(legacyAddress));
        // Act
        subject.handlePrivateKey(mockECKey, null);
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_OK));
        verify(activity).onUpdateAccountsList();
        verify(activity).showRenameImportedAddressDialog(legacyAddress);
        verifyNoMoreInteractions(activity);
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void handlePrivateKeyExistingAddressFailure() throws Exception {
        // Arrange
        ECKey mockECKey = mock(ECKey.class);
        when(mockECKey.hasPrivKey()).thenReturn(true);
        when(payloadDataManager.setKeyForLegacyAddress(mockECKey, null))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        subject.handlePrivateKey(mockECKey, null);
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

}