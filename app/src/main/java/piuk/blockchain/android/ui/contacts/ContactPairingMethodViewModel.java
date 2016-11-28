package piuk.blockchain.android.ui.contacts;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.MetaDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;

public class ContactPairingMethodViewModel extends BaseViewModel {

    private DataListener dataListener;
    @Inject AppUtil appUtil;
    @Inject MetaDataManager metaDataManager;

    interface DataListener {

        void onShowToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void finishActivityWithResult(int resultCode);

    }

    ContactPairingMethodViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    void handleScanInput(@NonNull String extra) {
        // TODO: 15/11/2016 Input validation?

        compositeDisposable.add(
                metaDataManager.acceptInvitation(extra)
                        .flatMap(share -> metaDataManager.putTrusted(share.getMdid()))
                        .subscribe(
                                success -> {
                                    dataListener.onShowToast(R.string.remote_save_ok, ToastCustom.TYPE_OK);
                                    dataListener.finishActivityWithResult(Activity.RESULT_OK);
                                }, throwable -> dataListener.onShowToast(R.string.pairing_failed, ToastCustom.TYPE_ERROR)));
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
