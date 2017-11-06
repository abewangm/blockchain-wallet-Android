package piuk.blockchain.android.ui.account;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import info.blockchain.api.data.UnspentOutputs;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payload.data.Options;
import info.blockchain.wallet.payload.data.Wallet;
import info.blockchain.wallet.payment.SpendableUnspentOutputs;
import info.blockchain.wallet.util.PrivateKeyFactory;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.subjects.ReplaySubject;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.auth.AuthDataManager;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.payments.SendDataManager;
import piuk.blockchain.android.data.settings.SettingsDataManager;
import piuk.blockchain.android.data.walletoptions.WalletOptionsDataManager;
import piuk.blockchain.android.data.walletoptions.WalletOptionsState;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.send.PendingTransaction;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper;
import piuk.blockchain.android.ui.zxing.CaptureActivity;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class AccountEditPresenterTest {

    private AccountEditPresenter subject;
    @Mock private AccountEditView activity;
    @Mock private PayloadDataManager payloadDataManager;
    @Mock private PrefsUtil prefsUtil;
    @Mock private StringUtils stringUtils;
    @Mock private ExchangeRateFactory exchangeRateFactory;
    @Mock private AccountEditModel accountEditModel;
    @Mock private SwipeToReceiveHelper swipeToReceiveHelper;
    @Mock private SendDataManager sendDataManager;
    @Mock private PrivateKeyFactory privateKeyFactory;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private DynamicFeeCache dynamicFeeCache;
    @Mock private WalletOptionsDataManager walletOptionsDataManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        subject = new AccountEditPresenter(prefsUtil,
                stringUtils,
                payloadDataManager,
                exchangeRateFactory,
                sendDataManager,
                privateKeyFactory,
                swipeToReceiveHelper,
                dynamicFeeCache,
                walletOptionsDataManager);
        subject.initView(activity);
        subject.setAccountModel(accountEditModel);
    }

    @Test
    public void setAccountModel() throws Exception {
        // Arrange
        AccountEditModel newModel = new AccountEditModel(mock(Context.class));
        // Act
        subject.setAccountModel(newModel);
        // Assert
        assertEquals(newModel, subject.accountModel);
    }


    @SuppressWarnings("WrongConstant")
    @Test
    public void onViewReadyV3() throws Exception {
        // Arrange
        Intent intent = new Intent();
        intent.putExtra("account_index", 0);
        when(activity.getIntent()).thenReturn(intent);
        Wallet mockPayload = mock(Wallet.class, RETURNS_DEEP_STUBS);
        Account importedAccount = mock(Account.class);
        when(importedAccount.getXpub()).thenReturn("");
        Account account = mock(Account.class);
        when(account.getXpub()).thenReturn("");
        when(account.getLabel()).thenReturn("");
        when(mockPayload.getHdWallets().get(0).getAccounts())
                .thenReturn(Arrays.asList(account, importedAccount));
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        when(stringUtils.getString(anyInt())).thenReturn("string resource");
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getIntent();
        verify(accountEditModel).setLabel(anyString());
        verify(accountEditModel).setLabelHeader("string resource");
        verify(accountEditModel).setScanPrivateKeyVisibility(anyInt());
        verify(accountEditModel).setXpubText("string resource");
        verify(accountEditModel).setTransferFundsVisibility(anyInt());
    }

    @SuppressWarnings("WrongConstant")
    @Test
    public void onViewReadyV3Archived() throws Exception {
        // Arrange
        Intent intent = new Intent();
        intent.putExtra("account_index", 0);
        when(activity.getIntent()).thenReturn(intent);
        Wallet mockPayload = mock(Wallet.class, RETURNS_DEEP_STUBS);
        Account importedAccount = mock(Account.class);
        when(importedAccount.getXpub()).thenReturn("");
        Account account = mock(Account.class);
        when(account.getXpub()).thenReturn("");
        when(account.getLabel()).thenReturn("");
        when(account.isArchived()).thenReturn(true);
        when(mockPayload.getHdWallets().get(0).getAccounts())
                .thenReturn(Arrays.asList(account, importedAccount));
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        when(stringUtils.getString(anyInt())).thenReturn("string resource");
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getIntent();
        verify(accountEditModel).setLabel(anyString());
        verify(accountEditModel).setLabelHeader("string resource");
        verify(accountEditModel).setScanPrivateKeyVisibility(anyInt());
        verify(accountEditModel).setXpubText("string resource");
        verify(accountEditModel).setTransferFundsVisibility(anyInt());
    }

    @Test
    public void onClickTransferFundsSuccess() throws Exception {
        // Arrange
        LegacyAddress legacyAddress = new LegacyAddress();
        legacyAddress.setAddress("");
        legacyAddress.setLabel("");
        subject.legacyAddress = legacyAddress;
        Pair<BigInteger, BigInteger> sweepableCoins = Pair.of(BigInteger.ONE, BigInteger.TEN);
        when(dynamicFeeCache.getBtcFeeOptions().getRegularFee()).thenReturn(100L);
        when(payloadDataManager.getDefaultAccount()).thenReturn(mock(Account.class));
        when(payloadDataManager.getNextReceiveAddress(any(Account.class)))
                .thenReturn(Observable.just("address"));
        when(sendDataManager.getUnspentOutputs(legacyAddress.getAddress()))
                .thenReturn(Observable.just(mock(UnspentOutputs.class)));
        when(sendDataManager.getMaximumAvailable(any(UnspentOutputs.class), any(BigInteger.class), any(Boolean.class)))
                .thenReturn(sweepableCoins);
        SpendableUnspentOutputs spendableUnspentOutputs = mock(SpendableUnspentOutputs.class);
        when(spendableUnspentOutputs.getAbsoluteFee()).thenReturn(BigInteger.TEN);
        when(spendableUnspentOutputs.getConsumedAmount()).thenReturn(BigInteger.TEN);
        when(sendDataManager.getSpendableCoins(any(UnspentOutputs.class), any(BigInteger.class), any(BigInteger.class), any(Boolean.class)))
                .thenReturn(spendableUnspentOutputs);
        when(exchangeRateFactory.getLastBtcPrice(anyString())).thenReturn(100.0d);
        when(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD");
        when(sendDataManager.estimateSize(anyInt(), anyInt())).thenReturn(1337);
        // Act
        subject.onClickTransferFunds();
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        verify(activity).showPaymentDetails(any(PaymentConfirmationDetails.class));
    }

    @Test
    public void onClickTransferFundsSuccessTransactionEmpty() throws Exception {
        // Arrange
        LegacyAddress legacyAddress = new LegacyAddress();
        legacyAddress.setAddress("");
        legacyAddress.setLabel("");
        subject.legacyAddress = legacyAddress;
        Pair<BigInteger, BigInteger> sweepableCoins = Pair.of(BigInteger.ZERO, BigInteger.TEN);
        when(dynamicFeeCache.getBtcFeeOptions().getRegularFee()).thenReturn(100L);
        when(payloadDataManager.getDefaultAccount()).thenReturn(mock(Account.class));
        when(payloadDataManager.getNextReceiveAddress(any(Account.class)))
                .thenReturn(Observable.just("address"));
        when(sendDataManager.getUnspentOutputs(legacyAddress.getAddress()))
                .thenReturn(Observable.just(mock(UnspentOutputs.class)));
        when(sendDataManager.getMaximumAvailable(any(UnspentOutputs.class), any(BigInteger.class), any(Boolean.class)))
                .thenReturn(sweepableCoins);
        when(sendDataManager.getSpendableCoins(any(UnspentOutputs.class), any(BigInteger.class), any(BigInteger.class), any(Boolean.class)))
                .thenReturn(mock(SpendableUnspentOutputs.class));
        // Act
        subject.onClickTransferFunds();
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void onClickTransferFundsError() throws Exception {
        // Arrange
        LegacyAddress legacyAddress = new LegacyAddress();
        legacyAddress.setAddress("");
        legacyAddress.setLabel("");
        subject.legacyAddress = legacyAddress;
        when(sendDataManager.getUnspentOutputs(legacyAddress.getAddress()))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        subject.onClickTransferFunds();
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void transferFundsClickable() throws Exception {
        // Arrange
        when(accountEditModel.getTransferFundsClickable()).thenReturn(false);
        // Act
        boolean value = subject.transferFundsClickable();
        // Assert
        assertFalse(value);
    }

    @Test
    public void submitPaymentSuccess() throws Exception {
        // Arrange
        PendingTransaction pendingTransaction = new PendingTransaction();
        pendingTransaction.bigIntAmount = new BigInteger("1");
        pendingTransaction.bigIntFee = new BigInteger("1");
        LegacyAddress legacyAddress = new LegacyAddress();
        pendingTransaction.sendingObject = new ItemAccount("", "", "", null, legacyAddress, null);
        pendingTransaction.unspentOutputBundle = new SpendableUnspentOutputs();
        Wallet mockPayload = mock(Wallet.class, RETURNS_DEEP_STUBS);
        when(mockPayload.isDoubleEncryption()).thenReturn(false);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        when(payloadDataManager.getAddressECKey(legacyAddress, null))
                .thenReturn(mock(ECKey.class));
        when(sendDataManager.submitPayment(
                any(SpendableUnspentOutputs.class),
                anyList(),
                isNull(),
                isNull(),
                any(BigInteger.class),
                any(BigInteger.class),
                any(Boolean.class))).thenReturn(Observable.just("hash"));
        when(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete());
        subject.pendingTransaction = pendingTransaction;
        // Act
        subject.submitPayment();
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        verify(activity).showTransactionSuccess();
        //noinspection WrongConstant
        verify(accountEditModel).setTransferFundsVisibility(anyInt());
        verify(activity).setActivityResult(anyInt());
    }

    @Test
    public void submitPaymentFailed() throws Exception {
        // Arrange
        PendingTransaction pendingTransaction = new PendingTransaction();
        pendingTransaction.bigIntAmount = new BigInteger("1");
        pendingTransaction.bigIntFee = new BigInteger("1");
        LegacyAddress legacyAddress = new LegacyAddress();
        pendingTransaction.sendingObject = new ItemAccount("", "", "", null, legacyAddress, null);
        pendingTransaction.unspentOutputBundle = new SpendableUnspentOutputs();
        Wallet mockPayload = mock(Wallet.class, RETURNS_DEEP_STUBS);
        when(mockPayload.isDoubleEncryption()).thenReturn(false);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        when(payloadDataManager.getAddressECKey(eq(legacyAddress), anyString()))
                .thenReturn(mock(ECKey.class));
        when(sendDataManager.submitPayment(
                any(SpendableUnspentOutputs.class),
                anyList(),
                isNull(),
                isNull(),
                any(BigInteger.class),
                any(BigInteger.class),
                any(Boolean.class))).thenReturn(Observable.error(new Throwable()));
        subject.pendingTransaction = pendingTransaction;
        // Act
        subject.submitPayment();
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void submitPaymentECKeyException() throws Exception {
        // Arrange
        PendingTransaction pendingTransaction = new PendingTransaction();
        pendingTransaction.bigIntAmount = new BigInteger("1");
        pendingTransaction.bigIntFee = new BigInteger("1");
        LegacyAddress legacyAddress = new LegacyAddress();
        pendingTransaction.sendingObject = new ItemAccount("", "", "", null, legacyAddress, null);
        Wallet mockPayload = mock(Wallet.class, RETURNS_DEEP_STUBS);
        when(mockPayload.isDoubleEncryption()).thenReturn(true);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        subject.pendingTransaction = pendingTransaction;
        // Act
        subject.submitPayment();
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void updateAccountLabelInvalid() throws Exception {
        // Arrange

        // Act
        subject.updateAccountLabel("    ");
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void updateAccountLabelSuccess() throws Exception {
        // Arrange
        subject.account = new Account();
        when(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete());
        // Act
        subject.updateAccountLabel("label");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        verify(accountEditModel).setLabel(anyString());
        verify(activity).setActivityResult(anyInt());
    }

    @Test
    public void updateAccountLabelFailed() throws Exception {
        // Arrange
        subject.legacyAddress = new LegacyAddress();
        subject.legacyAddress.setLabel("old label");
        when(payloadDataManager.syncPayloadWithServer())
                .thenReturn(Completable.error(new Throwable()));
        // Act
        subject.updateAccountLabel("new label");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        verify(accountEditModel).setLabel("old label");
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void onClickChangeLabel() throws Exception {
        // Arrange
        when(accountEditModel.getLabel()).thenReturn("label");
        // Act
        subject.onClickChangeLabel(null);
        // Assert
        verify(activity).promptAccountLabel("label");
    }

    @Test
    public void onClickDefaultSuccess() throws Exception {
        // Arrange
        Account account = new Account();
        account.setXpub("");
        subject.account = account;
        Wallet mockPayload = mock(Wallet.class, RETURNS_DEEP_STUBS);
        when(payloadDataManager.getDefaultAccountIndex()).thenReturn(0);
        when(mockPayload.getHdWallets().get(0).getAccounts())
                .thenReturn(Collections.singletonList(account));
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        when(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete());
        // Act
        subject.onClickDefault(null);
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        verify(activity).setActivityResult(anyInt());
        verify(activity).updateAppShortcuts();
    }

    @Test
    public void onClickDefaultFailure() throws Exception {
        // Arrange
        Account account = new Account();
        account.setXpub("");
        subject.account = account;
        Wallet mockPayload = mock(Wallet.class, RETURNS_DEEP_STUBS);
        when(payloadDataManager.getDefaultAccountIndex()).thenReturn(0);
        when(mockPayload.getHdWallets().get(0).getAccounts())
                .thenReturn(Collections.singletonList(account));
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        when(payloadDataManager.syncPayloadWithServer())
                .thenReturn(Completable.error(new Throwable()));
        // Act
        subject.onClickDefault(null);
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void onClickScanXpriv() throws Exception {
        // Arrange
        subject.legacyAddress = new LegacyAddress();
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.isDoubleEncryption()).thenReturn(false);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        // Act
        subject.onClickScanXpriv(null);
        // Assert
        verify(activity).startScanActivity();
    }

    @Test
    public void onClickScanXprivDoubleEncrypted() throws Exception {
        // Arrange
        subject.legacyAddress = new LegacyAddress();
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.isDoubleEncryption()).thenReturn(true);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        when(stringUtils.getString(R.string.watch_only_spend_instructionss)).thenReturn("%1$s");
        // Act
        subject.onClickScanXpriv(null);
        // Assert
        verify(activity).promptPrivateKey(anyString());
    }

    @Test
    public void onClickShowXpubAccount() throws Exception {
        // Arrange
        subject.account = new Account();
        // Act
        subject.onClickShowXpub(null);
        // Assert
        verify(activity).showXpubSharingWarning();
    }

    @Test
    public void onClickShowXpubLegacyAddress() throws Exception {
        // Arrange
        subject.legacyAddress = new LegacyAddress();
        // Act
        subject.onClickShowXpub(null);
        // Assert
        verify(activity).showAddressDetails(isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    public void onClickArchive() throws Exception {
        // Arrange
        subject.account = new Account();
        when(stringUtils.getString(anyInt())).thenReturn("resource string");
        // Act
        subject.onClickArchive(null);
        // Assert
        verify(activity).promptArchive("resource string", "resource string");
    }

    @Test
    public void showAddressDetails() throws Exception {
        // Arrange
        subject.legacyAddress = new LegacyAddress();
        // Act
        subject.showAddressDetails();
        // Assert
        verify(activity).showAddressDetails(isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    public void handleIncomingScanIntentInvalidData() throws Exception {
        // Arrange
        Intent intent = new Intent();
        intent.putExtra(CaptureActivity.SCAN_RESULT, (String[]) null);
        // Act
        subject.handleIncomingScanIntent(intent);
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void handleIncomingScanIntentUnrecognisedKeyFormat() throws Exception {
        // Arrange
        Intent intent = new Intent();
        intent.putExtra(CaptureActivity.SCAN_RESULT, "6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pS");
        when(privateKeyFactory.getFormat(anyString())).thenReturn(null);
        // Act
        subject.handleIncomingScanIntent(intent);
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void handleIncomingScanIntentBip38() throws Exception {
        // Arrange
        Intent intent = new Intent();
        intent.putExtra(CaptureActivity.SCAN_RESULT, "6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS");
        when(privateKeyFactory.getFormat(anyString())).thenReturn(PrivateKeyFactory.BIP38);
        // Act
        subject.handleIncomingScanIntent(intent);
        // Assert
        verify(activity).promptBIP38Password(anyString());
    }

    @Test
    public void handleIncomingScanIntentNonBip38NoKey() throws Exception {
        // Arrange
        Intent intent = new Intent();
        intent.putExtra(CaptureActivity.SCAN_RESULT, "L1FQxC7wmmRNNe2YFPNXscPq3kaheiA4T7SnTr7vYSBW7Jw1A7PD");
        when(privateKeyFactory.getFormat(anyString())).thenReturn(PrivateKeyFactory.BASE58);
        // Act
        subject.handleIncomingScanIntent(intent);
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void handleIncomingScanIntentNonBip38WithKey() throws Exception {
        // Arrange
        LegacyAddress legacyAddress = new LegacyAddress();
        legacyAddress.setAddress("");
        subject.legacyAddress = legacyAddress;
        Intent intent = new Intent();
        intent.putExtra(CaptureActivity.SCAN_RESULT, "L1FQxC7wmmRNNe2YFPNXscPq3kaheiA4T7SnTr7vYSBW7Jw1A7PD");
        when(privateKeyFactory.getFormat(anyString())).thenReturn(PrivateKeyFactory.BASE58);
        when(privateKeyFactory.getKey(anyString(), anyString())).thenReturn(new ECKey());
        // Act
        subject.handleIncomingScanIntent(intent);
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void setSecondPassword() throws Exception {
        // Arrange

        // Act
        subject.setSecondPassword("password");
        // Assert
        assertEquals("password", subject.secondPassword);
    }

    @Test
    public void archiveAccountSuccess() throws Exception {
        // Arrange
        subject.account = new Account();
        when(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete());
        when(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete());
        // Act
        subject.archiveAccount();
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        verify(activity).setActivityResult(anyInt());
        verify(payloadDataManager).syncPayloadWithServer();
        verify(payloadDataManager).updateAllTransactions();
    }

    @Test
    public void archiveAccountFailed() throws Exception {
        // Arrange
        subject.account = new Account();
        when(payloadDataManager.syncPayloadWithServer())
                .thenReturn(Completable.error(new Throwable()));
        // Act
        subject.archiveAccount();
        // Assert
        verify(payloadDataManager).syncPayloadWithServer();
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void importBIP38AddressError() throws Exception {
        // Arrange

        // Act
        subject.importBIP38Address("", "");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(activity).dismissProgressDialog();
    }

    @Test
    public void importBIP38AddressValidAddressEmptyKey() throws Exception {
        // Arrange

        // Act
        subject.importBIP38Address("6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS", "");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(activity).dismissProgressDialog();
    }

    @Ignore("Cannot decrypt key in test VM")
    @Test
    public void importBIP38AddressValidAddressWithKey() throws Exception {
        // Arrange
        LegacyAddress legacyAddress = new LegacyAddress();
        legacyAddress.setAddress("");
        subject.legacyAddress = legacyAddress;
        // Act
        subject.importBIP38Address("6PYX4iD7a39UeAsd7RQiwHFjgbRwJVLhfEHxcvTD4HPKxK1JSnkPZ7jben", "password");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(activity).dismissProgressDialog();
    }

    @SuppressLint("VisibleForTests")
    @SuppressWarnings("WrongConstant")
    @Test
    public void importAddressPrivateKeySuccessMatchesIntendedAddressNoDoubleEncryption() throws Exception {
        // Arrange
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.isDoubleEncryption()).thenReturn(false);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        ECKey mockEcKey = mock(ECKey.class);
        when(mockEcKey.getPrivKeyBytes()).thenReturn("privkey".getBytes());
        when(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete());
        // Act
        subject.importAddressPrivateKey(mockEcKey, new LegacyAddress(), true);
        // Assert
        verify(activity).setActivityResult(anyInt());
        verify(accountEditModel).setScanPrivateKeyVisibility(anyInt());
        verify(accountEditModel).setArchiveVisibility(anyInt());
        verify(activity).privateKeyImportSuccess();
    }

    @SuppressLint("VisibleForTests")
    @SuppressWarnings("WrongConstant")
    @Test
    public void importAddressPrivateKeySuccessNoAddressMatchDoubleEncryption() throws Exception {
        // Arrange
        subject.setSecondPassword("password");
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.isDoubleEncryption()).thenReturn(true);
        Options mockOptions = mock(Options.class);
        when(mockOptions.getPbkdf2Iterations()).thenReturn(1);
        when(mockPayload.getOptions()).thenReturn(mockOptions);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        ECKey mockEcKey = mock(ECKey.class);
        when(mockEcKey.getPrivKeyBytes()).thenReturn("privkey".getBytes());
        when(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete());
        // Act
        subject.importAddressPrivateKey(mockEcKey, new LegacyAddress(), false);
        // Assert
        verify(activity).setActivityResult(anyInt());
        verify(accountEditModel).setScanPrivateKeyVisibility(anyInt());
        verify(accountEditModel).setArchiveVisibility(anyInt());
        verify(activity).privateKeyImportMismatch();
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void importAddressPrivateKeyFailed() throws Exception {
        // Arrange
        subject.setSecondPassword("password");
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.isDoubleEncryption()).thenReturn(true);
        Options mockOptions = mock(Options.class);
        when(mockOptions.getPbkdf2Iterations()).thenReturn(1);
        when(mockPayload.getOptions()).thenReturn(mockOptions);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        ECKey mockEcKey = mock(ECKey.class);
        when(mockEcKey.getPrivKeyBytes()).thenReturn("privkey".getBytes());
        when(payloadDataManager.syncPayloadWithServer())
                .thenReturn(Completable.error(new Throwable()));
        // Act
        subject.importAddressPrivateKey(mockEcKey, new LegacyAddress(), false);
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void importUnmatchedPrivateKeyFoundInPayloadSuccess() throws Exception {
        // Arrange
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.isDoubleEncryption()).thenReturn(false);
        List<String> legacyStrings = Arrays.asList("addr0", "addr1", "addr2");
        when(mockPayload.getLegacyAddressStringList()).thenReturn(legacyStrings);
        LegacyAddress legacyAddress = new LegacyAddress();
        legacyAddress.setAddress("addr0");
        List<LegacyAddress> legacyAddresses = Collections.singletonList(legacyAddress);
        when(mockPayload.getLegacyAddressList()).thenReturn(legacyAddresses);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        ECKey mockEcKey = mock(ECKey.class);
        when(mockEcKey.getPrivKeyBytes()).thenReturn("privkey".getBytes());
        Address mockAddress = mock(Address.class);
        when(mockAddress.toString()).thenReturn("addr0");
        when(mockEcKey.toAddress(any(NetworkParameters.class))).thenReturn(mockAddress);
        when(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete());
        // Act
        subject.importUnmatchedPrivateKey(mockEcKey);
        // Assert
        verify(activity).setActivityResult(anyInt());
        //noinspection WrongConstant
        verify(accountEditModel).setScanPrivateKeyVisibility(anyInt());
        verify(activity).privateKeyImportMismatch();
    }

    @Test
    public void importUnmatchedPrivateNotFoundInPayloadSuccess() throws Exception {
        // Arrange
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.isDoubleEncryption()).thenReturn(false);
        when(mockPayload.getLegacyAddressList()).thenReturn(new ArrayList<>());
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        ECKey ecKey = new ECKey();
        Address mockAddress = mock(Address.class);
        when(mockAddress.toString()).thenReturn("addr0");
        when(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete());
        // Act
        subject.importUnmatchedPrivateKey(ecKey);
        // Assert
        verify(activity).setActivityResult(anyInt());
        verify(activity).sendBroadcast(anyString(), anyString());
        //noinspection WrongConstant
        verify(activity).privateKeyImportMismatch();
    }

    @SuppressWarnings("WrongConstant")
    @Test
    public void importUnmatchedPrivateNotFoundInPayloadFailure() throws Exception {
        // Arrange
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.isDoubleEncryption()).thenReturn(false);
        when(mockPayload.getLegacyAddressList()).thenReturn(new ArrayList<>());
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        ECKey ecKey = new ECKey();
        Address mockAddress = mock(Address.class);
        when(mockAddress.toString()).thenReturn("addr0");
        when(payloadDataManager.syncPayloadWithServer())
                .thenReturn(Completable.error(new Throwable()));
        // Act
        subject.importUnmatchedPrivateKey(ecKey);
        // Assert
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(activity).privateKeyImportMismatch();
    }

}