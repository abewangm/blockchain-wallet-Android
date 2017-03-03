package piuk.blockchain.android.data.datamanagers;

import android.support.annotation.VisibleForTesting;
import android.util.Log;

import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.exceptions.PayloadException;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Wallet;

import org.apache.commons.lang3.NotImplementedException;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.WalletPayloadService;
import piuk.blockchain.android.util.AESUtilWrapper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;
import piuk.blockchain.android.util.annotations.Thunk;

@SuppressWarnings("WeakerAccess")
public class AuthDataManager {

    private WalletPayloadService walletPayloadService;
    private PayloadManager payloadManager;
    private AppUtil appUtil;
    private AESUtilWrapper aesUtilWrapper;
    private AccessState accessState;
    private StringUtils stringUtils;
    @Thunk PrefsUtil prefsUtil;
    @VisibleForTesting protected int timer;

    public AuthDataManager(PayloadManager payloadManager,
                           PrefsUtil prefsUtil,
                           WalletPayloadService walletPayloadService,
                           AppUtil appUtil,
                           AESUtilWrapper aesUtilWrapper,
                           AccessState accessState,
                           StringUtils stringUtils) {

        this.payloadManager = payloadManager;
        this.prefsUtil = prefsUtil;
        this.walletPayloadService = walletPayloadService;
        this.appUtil = appUtil;
        this.aesUtilWrapper = aesUtilWrapper;
        this.accessState = accessState;
        this.stringUtils = stringUtils;
    }

    public Observable<String> getEncryptedPayload(String guid, String sessionId) {
        return walletPayloadService.getEncryptedPayload(guid);
    }

    // TODO: 22/02/2017  
//    public Observable<String> getSessionId(String guid) {
//        return walletPayloadService.getSessionId(guid);
//    }

    public Completable updatePayload(String sharedKey, String guid, String password) {
        return getUpdatePayloadObservable(sharedKey, guid, password)
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
        return Observable.fromCallable(() -> payloadManager.create(walletName, email, password))
            .compose(RxUtil.applySchedulersToObservable())
            .doOnNext(payload -> {
                // Successfully created and saved
                appUtil.setNewlyCreated(true);
                prefsUtil.setValue(PrefsUtil.KEY_GUID, payload.getGuid());
                appUtil.setSharedKey(payload.getSharedKey());
            });
    }

    public Observable<Wallet> restoreHdWallet(String email, String password, String mnemonic) {
        return Observable.fromCallable(() -> payloadManager.recoverFromMnemonic(
            mnemonic, stringUtils.getString(R.string.default_wallet_name), email, password))
            .doOnNext(payload -> {
                if (payload == null) {
                    throw Exceptions.propagate(new Throwable("Save failed"));
                } else {
                    appUtil.setNewlyCreated(true);
                    prefsUtil.setValue(PrefsUtil.KEY_GUID, payload.getGuid());
                    appUtil.setSharedKey(payload.getSharedKey());
                }
            })
            .compose(RxUtil.applySchedulersToObservable());
    }

    public Observable<String> startPollingAuthStatus(String guid) {
        throw new NotImplementedException("");
        // TODO: 21/02/2017
        // Get session id
//        return Observable.interval(2, TimeUnit.SECONDS)
//            // For each emission from the timer, try to get the payload
//            .map(tick -> getEncryptedPayload(guid).blockingFirst())
//            // If auth not required, emit payload
//            .filter(s -> !s.equals("Authorization Required"))
//            // If error called, emit Auth Required
//            .onErrorReturn(throwable -> "Authorization Required")
//            // Make sure threading is correct
//            .compose(RxUtil.applySchedulersToObservable())
//            // Only emit the first object
//            .firstElement()
//            // As Observable rather than Maybe
//            .toObservable();
    }

    private Completable getUpdatePayloadObservable(String sharedKey, String guid, String password) {
        return Completable.defer(() -> Completable.create(subscriber -> {
            try {
                payloadManager.initializeAndDecrypt(
                    sharedKey,
                    guid,
                    password);
                subscriber.onComplete();
            } catch (Exception e) {
                if (!subscriber.isDisposed()) {
                    subscriber.onError(e);
                }
            }
        }));
    }

    public Observable<Integer> createCheckEmailTimer() {
        timer = 2 * 60;

        return Observable.interval(0, 1, TimeUnit.SECONDS, Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(aLong -> timer--)
                .takeUntil(aLong -> timer < 0);
    }

    /*
     * TODO - move to jar and make more testable
     */
    public void attemptDecryptPayload(String password, String guid, String payload, DecryptPayloadListener listener) {
        // TODO: 21/02/2017
//        try {
//            JSONObject jsonObject = new JSONObject(payload);
//
//            if (jsonObject.has("payload")) {
//                String encryptedPayload = jsonObject.getString("payload");
//
//                int iterations = BlockchainWallet.DEFAULT_PBKDF2_ITERATIONS_V2;
//                if (jsonObject.has("pbkdf2_iterations")) {
//                    iterations = jsonObject.getInt("pbkdf2_iterations");
//                }
//
//                String decryptedPayload = null;
//                try {
//                    decryptedPayload = aesUtilWrapper.decrypt(encryptedPayload, password, iterations);
//                } catch (Exception e) {
//                    listener.onFatalError();
//                }
//
//                if (decryptedPayload != null) {
//                    attemptUpdatePayload(password, guid, decryptedPayload, listener);
//                } else {
//                    // Decryption failed
//                    listener.onAuthFail();
//                }
//            }
//        } catch (JSONException e) {
//            // Most likely a V1 Wallet, attempt parse
//            try {
//                BlockchainWallet v1Wallet = new BlockchainWallet(payload, password);
//                attemptUpdatePayload(password, guid, v1Wallet.getPayload().getDecryptedPayload(), listener);
//            } catch (DecryptionException | NullPointerException authException) {
//                Log.e(getClass().getSimpleName(), "attemptDecryptPayload: ", authException);
//                listener.onAuthFail();
//            } catch (Exception fatalException) {
//                Log.e(getClass().getSimpleName(), "attemptDecryptPayload: ", fatalException);
//                listener.onFatalError();
//            }
//        }
    }

    private void attemptUpdatePayload(String password, String guid, String decryptedPayload, DecryptPayloadListener listener) throws JSONException {
        JSONObject decryptedJsonObject = new JSONObject(decryptedPayload);
// TODO: 21/02/2017
//        if (decryptedJsonObject.has("sharedKey")) {
//            prefsUtil.setValue(PrefsUtil.KEY_GUID, guid);
//            payloadManager.setTempPassword(password);
//
//            String sharedKey = decryptedJsonObject.getString("sharedKey");
//            appUtil.setSharedKey(sharedKey);
//
//            updatePayload(sharedKey, guid, password)
//                    .subscribe(new CompletableObserver() {
//                        @Override
//                        public void onSubscribe(Disposable d) {
//                            // No-op
//                        }
//
//                        @Override
//                        public void onComplete() {
//                            prefsUtil.setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
//                            listener.onSuccess();
//                        }
//
//                        @Override
//                        public void onError(Throwable throwable) {
//                            if (throwable instanceof InvalidCredentialsException) {
//                                listener.onAuthFail();
//
//                            } else if (throwable instanceof PayloadException) {
//                                // This shouldn't happen - Payload retrieved from server couldn't be parsed
//                                listener.onFatalError();
//
//                            } else if (throwable instanceof HDWalletException) {
//                                // This shouldn't happen. HD fatal error - not safe to continue - don't clear credentials
//                                listener.onFatalError();
//
//                            } else {
//                                listener.onPairFail();
//                            }
//                        }
//                    });
//        } else {
//            listener.onFatalError();
//        }
    }

    public interface DecryptPayloadListener {

        void onSuccess();

        void onPairFail();

        void onAuthFail();

        void onFatalError();
    }
}
