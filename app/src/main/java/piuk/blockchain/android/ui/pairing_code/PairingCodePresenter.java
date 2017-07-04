package piuk.blockchain.android.ui.pairing_code;

import android.graphics.Bitmap;

import javax.inject.Inject;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.StringUtils;

public class PairingCodePresenter extends BasePresenter<PairingCodeView> {

    private static final String WEB_WALLET_URL = "blockchain.info/wallet/login";

    @Inject QrCodeDataManager qrCodeDataManager;
    @Inject StringUtils stringUtils;
    @Inject PayloadDataManager payloadDataManager;
    @Inject AuthDataManager authDataManager;

    public PairingCodePresenter() {
        Injector.getInstance().getDataManagerComponent().inject(this);
    }

    @Override
    public void onViewReady() {
        //no op
    }

    String getFirstStep() {
        return String.format(stringUtils.getString(R.string.pairing_code_instruction_1), WEB_WALLET_URL);
    }

    public void generatePairingQr() {
        getPairingEncryptionPasswordObservable()
                .doOnSubscribe(disposable -> getView().showProgressSpinner())
                .doAfterTerminate(() -> getView().hideProgressSpinner())
                .flatMap(encryptionPassword -> generatePairingCodeObservable(encryptionPassword.string()))
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(
                        bitmap -> getView().onQrLoaded(bitmap),
                        throwable -> getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR));
    }

    private Observable<ResponseBody> getPairingEncryptionPasswordObservable() {

        String guid = payloadDataManager.getWallet().getGuid();
        return authDataManager.getPairingEncryptionPassword(guid);
    }

    private Observable<Bitmap> generatePairingCodeObservable(String encryptionPhrase) {

        String guid = payloadDataManager.getWallet().getGuid();
        String sharedKey = payloadDataManager.getWallet().getSharedKey();
        String password = payloadDataManager.getTempPassword();

        return qrCodeDataManager.generatePairingCode(
                guid,
                password,
                sharedKey,
                encryptionPhrase,
                600);
    }
}
