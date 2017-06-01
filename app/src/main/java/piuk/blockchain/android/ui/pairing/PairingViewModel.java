package piuk.blockchain.android.ui.pairing;

import android.support.annotation.StringRes;

import javax.inject.Inject;
import javax.net.ssl.SSLPeerUnverifiedException;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;

public class PairingViewModel extends BaseViewModel {

    @Inject protected AppUtil appUtil;
    @Inject protected PayloadDataManager payloadDataManager;
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
        appUtil.clearCredentials();

        compositeDisposable.add(
                payloadDataManager.handleQrCode(raw)
                        .doOnSubscribe(disposable -> dataListener.showProgressDialog(R.string.please_wait))
                        .doOnComplete(() -> appUtil.setSharedKey(payloadDataManager.getWallet().getSharedKey()))
                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                        .subscribe(() -> {
                            prefsUtil.setValue(PrefsUtil.KEY_GUID, payloadDataManager.getWallet().getGuid());
                            prefsUtil.setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
                            prefsUtil.setValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, true);
                            dataListener.startPinEntryActivity();
                        }, throwable -> {
                            if (throwable instanceof SSLPeerUnverifiedException) {
                                // BaseActivity handles message
                                appUtil.clearCredentials();
                            } else {
                                dataListener.showToast(R.string.pairing_failed, ToastCustom.TYPE_ERROR);
                                appUtil.clearCredentialsAndRestart();
                            }
                        }));
    }

}
