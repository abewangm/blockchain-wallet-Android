package piuk.blockchain.android.ui.send;

import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BasePresenter;

public class ConfirmPaymentPresenter extends BasePresenter<ConfirmPaymentView> {

    ConfirmPaymentPresenter() {
        Injector.getInstance().getDataManagerComponent().inject(this);
    }

    @Override
    public void onViewReady() {
        // TODO: 04/05/2017
    }

}
