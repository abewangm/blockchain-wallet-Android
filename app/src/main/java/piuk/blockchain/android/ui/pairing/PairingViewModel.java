package piuk.blockchain.android.ui.pairing;

import android.support.annotation.StringRes;

import info.blockchain.wallet.payload.PayloadManager;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;

public class PairingViewModel extends BaseViewModel {

    @Inject protected AppUtil appUtil;
    @Inject protected PayloadManager payloadManager;
    @Inject protected PrefsUtil prefsUtil;
    private DataListener dataListener;

    interface DataListener {

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void startPinEntryActivity();

        void showProgressDialog(@StringRes int message);

        void dismissProgressDialog();

    }

    PairingViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    void pairWithQR(String raw) {
        dataListener.showProgressDialog(R.string.please_wait);

        appUtil.clearCredentials();

        compositeDisposable.add(
                handleQrCode(raw)
                        .compose(RxUtil.applySchedulersToObservable())
                        .subscribe((voidType) -> {
                            dataListener.dismissProgressDialog();

                            prefsUtil.setValue(PrefsUtil.KEY_GUID, payloadManager.getPayload().getGuid());
                            prefsUtil.setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
                            dataListener.startPinEntryActivity();

                        }, throwable -> {
                            dataListener.dismissProgressDialog();
                            dataListener.showToast(R.string.pairing_failed, ToastCustom.TYPE_ERROR);
                            appUtil.clearCredentialsAndRestart();
                        }));
    }

    private Observable handleQrCode(String data) {
        return Observable.fromCallable(() -> {
            payloadManager.initializeAndDecryptFromQR(data);
            appUtil.setSharedKey(payloadManager.getPayload().getSharedKey());
            return Void.TYPE;
        });
    }
}
