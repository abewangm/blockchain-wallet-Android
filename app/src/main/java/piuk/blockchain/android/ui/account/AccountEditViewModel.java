package piuk.blockchain.android.ui.account;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.View;

import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.HDWallet;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payload.data.Wallet;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.PrivateKeyFactory;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.params.MainNetParams;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.datamanagers.AccountEditDataManager;
import piuk.blockchain.android.data.datamanagers.SendDataManager;
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.send.PendingTransaction;
import piuk.blockchain.android.ui.send.SendModel;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.ui.zxing.CaptureActivity;
import piuk.blockchain.android.ui.zxing.Contents;
import piuk.blockchain.android.ui.zxing.encode.QRCodeEncoder;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

@SuppressWarnings("WeakerAccess")
public class AccountEditViewModel extends BaseViewModel {

    private static final String TAG = AccountEditViewModel.class.getSimpleName();

    private DataListener dataListener;

    @Inject protected PayloadManager payloadManager;
    @Inject protected PrefsUtil prefsUtil;
    @Inject protected StringUtils stringUtils;
    @Inject protected AccountEditDataManager accountEditDataManager;
    @Inject protected PayloadDataManager payloadDataManager;
    @Inject protected ExchangeRateFactory exchangeRateFactory;
    @Inject protected SendDataManager sendDataManager;
    @Inject protected PrivateKeyFactory privateKeyFactory;
    @Inject protected SwipeToReceiveHelper swipeToReceiveHelper;
    @Inject protected DynamicFeeCache dynamicFeeCache;

    // Visible for data binding
    public AccountEditModel accountModel;

    @VisibleForTesting LegacyAddress legacyAddress;
    @VisibleForTesting Account account;
    @VisibleForTesting String secondPassword;
    private MonetaryUtil monetaryUtil;
    private int accountIndex;

    public interface DataListener {

        Intent getIntent();

        void promptAccountLabel(@Nullable String currentLabel);

        void showToast(@StringRes int message, @ToastCustom.ToastType String type);

        void setActivityResult(int resultCode);

        void startScanActivity();

        void promptPrivateKey(String message);

        void promptArchive(String title, String message);

        void promptBIP38Password(String data);

        void privateKeyImportMismatch();

        void privateKeyImportSuccess();

        void showXpubSharingWarning();

        void showAddressDetails(String heading, String note, String copy, Bitmap bitmap, String qrString);

        void showPaymentDetails(PaymentConfirmationDetails details, PendingTransaction pendingTransaction);

        void showTransactionSuccess();

        void showProgressDialog(@StringRes int message);

        void dismissProgressDialog();

        void sendBroadcast(String key, String data);

        void updateAppShortcuts();
    }

    AccountEditViewModel(AccountEditModel accountModel, DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;

        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));

        this.accountModel = accountModel;
    }

    @SuppressWarnings("unused")
    public void setAccountModel(AccountEditModel accountModel) {
        this.accountModel = accountModel;
    }

    @Override
    public void onViewReady() {
        Intent intent = dataListener.getIntent();

        int accountIndex = intent.getIntExtra("account_index", -1);
        int addressIndex = intent.getIntExtra("address_index", -1);

        if (accountIndex >= 0) {
            // V3
            List<Account> accounts = payloadManager.getPayload().getHdWallets().get(0).getAccounts();

            // Remove "All"
            List<Account> accountClone = new ArrayList<>(accounts.size());
            accountClone.addAll(accounts);

            if (accountClone.get(accountClone.size() - 1) instanceof ConsolidatedAccount) {
                accountClone.remove(accountClone.size() - 1);
            }

            account = accountClone.get(accountIndex);

            accountModel.setLabel(account.getLabel());
            accountModel.setLabelHeader(stringUtils.getString(R.string.name));
            accountModel.setScanPrivateKeyVisibility(View.GONE);
            accountModel.setXpubDescriptionVisibility(View.VISIBLE);
            accountModel.setXpubText(stringUtils.getString(R.string.extended_public_key));
            accountModel.setTransferFundsVisibility(View.GONE);
            setArchive(account.isArchived());
            setDefault(isDefault(account));

        } else if (addressIndex >= 0) {
            // V2
            ConsolidatedAccount iAccount = null;
            if (!payloadManager.getPayload().getLegacyAddressList().isEmpty()) {

                iAccount = new ConsolidatedAccount(stringUtils.getString(R.string.imported_addresses),
                        payloadManager.getPayload().getLegacyAddressList(),
                        payloadManager.getImportedAddressesBalance().longValue());
            }

            if (iAccount != null) {

                List<LegacyAddress> legacy = iAccount.getLegacyAddresses();
                legacyAddress = legacy.get(addressIndex);

                accountModel.setLabel(legacyAddress.getLabel());
                accountModel.setLabelHeader(stringUtils.getString(R.string.name));
                accountModel.setXpubDescriptionVisibility(View.GONE);
                accountModel.setXpubText(stringUtils.getString(R.string.address));
                accountModel.setDefaultAccountVisibility(View.GONE);//No default for V2
                setArchive(legacyAddress.getTag() == LegacyAddress.ARCHIVED_ADDRESS);

                if (legacyAddress.isWatchOnly()) {
                    accountModel.setScanPrivateKeyVisibility(View.VISIBLE);
                    accountModel.setArchiveVisibility(View.GONE);
                } else {
                    accountModel.setScanPrivateKeyVisibility(View.GONE);
                    accountModel.setArchiveVisibility(View.VISIBLE);
                }

                if (payloadManager.getPayload().isUpgraded()) {

                    // Subtract fee
                    long balanceAfterFee = payloadManager.getAddressBalance(
                            legacyAddress.getAddress()).longValue() -
                            sendDataManager.estimatedFee(1, 1,
                                    BigDecimal.valueOf(dynamicFeeCache.getCachedDynamicFee()
                                            .getDefaultFee()
                                            .getFee())
                                            .toBigInteger())
                                    .longValue();

                    if (balanceAfterFee > Payment.DUST.longValue() && !legacyAddress.isWatchOnly()) {
                        accountModel.setTransferFundsVisibility(View.VISIBLE);
                    } else {
                        // No need to show 'transfer' if funds are less than dust amount
                        accountModel.setTransferFundsVisibility(View.GONE);
                    }
                } else {
                    // No transfer option for V2
                    accountModel.setTransferFundsVisibility(View.GONE);
                }
            }
        }
    }

    public boolean areLauncherShortcutsEnabled() {
        return prefsUtil.getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true);
    }

    private void setDefault(boolean isDefault) {
        if (isDefault) {
            accountModel.setDefaultAccountVisibility(View.GONE);
            accountModel.setArchiveAlpha(0.5f);
            accountModel.setArchiveText(stringUtils.getString(R.string.default_account_description));
            accountModel.setArchiveClickable(false);
        } else {
            accountModel.setDefaultAccountVisibility(View.VISIBLE);
            accountModel.setDefaultText(stringUtils.getString(R.string.make_default));
            accountModel.setDefaultTextColor(R.color.primary_blue_accent);
        }
    }

    private boolean isDefault(Account account) {
        int defaultIndex = payloadManager.getPayload().getHdWallets().get(0).getDefaultAccountIdx();
        List<Account> accounts = payloadManager.getPayload().getHdWallets().get(0).getAccounts();

        int accountIndex = 0;
        for (Account acc : accounts) {

            if (acc.getXpub().equals(account.getXpub())) {
                this.accountIndex = accountIndex;//sets this account index

                if (accountIndex == defaultIndex) {//this is current default already
                    return true;
                }
            }

            accountIndex++;
        }
        return false;
    }

    private void setArchive(boolean isArchived) {
        if (isArchived) {
            accountModel.setArchiveHeader(stringUtils.getString(R.string.unarchive));
            accountModel.setArchiveText(stringUtils.getString(R.string.archived_description));
            accountModel.setArchiveAlpha(1.0f);
            accountModel.setArchiveVisibility(View.VISIBLE);
            accountModel.setArchiveClickable(true);

            accountModel.setLabelAlpha(0.5f);
            accountModel.setLabelClickable(false);
            accountModel.setXpubAlpha(0.5f);
            accountModel.setXpubClickable(false);
            accountModel.setXprivAlpha(0.5f);
            accountModel.setXprivClickable(false);
            accountModel.setDefaultAlpha(0.5f);
            accountModel.setDefaultClickable(false);
            accountModel.setTransferFundsAlpha(0.5f);
            accountModel.setTransferFundsClickable(false);
        } else {
            // Don't allow archiving of default account
            if (isArchivable()) {
                accountModel.setArchiveAlpha(1.0f);
                accountModel.setArchiveVisibility(View.VISIBLE);
                accountModel.setArchiveText(stringUtils.getString(R.string.not_archived_description));
                accountModel.setArchiveClickable(true);
            } else {
                accountModel.setArchiveVisibility(View.VISIBLE);
                accountModel.setArchiveAlpha(0.5f);
                accountModel.setArchiveText(stringUtils.getString(R.string.default_account_description));
                accountModel.setArchiveClickable(false);
            }

            accountModel.setArchiveHeader(stringUtils.getString(R.string.archive));

            accountModel.setLabelAlpha(1.0f);
            accountModel.setLabelClickable(true);
            accountModel.setXpubAlpha(1.0f);
            accountModel.setXpubClickable(true);
            accountModel.setXprivAlpha(1.0f);
            accountModel.setXprivClickable(true);
            accountModel.setDefaultAlpha(1.0f);
            accountModel.setDefaultClickable(true);
            accountModel.setTransferFundsAlpha(1.0f);
            accountModel.setTransferFundsClickable(true);
        }
    }

    private boolean isArchivable() {

        Wallet payload = payloadManager.getPayload();

        if (payload.isUpgraded()) {
            //V3 - can't archive default account
            HDWallet hdWallet = payload.getHdWallets().get(0);

            int defaultIndex = hdWallet.getDefaultAccountIdx();
            Account defaultAccount = hdWallet.getAccounts().get(defaultIndex);

            if (defaultAccount == account)
                return false;
        } else {
            //V2 - must have a single unarchived address
            List<LegacyAddress> allActiveLegacyAddresses = payload.getLegacyAddressList(LegacyAddress.NORMAL_ADDRESS);
            return (allActiveLegacyAddresses.size() > 1);
        }

        return true;
    }

    void onClickTransferFunds() {
        dataListener.showProgressDialog(R.string.please_wait);

        compositeDisposable.add(
                accountEditDataManager.getPendingTransactionForLegacyAddress(legacyAddress)
                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                        .subscribe(pendingTransaction -> {
                            if (pendingTransaction != null && pendingTransaction.bigIntAmount.compareTo(BigInteger.ZERO) == 1) {
                                PaymentConfirmationDetails details = getTransactionDetailsForDisplay(pendingTransaction);
                                dataListener.showPaymentDetails(details, pendingTransaction);
                            } else {
                                dataListener.showToast(R.string.insufficient_funds, ToastCustom.TYPE_ERROR);
                            }

                        }, throwable -> dataListener.showToast(R.string.insufficient_funds, ToastCustom.TYPE_ERROR)));

    }

    boolean transferFundsClickable() {
        return accountModel.getTransferFundsClickable();
    }

    private PaymentConfirmationDetails getTransactionDetailsForDisplay(PendingTransaction pendingTransaction) {
        PaymentConfirmationDetails details = new PaymentConfirmationDetails();
        details.fromLabel = pendingTransaction.sendingObject.label;
        if (pendingTransaction.receivingObject != null
                && pendingTransaction.receivingObject.label != null
                && !pendingTransaction.receivingObject.label.isEmpty()) {
            details.toLabel = pendingTransaction.receivingObject.label;
        } else {
            details.toLabel = pendingTransaction.receivingAddress;
        }

        String fiatUnit = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        String btcUnit = monetaryUtil.getBTCUnit(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        double exchangeRate = exchangeRateFactory.getLastPrice(fiatUnit);

        details.btcAmount = monetaryUtil.getDisplayAmount(pendingTransaction.bigIntAmount.longValue());
        details.btcFee = monetaryUtil.getDisplayAmount(pendingTransaction.bigIntFee.longValue());
        details.btcSuggestedFee = monetaryUtil.getDisplayAmount(pendingTransaction.bigIntFee.longValue());
        details.btcUnit = btcUnit;
        details.fiatUnit = fiatUnit;
        details.btcTotal = monetaryUtil.getDisplayAmount(pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee).longValue());

        details.fiatFee = monetaryUtil.getFiatFormat(fiatUnit)
                .format(exchangeRate * (pendingTransaction.bigIntFee.doubleValue() / 1e8));

        details.fiatAmount = monetaryUtil.getFiatFormat(fiatUnit)
                .format(exchangeRate * (pendingTransaction.bigIntAmount.doubleValue() / 1e8));

        BigInteger totalFiat = pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee);
        details.fiatTotal = monetaryUtil.getFiatFormat(fiatUnit)
                .format(exchangeRate * totalFiat.doubleValue() / 1e8);

        details.isSurge = false;
        details.isLargeTransaction = isLargeTransaction(pendingTransaction);
        details.hasConsumedAmounts = pendingTransaction.unspentOutputBundle.getConsumedAmount().compareTo(BigInteger.ZERO) == 1;

        return details;
    }

    private boolean isLargeTransaction(PendingTransaction pendingTransaction) {
        int txSize = sendDataManager.estimateSize(pendingTransaction.unspentOutputBundle.getSpendableOutputs().size(), 2);//assume change
        double relativeFee = pendingTransaction.bigIntFee.doubleValue() / pendingTransaction.bigIntAmount.doubleValue() * 100.0;

        return pendingTransaction.bigIntFee.longValue() > SendModel.LARGE_TX_FEE
                && txSize > SendModel.LARGE_TX_SIZE
                && relativeFee > SendModel.LARGE_TX_PERCENTAGE;
    }

    void submitPayment(PendingTransaction pendingTransaction) {
        dataListener.showProgressDialog(R.string.please_wait);

        LegacyAddress legacyAddress = ((LegacyAddress) pendingTransaction.sendingObject.accountObject);
        String changeAddress = legacyAddress.getAddress();

        List<ECKey> keys = new ArrayList<>();

        try {
            ECKey walletKey = payloadManager.getAddressECKey(legacyAddress, secondPassword);
            keys.add(walletKey);
        } catch (Exception e) {
            dataListener.dismissProgressDialog();
            dataListener.showToast(R.string.transaction_failed, ToastCustom.TYPE_ERROR);
            return;
        }

        compositeDisposable.add(
                accountEditDataManager.submitPayment(pendingTransaction.unspentOutputBundle,
                        keys,
                        pendingTransaction.receivingAddress,
                        changeAddress,
                        pendingTransaction.bigIntFee,
                        pendingTransaction.bigIntAmount)
                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                        .subscribe(hash -> {
                            legacyAddress.setTag(LegacyAddress.ARCHIVED_ADDRESS);
                            setArchive(true);

                            dataListener.showTransactionSuccess();

                            // Update V2 balance immediately after spend - until refresh from server
                            long spentAmount = (pendingTransaction.bigIntAmount.longValue() + pendingTransaction.bigIntFee.longValue());

                            if (pendingTransaction.sendingObject.accountObject instanceof Account) {
                                payloadManager.subtractAmountFromAddressBalance(
                                        ((Account) pendingTransaction.sendingObject.accountObject).getXpub(), BigInteger.valueOf(spentAmount));
                            } else {
                                payloadManager.subtractAmountFromAddressBalance(
                                        ((LegacyAddress) pendingTransaction.sendingObject.accountObject).getAddress(), BigInteger.valueOf(spentAmount));
                            }

                            payloadDataManager.syncPayloadWithServer().subscribe(new IgnorableDefaultObserver<>());

                            accountModel.setTransferFundsVisibility(View.GONE);
                            dataListener.setActivityResult(Activity.RESULT_OK);
                        }, throwable -> dataListener.showToast(R.string.send_failed, ToastCustom.TYPE_ERROR)));

    }

    void updateAccountLabel(String newLabel) {
        newLabel = newLabel.trim();

        if (!newLabel.isEmpty()) {
            String finalNewLabel = newLabel;
            String revertLabel;

            if (account != null) {
                revertLabel = account.getLabel();
                account.setLabel(finalNewLabel);
            } else {
                revertLabel = legacyAddress.getLabel();
                legacyAddress.setLabel(finalNewLabel);
            }

            dataListener.showProgressDialog(R.string.please_wait);

            compositeDisposable.add(
                    payloadDataManager.syncPayloadWithServer()
                            .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                            .subscribe(() -> {
                                accountModel.setLabel(finalNewLabel);
                                dataListener.setActivityResult(Activity.RESULT_OK);
                            }, throwable -> revertLabelAndShowError(revertLabel)));
        } else {
            dataListener.showToast(R.string.label_cant_be_empty, ToastCustom.TYPE_ERROR);
        }
    }

    private void revertLabelAndShowError(String revertLabel) {
        // Remote save not successful - revert
        if (account != null) {
            account.setLabel(revertLabel);
        } else {
            legacyAddress.setLabel(revertLabel);
        }
        accountModel.setLabel(revertLabel);
        dataListener.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR);
    }

    @SuppressWarnings("unused")
    public void onClickChangeLabel(View view) {
        dataListener.promptAccountLabel(accountModel.getLabel());
    }

    @SuppressWarnings("unused")
    public void onClickDefault(View view) {
        int revertDefault = payloadManager.getPayload().getHdWallets().get(0).getDefaultAccountIdx();
        payloadManager.getPayload().getHdWallets().get(0).setDefaultAccountIdx(accountIndex);

        dataListener.showProgressDialog(R.string.please_wait);

        compositeDisposable.add(
                payloadDataManager.syncPayloadWithServer()
                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                        .subscribe(() -> {
                            setDefault(isDefault(account));
                            updateSwipeToReceiveAddresses();
                            dataListener.updateAppShortcuts();
                            dataListener.setActivityResult(Activity.RESULT_OK);
                        }, throwable -> revertDefaultAndShowError(revertDefault)));
    }

    private void updateSwipeToReceiveAddresses() {
        // Defer to background thread as deriving addresses is quite processor intensive
        compositeDisposable.add(
                Completable.fromCallable(() -> {
                    swipeToReceiveHelper.updateAndStoreAddresses();
                    return Void.TYPE;
                }).subscribeOn(Schedulers.computation())
                        .subscribe(() -> {
                            // No-op
                        }, Throwable::printStackTrace));
    }

    private void revertDefaultAndShowError(int revertDefault) {
        // Remote save not successful - revert
        payloadManager.getPayload().getHdWallets().get(0).setDefaultAccountIdx(revertDefault);
        dataListener.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR);
    }

    @SuppressWarnings("unused")
    public void onClickScanXpriv(View view) {
        if (payloadManager.getPayload().isDoubleEncryption()) {
            dataListener.promptPrivateKey(String.format(stringUtils.getString(R.string.watch_only_spend_instructionss), legacyAddress.getAddress()));
        } else {
            dataListener.startScanActivity();
        }
    }

    @SuppressWarnings("unused")
    public void onClickShowXpub(View view) {
        if (account != null) {
            dataListener.showXpubSharingWarning();
        } else {
            showAddressDetails();
        }
    }

    @SuppressWarnings("unused")
    public void onClickArchive(View view) {
        String title = stringUtils.getString(R.string.archive);
        String subTitle = stringUtils.getString(R.string.archive_are_you_sure);

        if ((account != null && account.isArchived())
                || (legacyAddress != null && legacyAddress.getTag() == LegacyAddress.ARCHIVED_ADDRESS)) {
            title = stringUtils.getString(R.string.unarchive);
            subTitle = stringUtils.getString(R.string.unarchive_are_you_sure);
        }

        dataListener.promptArchive(title, subTitle);
    }

    private boolean toggleArchived() {
        if (account != null) {
            account.setArchived(!account.isArchived());
            return account.isArchived();
        } else {
            if (legacyAddress.getTag() == LegacyAddress.ARCHIVED_ADDRESS) {
                legacyAddress.setTag(LegacyAddress.NORMAL_ADDRESS);
                return false;
            } else {
                legacyAddress.setTag(LegacyAddress.ARCHIVED_ADDRESS);
                return true;
            }
        }
    }

    @VisibleForTesting
    void importAddressPrivateKey(ECKey key, LegacyAddress address, boolean matchesIntendedAddress) throws Exception {
        setLegacyAddressKey(key, address);

        compositeDisposable.add(
                payloadDataManager.syncPayloadWithServer()
                        .subscribe(() -> {
                            dataListener.setActivityResult(Activity.RESULT_OK);
                            accountModel.setScanPrivateKeyVisibility(View.GONE);
                            accountModel.setArchiveVisibility(View.VISIBLE);

                            if (matchesIntendedAddress) {
                                dataListener.privateKeyImportSuccess();
                            } else {
                                dataListener.privateKeyImportMismatch();
                            }
                        }, throwable -> dataListener.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR)));
    }

    private void setLegacyAddressKey(ECKey key, LegacyAddress address) throws Exception {
        // If double encrypted, save encrypted in payload
        if (!payloadManager.getPayload().isDoubleEncryption()) {
            address.setPrivateKeyFromBytes(key.getPrivKeyBytes());
        } else {
            String encryptedKey = Base58.encode(key.getPrivKeyBytes());
            String encrypted2 = DoubleEncryptionFactory.encrypt(
                    encryptedKey,
                    payloadManager.getPayload().getSharedKey(),
                    secondPassword,
                    payloadManager.getPayload().getOptions().getPbkdf2Iterations());
            address.setPrivateKey(encrypted2);
        }
    }

    void importUnmatchedPrivateKey(ECKey key) throws Exception {
        if (payloadManager.getPayload().getLegacyAddressStringList().contains(key.toAddress(MainNetParams.get()).toString())) {
            // Wallet contains address associated with this private key, find & save it with scanned key
            String foundAddressString = key.toAddress(MainNetParams.get()).toString();
            for (LegacyAddress legacyAddress : payloadManager.getPayload().getLegacyAddressList()) {
                if (legacyAddress.getAddress().equals(foundAddressString)) {
                    importAddressPrivateKey(key, legacyAddress, false);
                }
            }
        } else {
            // Create new address and store
            final LegacyAddress legacyAddress = LegacyAddress.fromECKey(key);

            setLegacyAddressKey(key, legacyAddress);
            remoteSaveUnmatchedPrivateKey(legacyAddress);

            dataListener.privateKeyImportMismatch();
        }
    }

    void showAddressDetails() {

        String heading = null;
        String note = null;
        String copy = null;
        String qrString = null;
        Bitmap bitmap = null;

        if (account != null) {
            heading = stringUtils.getString(R.string.extended_public_key);
            note = stringUtils.getString(R.string.scan_this_code);
            copy = stringUtils.getString(R.string.copy_xpub);
            qrString = account.getXpub();

        } else if (legacyAddress != null) {
            heading = stringUtils.getString(R.string.address);
            note = legacyAddress.getAddress();
            copy = stringUtils.getString(R.string.copy_address);
            qrString = legacyAddress.getAddress();
        }

        int qrCodeDimension = 260;
        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(qrString, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);
        try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            Log.e(TAG, "showAddressDetails: ", e);
        }

        dataListener.showAddressDetails(heading, note, copy, bitmap, qrString);
    }

    void handleIncomingScanIntent(Intent data) {
        String scanData = data.getStringExtra(CaptureActivity.SCAN_RESULT);

        try {
            String format = privateKeyFactory.getFormat(scanData);
            if (format != null) {
                if (!format.equals(PrivateKeyFactory.BIP38)) {
                    importNonBIP38Address(format, scanData);
                } else {
                    dataListener.promptBIP38Password(scanData);
                }
            } else {
                dataListener.showToast(R.string.privkey_error, ToastCustom.TYPE_ERROR);
            }

        } catch (Exception e) {
            dataListener.showToast(R.string.scan_not_recognized, ToastCustom.TYPE_ERROR);
            Log.e(TAG, "handleIncomingScanIntent: ", e);
        }
    }

    public void setSecondPassword(String secondPassword) {
        this.secondPassword = secondPassword;
    }

    void archiveAccount() {
        dataListener.showProgressDialog(R.string.please_wait);

        boolean isArchived = toggleArchived();
        compositeDisposable.add(
                payloadDataManager.syncPayloadWithServer()
                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                        .subscribe(() -> {
                            payloadDataManager.updateBalancesAndTransactions()
                                    .subscribe(new IgnorableDefaultObserver<>());

                            setArchive(isArchived);
                            dataListener.setActivityResult(Activity.RESULT_OK);
                        }, throwable -> dataListener.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR)));
    }

    private void importNonBIP38Address(final String format, final String data) {
        dataListener.showProgressDialog(R.string.please_wait);

        try {
            final ECKey key = privateKeyFactory.getKey(format, data);
            if (key != null && key.hasPrivKey()) {
                final String keyAddress = key.toAddress(MainNetParams.get()).toString();
                if (!legacyAddress.getAddress().equals(keyAddress)) {
                    // Private key does not match this address - warn user but import nevertheless
                    importUnmatchedPrivateKey(key);
                } else {
                    importAddressPrivateKey(key, legacyAddress, true);
                }
            } else {
                dataListener.showToast(R.string.invalid_private_key, ToastCustom.TYPE_ERROR);
            }
        } catch (Exception e) {
            dataListener.showToast(R.string.no_private_key, ToastCustom.TYPE_ERROR);
        }

        dataListener.dismissProgressDialog();
    }

    void importBIP38Address(final String data, final String pw) {
        dataListener.showProgressDialog(R.string.please_wait);

        try {
            BIP38PrivateKey bip38 = new BIP38PrivateKey(MainNetParams.get(), data);
            final ECKey key = bip38.decrypt(pw);

            if (key != null && key.hasPrivKey()) {
                final String keyAddress = key.toAddress(MainNetParams.get()).toString();
                if (!legacyAddress.getAddress().equals(keyAddress)) {
                    // Private key does not match this address - warn user but import nevertheless
                    importUnmatchedPrivateKey(key);
                } else {
                    importAddressPrivateKey(key, legacyAddress, true);
                }

            } else {
                dataListener.showToast(R.string.invalid_private_key, ToastCustom.TYPE_ERROR);
            }
        } catch (Exception e) {
            dataListener.showToast(R.string.bip38_error, ToastCustom.TYPE_ERROR);
            Log.e(TAG, "importBIP38Address: ", e);
        }

        dataListener.dismissProgressDialog();
    }

    private void remoteSaveUnmatchedPrivateKey(final LegacyAddress legacyAddress) {
        Wallet updatedPayload = payloadManager.getPayload();
        List<LegacyAddress> updatedLegacyAddresses = updatedPayload.getLegacyAddressList();
        updatedLegacyAddresses.add(legacyAddress);
        payloadManager.getPayload().getLegacyAddressList().clear();
        payloadManager.getPayload().getLegacyAddressList().addAll(updatedLegacyAddresses);

        compositeDisposable.add(
                payloadDataManager.syncPayloadWithServer()
                        .subscribe(() -> {
                            // Subscribe to new address only if successfully created
                            dataListener.sendBroadcast("address", legacyAddress.getAddress());
                            dataListener.setActivityResult(Activity.RESULT_OK);
                        }, throwable -> dataListener.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR)));
    }

    public PayloadDataManager getPayloadDataManager() {
        return payloadDataManager;
    }
}
