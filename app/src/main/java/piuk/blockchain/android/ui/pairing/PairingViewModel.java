package piuk.blockchain.android.ui.pairing;

import android.support.annotation.StringRes;

import info.blockchain.wallet.payload.PayloadManager;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.exceptions.Exceptions;
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

        // TODO: 21/02/2017 QR classes removed from jar. makes sense to do in android?
//        Pairing pairing = new Pairing();
//        WalletPayload access = new WalletPayload();
//
//        compositeDisposable.add(
//                handleQrCode(raw, pairing, access)
//                        .compose(RxUtil.applySchedulersToObservable())
//                        .subscribe(pairingQRComponents -> {
//                            dataListener.dismissProgressDialog();
//                            if (pairingQRComponents.guid != null) {
//                                prefsUtil.setValue(PrefsUtil.KEY_GUID, pairingQRComponents.guid);
//                                prefsUtil.setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
//                                dataListener.startPinEntryActivity();
//                            } else {
//                                throw Exceptions.propagate(new Throwable("GUID was null"));
//                            }
//                        }, throwable -> {
//                            dataListener.dismissProgressDialog();
//                            dataListener.showToast(R.string.pairing_failed, ToastCustom.TYPE_ERROR);
//                            appUtil.clearCredentialsAndRestart();
//                        }));
    }

    // TODO: 22/02/2017  
//    private Observable<PairingQRComponents> handleQrCode(String data, Pairing pairing, WalletPayload access) {
//        return Observable.fromCallable(() -> {
//            PairingQRComponents qrComponents = pairing.getQRComponentsFromRawString(data);
//            String encryptionPassword = access.getPairingEncryptionPassword(qrComponents.guid);
//            String[] sharedKeyAndPassword = pairing.getSharedKeyAndPassword(qrComponents.encryptedPairingCode, encryptionPassword);
//
//            CharSequenceX password = new CharSequenceX(new String(Hex.decode(sharedKeyAndPassword[1]), "UTF-8"));
//
//            payloadManager.setTempPassword(password);
//            appUtil.setSharedKey(sharedKeyAndPassword[0]);
//
//            return qrComponents;
//        });
//    }
}
