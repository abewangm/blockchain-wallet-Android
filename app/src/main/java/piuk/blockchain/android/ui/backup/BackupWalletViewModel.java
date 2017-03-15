package piuk.blockchain.android.ui.backup;

import javax.inject.Inject;
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.util.annotations.Thunk;

public class BackupWalletViewModel extends BaseViewModel{

    @Inject
    TransferFundsDataManager transferFundsDataManager;

    @Thunk
    DataListener dataListener;

    public BackupWalletViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    public interface DataListener {
        void showTransferFundsPrompt();
    }
        @Override
    public void onViewReady() {
        //no op
    }

    public void checkTransferableFunds() {
        compositeDisposable.add(
            transferFundsDataManager.getTransferableFundTransactionListForDefaultAccount()
                .subscribe(triple -> {
                    if (!triple.getLeft().isEmpty()) {
                        dataListener.showTransferFundsPrompt();
                    }
                }, Throwable::printStackTrace));
    }
}
