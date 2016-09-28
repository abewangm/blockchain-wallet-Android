package piuk.blockchain.android.ui.account;

import android.support.v4.util.Pair;

import info.blockchain.wallet.payload.PayloadManager;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.send.PendingTransaction;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.annotations.Thunk;

@SuppressWarnings("WeakerAccess")
public class AccountViewModel extends BaseViewModel {

    public static final String KEY_WARN_TRANSFER_ALL = "WARN_TRANSFER_ALL";

    @Thunk DataListener dataListener;
    @Inject PayloadManager payloadManager;
    @Inject TransferFundsDataManager mFundsDataManager;
    @Inject PrefsUtil prefsUtil;

    public AccountViewModel(DataListener dataListener) {
        Injector.getInstance().getAppComponent().inject(this);
        this.dataListener = dataListener;
    }

    public interface DataListener {
        void onShowTransferableLegacyFundsWarning(boolean isAutoPopup);

        void onSetTransferLegacyFundsMenuItemVisible(boolean visible);

        void onShowProgressDialog(String title, String message);

        void onDismissProgressDialog();

        void onUpdateAccountsList();
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
                mFundsDataManager.getTransferableFundTransactionListForDefaultAccount()
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
                            dataListener.onDismissProgressDialog();
                        }, throwable -> {
                            dataListener.onSetTransferLegacyFundsMenuItemVisible(false);
                        }));
    }
}
