package piuk.blockchain.android.ui.backup;

import android.os.Bundle;
import android.support.annotation.StringRes;

import java.util.List;

import javax.inject.Inject;

import kotlin.Pair;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.BackupWalletUtil;
import piuk.blockchain.android.util.PrefsUtil;

import static piuk.blockchain.android.ui.backup.BackupWalletWordListFragment.ARGUMENT_SECOND_PASSWORD;

@SuppressWarnings("WeakerAccess")
public class BackupVerifyViewModel extends BaseViewModel {

    private DataListener dataListener;
    @Inject PayloadDataManager payloadDataManager;
    @Inject PrefsUtil prefsUtil;
    @Inject BackupWalletUtil backupWalletUtil;

    interface DataListener {

        Bundle getPageBundle();

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
        payloadDataManager.getWallet().getHdWallets().get(0).setMnemonicVerified(true);

        compositeDisposable.add(
                payloadDataManager.syncPayloadWithServer()
                        .doOnSubscribe(ignored -> dataListener.showProgressDialog())
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

    List<Pair<Integer, String>> getBackupConfirmSequence() {
        Bundle bundle = dataListener.getPageBundle();
        String secondPassword = null;
        if (bundle != null) {
            secondPassword = bundle.getString(ARGUMENT_SECOND_PASSWORD);
        }

        return backupWalletUtil.getConfirmSequence(secondPassword);
    }

}
