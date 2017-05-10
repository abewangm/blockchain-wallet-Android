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

public class BuyViewModel extends BaseViewModel {
    private static final String TAG = BuyViewModel.class.getSimpleName();
    private DataListener dataListener;

    @Inject protected AppUtil appUtil;
    @Inject protected BuyDataManager buyDataManager;

    public interface DataListener {
        void setWebViewLoginDetails(WebViewLoginDetails webViewLoginDetails);
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
                        .getWebViewLoginDetails()
                        .subscribe(webViewLoginDetails -> {
                            dataListener.setWebViewLoginDetails(webViewLoginDetails);
                        }, Throwable::printStackTrace)
        );
    }
}
