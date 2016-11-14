package piuk.blockchain.android.ui.metadata;

import android.support.annotation.StringRes;

import javax.inject.Inject;

import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;

public class ContactPairingMethodViewModel extends BaseViewModel {

    @Inject AppUtil appUtil;
    private DataListener dataListener;

    interface DataListener {

        void onShowToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    }

    ContactPairingMethodViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    void handleScanInput(String extra) {
        // TODO: 14/11/2016 Handle this input
    }

    void onSendLinkClicked() {
        // TODO: 14/11/2016 Generate link and share
    }

    void onNfcClicked() {
        // TODO: 14/11/2016 Generate link and share via NFC
        /**
         * Although to be honest, using a URI for sharing will allow NFC use anyway, but it might
         * be good to make the option explicit?
         */
    }

    boolean isCameraOpen() {
        return appUtil.isCameraOpen();
    }

    @Override
    public void onViewReady() {
        // No-op
    }
}
