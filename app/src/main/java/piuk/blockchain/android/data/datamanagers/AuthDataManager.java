package piuk.blockchain.android.data.datamanagers;

import android.support.annotation.VisibleForTesting;

import info.blockchain.wallet.payload.data.Wallet;

import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import okhttp3.ResponseBody;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.WalletService;
import piuk.blockchain.android.ui.transactions.PayloadDataManager;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;
import piuk.blockchain.android.util.annotations.Thunk;
import retrofit2.Response;

@SuppressWarnings("WeakerAccess")
public class AuthDataManager {

    @VisibleForTesting static final String AUTHORIZATION_REQUIRED = "Authorization Required";

    private WalletService walletService;
    private AppUtil appUtil;
    private AccessState accessState;
    private StringUtils stringUtils;
    private PayloadDataManager payloadDataManager;
    @Thunk PrefsUtil prefsUtil;
    @VisibleForTesting protected int timer;

    public AuthDataManager(PayloadDataManager payloadDataManager,
                           PrefsUtil prefsUtil,
                           WalletService walletService,
                           AppUtil appUtil,
                           AccessState accessState,
                           StringUtils stringUtils) {

        this.payloadDataManager = payloadDataManager;
        this.prefsUtil = prefsUtil;
        this.walletService = walletService;
        this.appUtil = appUtil;
        this.accessState = accessState;
        this.stringUtils = stringUtils;
    }

    public Observable<Response<ResponseBody>> getEncryptedPayload(String guid, String sessionId) {
        return walletService.getEncryptedPayload(guid, sessionId)
                .compose(RxUtil.applySchedulersToObservable());
    }

    public Observable<String> getSessionId(String guid) {
        return walletService.getSessionId(guid)
                .compose(RxUtil.applySchedulersToObservable());
    }

    public Completable updatePayload(String sharedKey, String guid, String password) {
        return payloadDataManager.initializeAndDecrypt(sharedKey, guid, password)
                .compose(RxUtil.applySchedulersToCompletable());
    }

    public Observable<String> validatePin(String pin) {
        return accessState.validatePin(pin);
    }

    public Observable<Boolean> createPin(String password, String pin) {
        return accessState.createPin(password, pin)
                .compose(RxUtil.applySchedulersToObservable());
    }

    public Observable<Wallet> createHdWallet(String password, String walletName, String email) {
        return payloadDataManager.createHdWallet(walletName, email, password)
                .compose(RxUtil.applySchedulersToObservable())
                .doOnNext(payload -> {
                    // Successfully created and saved
                    appUtil.setNewlyCreated(true);
                    prefsUtil.setValue(PrefsUtil.KEY_GUID, payload.getGuid());
                    appUtil.setSharedKey(payload.getSharedKey());
                });
    }

    public Observable<Wallet> restoreHdWallet(String email, String password, String mnemonic) {
        return payloadDataManager.restoreHdWallet(
                mnemonic,
                stringUtils.getString(R.string.default_wallet_name),
                email,
                password)
                .doOnNext(payload -> {
                    appUtil.setNewlyCreated(true);
                    prefsUtil.setValue(PrefsUtil.KEY_GUID, payload.getGuid());
                    appUtil.setSharedKey(payload.getSharedKey());
                })
                .compose(RxUtil.applySchedulersToObservable());
    }

    public Observable<String> startPollingAuthStatus(String guid, String sessionId) {
        // Emit tick every 2 seconds
        return Observable.interval(2, TimeUnit.SECONDS)
                // For each emission from the timer, try to get the payload
                .map(tick -> getEncryptedPayload(guid, sessionId).blockingFirst())
                // If auth not required, emit payload
                .filter(s -> s.errorBody() == null || !s.errorBody().string().contains(AUTHORIZATION_REQUIRED))
                // Return message in response
                .map(responseBodyResponse -> responseBodyResponse.body().string())
                // If error called, emit Auth Required
                .onErrorReturn(throwable -> AUTHORIZATION_REQUIRED)
                // Only emit the first object
                .firstElement()
                // As Observable rather than Maybe
                .toObservable()
                // Apply correct threading
                .compose(RxUtil.applySchedulersToObservable());
    }

    public Observable<Integer> createCheckEmailTimer() {
        timer = 2 * 60;

        return Observable.interval(0, 1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(aLong -> timer--)
                .takeUntil(aLong -> timer < 0);
    }

    public Completable initializeFromPayload(String payload, String password) {
        return payloadDataManager.initializeFromPayload(payload, password)
                .doOnComplete(() -> {
                    prefsUtil.setValue(PrefsUtil.KEY_GUID, payloadDataManager.getWallet().getGuid());
                    appUtil.setSharedKey(payloadDataManager.getWallet().getSharedKey());
                    prefsUtil.setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
                }).compose(RxUtil.applySchedulersToCompletable());
    }

}
