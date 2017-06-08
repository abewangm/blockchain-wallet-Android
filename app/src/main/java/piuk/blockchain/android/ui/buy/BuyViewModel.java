package piuk.blockchain.android.ui.buy;

import javax.inject.Inject;

import piuk.blockchain.android.data.datamanagers.BuyDataManager;
import piuk.blockchain.android.data.exchange.WebViewLoginDetails;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.util.AppUtil;

/**
 * Created by justin on 4/27/17.
 */

@SuppressWarnings("WeakerAccess")
public class BuyViewModel extends BaseViewModel {

    private DataListener dataListener;

    @Inject AppUtil appUtil;
    @Inject BuyDataManager buyDataManager;

    public interface DataListener {

        void setWebViewLoginDetails(WebViewLoginDetails webViewLoginDetails);

    }

    BuyViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        compositeDisposable.add(
                buyDataManager
                        .getWebViewLoginDetails()
                        .subscribe(webViewLoginDetails -> dataListener.setWebViewLoginDetails(webViewLoginDetails),
                                Throwable::printStackTrace));
    }

    Boolean isNewlyCreated() {
        return appUtil.isNewlyCreated();
    }

    void reloadExchangeDate() {
        buyDataManager.reloadExchangeData();
    }

}
