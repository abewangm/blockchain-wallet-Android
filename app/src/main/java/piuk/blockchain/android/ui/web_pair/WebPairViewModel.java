package piuk.blockchain.android.ui.web_pair;

import android.graphics.Bitmap;
import android.support.annotation.StringRes;

import javax.inject.Inject;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.StringUtils;

public class WebPairViewModel extends BaseViewModel {

    private final String TAG = getClass().getName();
    private final String webWalletUrl = "blockchain.info/wallet/login";

    @Inject QrCodeDataManager qrCodeDataManager;
    @Inject StringUtils stringUtils;
    @Inject PayloadDataManager payloadDataManager;
    @Inject AuthDataManager authDataManager;
    
    private DataListener dataListener;

    public interface DataListener {

        void onQrLoaded(Bitmap bitmap);

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void showProgressSpinner();
    }

    public WebPairViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        //no op
    }

    public String getFirstStep() {
        return String.format(stringUtils.getString(R.string.web_pair_instruction_1), webWalletUrl);
    }

    public void generatePairingQr() {

        compositeDisposable.add(
                getPairingEncryptionPasswordObservable()
                        .doOnSubscribe(disposable -> dataListener.showProgressSpinner())
                        .flatMap(encryptionPassword -> generatePairingCodeObservable(encryptionPassword.string()))
                        .subscribe(bitmap -> dataListener.onQrLoaded(bitmap),
                                throwable -> dataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));
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
                180);
    }
}
