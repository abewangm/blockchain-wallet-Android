package piuk.blockchain.android.ui.buy;

import javax.inject.Inject;

import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.util.AppUtil;

/**
 * Created by justin on 4/27/17.
 */

public class BuyViewModel extends BaseViewModel {
    @Inject protected AppUtil appUtil;

    BuyViewModel() {
        Injector.getInstance().getDataManagerComponent().inject(this);
    }

    public Boolean isNewlyCreated() {
        return appUtil.isNewlyCreated();
    }

    @Override
    public void onViewReady() {
    }
}
