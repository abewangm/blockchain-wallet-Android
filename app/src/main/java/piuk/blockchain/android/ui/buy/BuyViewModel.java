package piuk.blockchain.android.ui.buy;

import info.blockchain.wallet.metadata.Metadata;

import javax.inject.Inject;

import piuk.blockchain.android.data.datamanagers.BuyDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.util.AppUtil;

/**
 * Created by justin on 4/27/17.
 */

public class BuyViewModel extends BaseViewModel {
    private DataListener dataListener;

    @Inject protected AppUtil appUtil;
    @Inject protected BuyDataManager buyDataManager;

    public interface DataListener {
        void setExchangeData(Metadata metadata);
    }

    BuyViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    public Boolean isNewlyCreated() {
        return appUtil.isNewlyCreated();
    }

    public void reloadExchangeDate() {
        buyDataManager.reloadExchangeData();
    }

    @Override
    public void onViewReady() {
        compositeDisposable.add(
                buyDataManager
                        .getExchangeData()
                        .subscribe(metadata -> {
                            dataListener.setExchangeData(metadata);
                        })
        );
    }
}
