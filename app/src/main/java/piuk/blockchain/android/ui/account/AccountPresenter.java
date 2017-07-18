package piuk.blockchain.android.ui.account;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.PayloadException;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.PrivateKeyFactory;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.BIP38PrivateKey;

import java.math.BigInteger;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.datamanagers.AccountDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.LabelUtil;
import piuk.blockchain.android.util.PrefsUtil;
import timber.log.Timber;

public class AccountPresenter extends BasePresenter<AccountView> {

    public static final String KEY_WARN_TRANSFER_ALL = "WARN_TRANSFER_ALL";
    private static final String KEY_XPUB = "xpub";
    private static final String KEY_ADDRESS = "address";

    private PayloadDataManager payloadDataManager;
    private AccountDataManager accountDataManager;
    private TransferFundsDataManager fundsDataManager;
    private PrefsUtil prefsUtil;
    private AppUtil appUtil;
    private PrivateKeyFactory privateKeyFactory;
    private EnvironmentSettings environmentSettings;
    @VisibleForTesting String doubleEncryptionPassword;

    @Inject
    AccountPresenter(PayloadDataManager payloadDataManager,
                     AccountDataManager accountDataManager,
                     TransferFundsDataManager fundsDataManager,
                     PrefsUtil prefsUtil,
                     AppUtil appUtil,
                     PrivateKeyFactory privateKeyFactory,
                     EnvironmentSettings environmentSettings) {

        this.payloadDataManager = payloadDataManager;
        this.accountDataManager = accountDataManager;
        this.fundsDataManager = fundsDataManager;
        this.prefsUtil = prefsUtil;
        this.appUtil = appUtil;
        this.privateKeyFactory = privateKeyFactory;
        this.environmentSettings = environmentSettings;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    void setDoubleEncryptionPassword(String secondPassword) {
        doubleEncryptionPassword = secondPassword;
    }

    /**
     * Silently check if there are any spendable legacy funds that need to be sent to default
     * account. Prompt user when done calculating.
     */
    void checkTransferableLegacyFunds(boolean isAutoPopup, boolean showWarningDialog) {
        getCompositeDisposable().add(
                fundsDataManager.getTransferableFundTransactionListForDefaultAccount()
                        .subscribe(triple -> {
                            if (payloadDataManager.getWallet().isUpgraded() && !triple.getLeft().isEmpty()) {
                                getView().onSetTransferLegacyFundsMenuItemVisible(true);

                                if ((prefsUtil.getValue(KEY_WARN_TRANSFER_ALL, true) || !isAutoPopup) && showWarningDialog) {
                                    getView().onShowTransferableLegacyFundsWarning(isAutoPopup);
                                }
                            } else {
                                getView().onSetTransferLegacyFundsMenuItemVisible(false);
                            }
                            getView().dismissProgressDialog();
                        }, throwable -> getView().onSetTransferLegacyFundsMenuItemVisible(false)));
    }

    /**
     * Derive new Account from seed
     *
     * @param accountLabel A label for the account to be created
     */
    void createNewAccount(String accountLabel) {
        if (LabelUtil.isExistingLabel(payloadDataManager, accountLabel)) {
            getView().showToast(R.string.label_name_match, ToastCustom.TYPE_ERROR);
            return;
        }

        getCompositeDisposable().add(
                accountDataManager.createNewAccount(accountLabel, doubleEncryptionPassword)
                        .doOnSubscribe(disposable -> getView().showProgressDialog(R.string.please_wait))
                        .subscribe(account -> {
                            getView().dismissProgressDialog();
                            getView().showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK);
                            Intent intent = new Intent(WebSocketService.ACTION_INTENT);
                            intent.putExtra(KEY_XPUB, account.getXpub());
                            getView().broadcastIntent(intent);
                            getView().onUpdateAccountsList();

                        }, throwable -> {
                            getView().dismissProgressDialog();
                            if (throwable instanceof DecryptionException) {
                                getView().showToast(R.string.double_encryption_password_error, ToastCustom.TYPE_ERROR);
                            } else if (throwable instanceof PayloadException) {
                                getView().showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR);
                            } else {
                                getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                            }
                        }));
    }

    /**
     * Sync {@link LegacyAddress} with server after either creating a new address or updating the
     * address in some way, for instance updating its name.
     *
     * @param address The {@link LegacyAddress} to be sync'd with the server
     */
    void updateLegacyAddress(LegacyAddress address) {
        getView().showProgressDialog(R.string.saving_address);
        getCompositeDisposable().add(
                accountDataManager.updateLegacyAddress(address)
                        .subscribe(() -> {
                            getView().dismissProgressDialog();
                            getView().showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK);
                            Intent intent = new Intent(WebSocketService.ACTION_INTENT);
                            intent.putExtra(KEY_ADDRESS, address.getAddress());
                            getView().broadcastIntent(intent);
                            getView().onUpdateAccountsList();
                        }, throwable -> {
                            getView().dismissProgressDialog();
                            getView().showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR);
                        }));
    }

    /**
     * Checks status of camera and updates UI appropriately
     */
    void onScanButtonClicked() {
        if (!appUtil.isCameraOpen()) {
            getView().startScanForResult();
        } else {
            getView().showToast(R.string.camera_unavailable, ToastCustom.TYPE_ERROR);
        }
    }

    /**
     * Imports BIP38 address and prompts user to rename address if successful
     *
     * @param data     The address to be imported
     * @param password The BIP38 encryption passphrase
     */
    void importBip38Address(String data, String password) {
        getView().showProgressDialog(R.string.please_wait);
        try {
            BIP38PrivateKey bip38 = BIP38PrivateKey.fromBase58(environmentSettings.getNetworkParameters(), data);
            ECKey key = bip38.decrypt(password);
            handlePrivateKey(key, doubleEncryptionPassword);
        } catch (Exception e) {
            Timber.e("importBip38Address: ", e);
            getView().showToast(R.string.bip38_error, ToastCustom.TYPE_ERROR);
        } finally {
            getView().dismissProgressDialog();
        }
    }

    /**
     * Handles result of address scanning operation appropriately for each possible type of address
     *
     * @param data The address to be imported
     */
    void onAddressScanned(String data) {
        try {
            String format = privateKeyFactory.getFormat(data);
            if (format != null) {
                // Private key scanned
                if (!format.equals(PrivateKeyFactory.BIP38)) {
                    importNonBip38Address(format, data, doubleEncryptionPassword);
                } else {
                    getView().showBip38PasswordDialog(data);
                }
            } else {
                // Watch-only address scanned
                importWatchOnlyAddress(data);
            }
        } catch (Exception e) {
            Timber.e(e);
            getView().showToast(R.string.privkey_error, ToastCustom.TYPE_ERROR);
        }
    }

    /**
     * Create {@link LegacyAddress} from correctly formatted address string, show rename dialog
     * after finishing
     *
     * @param address The address to be saved
     */
    void confirmImportWatchOnly(String address) {
        LegacyAddress legacyAddress = new LegacyAddress();
        legacyAddress.setAddress(address);
        legacyAddress.setCreatedDeviceName("android");
        legacyAddress.setCreatedTime(System.currentTimeMillis());
        legacyAddress.setCreatedDeviceVersion(BuildConfig.VERSION_NAME);

        getCompositeDisposable().add(
                accountDataManager.addLegacyAddress(legacyAddress)
                        .subscribe(
                                () -> getView().showRenameImportedAddressDialog(legacyAddress),
                                throwable -> getView().showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR)));
    }

    ///////////////////////////////////////////////////////////////////////////
    // HELPER METHODS
    ///////////////////////////////////////////////////////////////////////////

    List<Account> getAccounts() {
        return payloadDataManager.getAccounts();
    }

    List<LegacyAddress> getLegacyAddressList() {
        return payloadDataManager.getLegacyAddresses();
    }

    int getDefaultAccountIndex() {
        return payloadDataManager.getDefaultAccountIndex();
    }

    String getXpubFromIndex(int index) {
        return payloadDataManager.getXpubFromIndex(index);
    }

    /* This will also return the final balance from an XPub */
    BigInteger getBalanceFromAddress(String address) {
        return payloadDataManager.getAddressBalance(address);
    }

    private void importWatchOnlyAddress(String address) {
        address = correctAddressFormatting(address);

        if (!FormatsUtil.isValidBitcoinAddress(address)) {
            getView().showToast(R.string.invalid_bitcoin_address, ToastCustom.TYPE_ERROR);
        } else if (payloadDataManager.getWallet().getLegacyAddressStringList().contains(address)) {
            getView().showToast(R.string.address_already_in_wallet, ToastCustom.TYPE_ERROR);
        } else {
            // Do some things
            getView().showWatchOnlyWarningDialog(address);
        }
    }

    private String correctAddressFormatting(String address) {
        // Check for poorly formed BIP21 URIs
        if (address.startsWith("bitcoin://") && address.length() > 10) {
            address = "bitcoin:" + address.substring(10);
        }

        if (FormatsUtil.isBitcoinUri(address)) {
            address = FormatsUtil.getBitcoinAddress(address);
        }

        return address;
    }

    private void importNonBip38Address(String format, String data, @Nullable String secondPassword) {
        getView().showProgressDialog(R.string.please_wait);

        getCompositeDisposable().add(
                accountDataManager.getKeyFromImportedData(format, data)
                        .subscribe(key -> {
                            handlePrivateKey(key, secondPassword);
                            getView().dismissProgressDialog();
                        }, throwable -> {
                            getView().showToast(R.string.no_private_key, ToastCustom.TYPE_ERROR);
                            getView().dismissProgressDialog();
                        }));
    }

    @VisibleForTesting
    void handlePrivateKey(ECKey key, @Nullable String secondPassword) {
        if (key != null && key.hasPrivKey()) {
            // A private key to an existing address has been scanned
            getCompositeDisposable().add(
                    accountDataManager.setKeyForLegacyAddress(key, secondPassword)
                            .subscribe(legacyAddress -> {
                                getView().showToast(R.string.private_key_successfully_imported, ToastCustom.TYPE_OK);
                                getView().onUpdateAccountsList();
                                getView().showRenameImportedAddressDialog(legacyAddress);
                            }, throwable -> getView().showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR)));
        } else {
            getView().showToast(R.string.no_private_key, ToastCustom.TYPE_ERROR);
        }
    }

}
