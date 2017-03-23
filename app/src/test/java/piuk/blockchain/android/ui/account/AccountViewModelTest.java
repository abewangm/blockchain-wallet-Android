package piuk.blockchain.android.ui.account;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;

import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.PayloadException;
import info.blockchain.wallet.payload.PayloadManager;
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
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.datamanagers.AccountDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.datamanagers.SendDataManager;
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
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
import static piuk.blockchain.android.ui.account.AccountViewModel.KEY_WARN_TRANSFER_ALL;

@SuppressWarnings({"PrivateMemberAccessBetweenOuterAndInnerClass", "AnonymousInnerClassMayBeStatic", "unchecked"})
@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class AccountViewModelTest {

    private AccountViewModel subject;
    @Mock private AccountViewModel.DataListener activity;
    @Mock private PayloadDataManager payloadDataManager;
    @Mock private AccountDataManager accountDataManager;
    @Mock private TransferFundsDataManager fundsDataManager;
    @Mock private PrefsUtil prefsUtil;
    @Mock private AppUtil appUtil;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new ApiModule(),
                new MockDataManagerModule());

        subject = new AccountViewModel(activity);
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
        when(accountDataManager.createNewAccount(anyString(), isNull()))
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
        when(accountDataManager.createNewAccount(anyString(), isNull()))
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
        when(accountDataManager.createNewAccount(anyString(), isNull()))
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
        when(accountDataManager.createNewAccount(anyString(), isNull()))
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
        when(payloadDataManager.syncPayloadWithServer())
                .thenReturn(Completable.complete());
        // Act
        subject.updateLegacyAddress(new LegacyAddress());
        // Assert
        verify(payloadDataManager).syncPayloadWithServer();
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
        when(payloadDataManager.syncPayloadWithServer())
                .thenReturn(Completable.error(new Throwable()));
        // Act
        subject.updateLegacyAddress(new LegacyAddress());
        // Assert
        verify(payloadDataManager).syncPayloadWithServer();
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
        when(accountDataManager.getKeyFromImportedData(anyString(), anyString()))
                .thenReturn(Observable.just(mock(ECKey.class)));
        // Act
        subject.onAddressScanned("L1FQxC7wmmRNNe2YFPNXscPq3kaheiA4T7SnTr7vYSBW7Jw1A7PD");
        // Assert
        verify(accountDataManager).getKeyFromImportedData(anyString(), anyString());
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
    }

    @Test
    public void onAddressScannedNonBip38Failure() throws Exception {
        // Arrange
        when(accountDataManager.getKeyFromImportedData(anyString(), anyString()))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        subject.onAddressScanned("L1FQxC7wmmRNNe2YFPNXscPq3kaheiA4T7SnTr7vYSBW7Jw1A7PD");
        when(accountDataManager.getKeyFromImportedData(anyString(), anyString()))
                .thenReturn(Observable.just(mock(ECKey.class)));
        // Assert
        verify(accountDataManager).getKeyFromImportedData(anyString(), anyString());
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
        when(accountDataManager.updateLegacyAddress(any(LegacyAddress.class)))
                .thenReturn(Completable.complete());
        // Act
        subject.confirmImportWatchOnly(address);
        // Assert
        //noinspection WrongConstant
        verify(accountDataManager).updateLegacyAddress(any(LegacyAddress.class));
        verifyNoMoreInteractions(accountDataManager);
        verify(activity).showRenameImportedAddressDialog(any(LegacyAddress.class));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void confirmImportWatchOnlyFailure() throws Exception {
        // Arrange
        String address = "17UovdU9ZvepPe75igTQwxqNME1HbnvMB7";
        when(accountDataManager.updateLegacyAddress(any(LegacyAddress.class)))
                .thenReturn(Completable.error(new Throwable()));
        // Act
        subject.confirmImportWatchOnly(address);
        // Assert
        verify(accountDataManager).updateLegacyAddress(any(LegacyAddress.class));
        verifyNoMoreInteractions(accountDataManager);
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
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
        when(accountDataManager.setKeyForLegacyAddress(mockECKey, null))
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
        when(accountDataManager.setKeyForLegacyAddress(mockECKey, null))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        subject.handlePrivateKey(mockECKey, null);
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
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
        protected AppUtil provideAppUtil() {
            return appUtil;
        }
    }

    private class MockDataManagerModule extends DataManagerModule {
        @Override
        protected TransferFundsDataManager provideTransferFundsDataManager(PayloadDataManager payloadDataManager,
                                                                           SendDataManager sendDataManager,
                                                                           DynamicFeeCache dynamicFeeCache) {
            return fundsDataManager;
        }

        @Override
        protected AccountDataManager provideAccountDataManager(PayloadManager payloadManager,
                                                               PrivateKeyFactory privateKeyFactory) {
            return accountDataManager;
        }

        @Override
        protected PayloadDataManager providePayloadDataManager(PayloadManager payloadManager) {
            return payloadDataManager;
        }
    }

}