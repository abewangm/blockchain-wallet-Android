package piuk.blockchain.android.ui.backup;

import android.support.annotation.StringRes;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.PrefsUtil;

public class BackupVerifyViewModel extends BaseViewModel {

    private DataListener dataListener;
    @Inject protected PayloadDataManager payloadDataManager;
    @Inject protected PrefsUtil prefsUtil;

    interface DataListener {

        void showProgressDialog();

        void hideProgressDialog();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void showCompletedFragment();

        void showStartingFragment();

    }

    BackupVerifyViewModel(DataListener dataListener) {
        this.dataListener = dataListener;
        Injector.getInstance().getDataManagerComponent().inject(this);
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    void onVerifyClicked() {
        dataListener.showProgressDialog();
        payloadDataManager.getWallet().getHdWallets().get(0).setMnemonicVerified(true);

        compositeDisposable.add(
                payloadDataManager.syncPayloadWithServer()
                        .doAfterTerminate(() -> dataListener.hideProgressDialog())
                        .subscribe(() -> {
                            prefsUtil.setValue(BackupWalletActivity.BACKUP_DATE_KEY, (int) (System.currentTimeMillis() / 1000));
                            dataListener.showToast(R.string.backup_confirmed, ToastCustom.TYPE_OK);
                            dataListener.showCompletedFragment();
                        }, throwable -> {
                            dataListener.showToast(R.string.api_fail, ToastCustom.TYPE_ERROR);
                            dataListener.showStartingFragment();
                        }));
    }

}
