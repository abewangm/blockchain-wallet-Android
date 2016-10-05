package piuk.blockchain.android.ui.account;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.util.Pair;

import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.PayloadException;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.PrivateKeyFactory;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.params.MainNetParams;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.AccountDataManager;
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.send.PendingTransaction;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.DialogButtonCallback;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.annotations.Thunk;
import rx.exceptions.Exceptions;

public class AccountViewModel extends BaseViewModel {

    public static final String KEY_WARN_TRANSFER_ALL = "WARN_TRANSFER_ALL";
    public static final String KEY_XPUB = "xpub";
    private static final String KEY_ADDRESS = "address";

    @Thunk DataListener dataListener;
    @Inject PayloadManager payloadManager;
    @Inject AccountDataManager accountDataManager;
    @Inject TransferFundsDataManager fundsDataManager;
    @Inject PrefsUtil prefsUtil;
    @Inject AppUtil appUtil;

    public AccountViewModel(DataListener dataListener) {
        Injector.getInstance().getAppComponent().inject(this);
        this.dataListener = dataListener;
    }

    public interface DataListener {
        void onShowTransferableLegacyFundsWarning(boolean isAutoPopup);

        void onSetTransferLegacyFundsMenuItemVisible(boolean visible);

        void showProgressDialog(@StringRes int message);

        void dismissProgressDialog();

        void onUpdateAccountsList();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void broadcastIntent(Intent intent);

        void showWatchOnlyWarning(DialogButtonCallback dialogButtonCallback);

        void showRenameImportedAddressDialog(LegacyAddress address);

        void startScanForResult();
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    @Override
    public void destroy() {
        super.destroy();
        dataListener = null;
    }

    /**
     * Silently check if there are any spendable legacy funds that need to be sent to default
     * account. Prompt user when done calculating.
     */
    void checkTransferableLegacyFunds(boolean isAutoPopup) {
        mCompositeSubscription.add(
                fundsDataManager.getTransferableFundTransactionListForDefaultAccount()
                        .subscribe(map -> {
                            Map.Entry<List<PendingTransaction>, Pair<Long, Long>> entry = map.entrySet().iterator().next();
                            if (payloadManager.getPayload().isUpgraded() && !entry.getKey().isEmpty()) {
                                dataListener.onSetTransferLegacyFundsMenuItemVisible(true);

                                if (prefsUtil.getValue(KEY_WARN_TRANSFER_ALL, true) || !isAutoPopup) {
                                    dataListener.onShowTransferableLegacyFundsWarning(isAutoPopup);
                                }
                            } else {
                                dataListener.onSetTransferLegacyFundsMenuItemVisible(false);
                            }
                            dataListener.dismissProgressDialog();
                        }, throwable -> {
                            dataListener.onSetTransferLegacyFundsMenuItemVisible(false);
                        }));
    }

    void createNewAccount(String accountLabel, @Nullable CharSequenceX secondPassword) {
        dataListener.showProgressDialog(R.string.please_wait);
        mCompositeSubscription.add(
                accountDataManager.createNewAccount(accountLabel, secondPassword)
                        .subscribe(account -> {
                            dataListener.dismissProgressDialog();
                            dataListener.showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK);
                            Intent intent = new Intent(WebSocketService.ACTION_INTENT);
                            intent.putExtra(KEY_XPUB, account.getXpub());
                            dataListener.broadcastIntent(intent);

                        }, throwable -> {
                            dataListener.dismissProgressDialog();
                            if (throwable instanceof DecryptionException) {
                                dataListener.showToast(R.string.double_encryption_password_error, ToastCustom.TYPE_ERROR);
                            } else if (throwable instanceof PayloadException) {
                                dataListener.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR);
                            } else {
                                dataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                            }
                        }));
    }

    void updateLegacyAddress(LegacyAddress address) {
        dataListener.showProgressDialog(R.string.saving_address);
        mCompositeSubscription.add(
                accountDataManager.updateLegacyAddress(address)
                        .subscribe(success -> {
                            if (success) {
                                dataListener.dismissProgressDialog();
                                dataListener.showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK);
                                Intent intent = new Intent(WebSocketService.ACTION_INTENT);
                                intent.putExtra(KEY_ADDRESS, address.getAddress());
                                dataListener.broadcastIntent(intent);
                                dataListener.onUpdateAccountsList();
                            } else {
                                throw Exceptions.propagate(new Throwable("Result was false"));
                            }
                        }, throwable -> {
                            dataListener.dismissProgressDialog();
                            dataListener.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR);
                        }));
    }

    void importWatchOnlyAddress(String address) {
        // Check for poorly formed BIP21 URIs
        if (address.startsWith("bitcoin://") && address.length() > 10) {
            address = "bitcoin:" + address.substring(10);
        }

        if (FormatsUtil.getInstance().isBitcoinUri(address)) {
            address = FormatsUtil.getInstance().getBitcoinAddress(address);
        }

        if (!FormatsUtil.getInstance().isValidBitcoinAddress(address)) {
            dataListener.showToast(R.string.invalid_bitcoin_address, ToastCustom.TYPE_ERROR);
        } else if (payloadManager.getPayload().getLegacyAddressStrings().contains(address)) {
            dataListener.showToast(R.string.address_already_in_wallet, ToastCustom.TYPE_ERROR);
        } else {
            // Do some things
            String finalAddress = address;
            dataListener.showWatchOnlyWarning(new DialogButtonCallback() {
                @Override
                public void onPositiveClicked() {
                    LegacyAddress legacyAddress = new LegacyAddress();
                    legacyAddress.setAddress(finalAddress);
                    legacyAddress.setCreatedDeviceName("android");
                    legacyAddress.setCreated(System.currentTimeMillis());
                    legacyAddress.setCreatedDeviceVersion(BuildConfig.VERSION_NAME);
                    legacyAddress.setWatchOnly(true);
                    dataListener.showRenameImportedAddressDialog(legacyAddress);
                }

                @Override
                public void onNegativeClicked() {
                    // No-op
                }
            });
        }
    }

    void onScanButtonClicked() {
        if (!appUtil.isCameraOpen()) {
            dataListener.startScanForResult();
        } else {
            dataListener.showToast(R.string.camera_unavailable, ToastCustom.TYPE_ERROR);
        }
    }

    void importNonBip38Address(String format, String data, @Nullable CharSequenceX secondPassword) {
        dataListener.showProgressDialog(R.string.please_wait);
        try {
            ECKey key = PrivateKeyFactory.getInstance().getKey(format, data);

            handlePrivateKey(secondPassword, key);
        } catch (Exception e) {
            dataListener.showToast(R.string.no_private_key, ToastCustom.TYPE_ERROR);
        } finally {
            dataListener.dismissProgressDialog();
        }
    }

    void importBip38Address(String data, String password, @Nullable CharSequenceX secondPassword) {
        dataListener.showProgressDialog(R.string.please_wait);
        try {
            BIP38PrivateKey bip38 = new BIP38PrivateKey(MainNetParams.get(), data);
            ECKey key = bip38.decrypt(password);

            handlePrivateKey(secondPassword, key);
        } catch (Exception e) {
            dataListener.showToast(R.string.bip38_error, ToastCustom.TYPE_ERROR);
        } finally {
            dataListener.dismissProgressDialog();
        }
    }

    private void handlePrivateKey(@Nullable CharSequenceX secondPassword, ECKey key) {
        if (key != null && key.hasPrivKey()
                && payloadManager.getPayload().getLegacyAddressStrings().contains(key.toAddress(MainNetParams.get()).toString())) {

            // A private key to an existing address has been scanned
            setPrivateECKey(key, secondPassword);

        } else if (key != null && key.hasPrivKey()
                && !payloadManager.getPayload().getLegacyAddressStrings().contains(key.toAddress(MainNetParams.get()).toString())) {
            LegacyAddress legacyAddress =
                    new LegacyAddress(
                            null,
                            System.currentTimeMillis() / 1000L,
                            key.toAddress(MainNetParams.get()).toString(),
                            "",
                            0L,
                            "android",
                            BuildConfig.VERSION_NAME);

            accountDataManager.setKeyForLegacyAddress(legacyAddress, key, secondPassword);
            dataListener.showRenameImportedAddressDialog(legacyAddress);

        } else {
            dataListener.showToast(R.string.no_private_key, ToastCustom.TYPE_ERROR);
        }
    }

    private void setPrivateECKey(ECKey key, @Nullable CharSequenceX secondPassword) {
        mCompositeSubscription.add(
                accountDataManager.setPrivateKey(key, secondPassword)
                        .subscribe(success -> {
                            if (success) {
                                dataListener.showToast(R.string.private_key_successfully_imported, ToastCustom.TYPE_OK);
                                dataListener.onUpdateAccountsList();
                            } else {
                                throw Exceptions.propagate(new Throwable("Save unsuccessful"));
                            }
                        }, throwable -> {
                            dataListener.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR);
                        }));
    }
}
