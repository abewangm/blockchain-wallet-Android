package piuk.blockchain.android.ui.backup;

import javax.inject.Inject;

import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.util.annotations.Thunk;

@SuppressWarnings("WeakerAccess")
public class BackupWalletViewModel extends BaseViewModel {

    @Inject TransferFundsDataManager transferFundsDataManager;
    @Thunk DataListener dataListener;

    public interface DataListener {

        void showTransferFundsPrompt();

    }

    BackupWalletViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    void checkTransferableFunds() {
        compositeDisposable.add(
                transferFundsDataManager.getTransferableFundTransactionListForDefaultAccount()
                        .subscribe(triple -> {
                            if (!triple.getLeft().isEmpty()) {
                                dataListener.showTransferFundsPrompt();
                            }
                        }, Throwable::printStackTrace));
    }
}
