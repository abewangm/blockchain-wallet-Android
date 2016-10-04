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

import org.bitcoinj.core.ECKey;

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
import piuk.blockchain.android.util.DialogButtonCallback;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.annotations.Thunk;
import rx.exceptions.Exceptions;

@SuppressWarnings("WeakerAccess")
public class AccountViewModel extends BaseViewModel {

    public static final String KEY_WARN_TRANSFER_ALL = "WARN_TRANSFER_ALL";
    public static final String KEY_XPUB = "xpub";
    private static final String KEY_ADDRESS = "address";

    @Thunk DataListener dataListener;
    @Inject PayloadManager payloadManager;
    @Inject AccountDataManager accountDataManager;
    @Inject TransferFundsDataManager fundsDataManager;
    @Inject PrefsUtil prefsUtil;

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
    public void checkTransferableLegacyFunds(boolean isAutoPopup) {
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

    public void createNewAccount(String accountLabel, @Nullable CharSequenceX secondPassword) {
        dataListener.showProgressDialog(R.string.please_wait);
        mCompositeSubscription.add(
                accountDataManager.createNewAccount(accountLabel, secondPassword)
                        .doOnTerminate(() -> dataListener.dismissProgressDialog())
                        .subscribe(account -> {
                            dataListener.showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK);
                            Intent intent = new Intent(WebSocketService.ACTION_INTENT);
                            intent.putExtra(KEY_XPUB, account.getXpub());
                            dataListener.broadcastIntent(intent);

                        }, throwable -> {
                            if (throwable instanceof DecryptionException) {
                                dataListener.showToast(R.string.double_encryption_password_error, ToastCustom.TYPE_ERROR);
                            } else if (throwable instanceof PayloadException) {
                                dataListener.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR);
                            } else {
                                dataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                            }
                        }));
    }

    public void updateLegacyAddress(LegacyAddress address) {
        dataListener.showProgressDialog(R.string.saving_address);
        mCompositeSubscription.add(
                accountDataManager.updateLegacyAddress(address)
                        .doOnTerminate(() -> dataListener.dismissProgressDialog())
                        .subscribe(success -> {
                            if (success) {
                                dataListener.showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK);
                                Intent intent = new Intent(WebSocketService.ACTION_INTENT);
                                intent.putExtra(KEY_ADDRESS, address.getAddress());
                                dataListener.broadcastIntent(intent);
                                dataListener.onUpdateAccountsList();
                            } else {
                                throw Exceptions.propagate(new Throwable("Result was false"));
                            }
                        }, throwable -> {
                            dataListener.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR);
                        }));
    }

    public void setPrivateECKey(ECKey key, @Nullable CharSequenceX secondPassword) {
        dataListener.showProgressDialog(R.string.please_wait);
        mCompositeSubscription.add(
                accountDataManager.setPrivateKey(key, secondPassword)
                        .doOnTerminate(() -> dataListener.dismissProgressDialog())
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

    public void importWatchOnlyAddress(String address) {
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
}
