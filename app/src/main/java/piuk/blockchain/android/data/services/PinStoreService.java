package piuk.blockchain.android.data.services;

import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.api.data.Status;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;

import org.json.JSONObject;

import piuk.blockchain.android.data.rxjava.RxUtil;
import io.reactivex.Observable;
import retrofit2.Response;

public class PinStoreService {

    /**
     * Sends the access key to the server
     *
     * @param key       The PIN identifier
     * @param value     The value, randomly generated
     * @param pin       The user's PIN
     * @return          An {@link Observable<Boolean>} where the boolean represents success
     */
    public Observable<Response<Status>> setAccessKey(String key, String value, String pin) {
        return Observable.fromCallable(() -> WalletApi.setAccess(key, value, pin).execute())
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Validates a user's PIN with the server
     *
     * @param key   The PIN identifier
     * @param pin   The user's PIN
     * @return      A {@link JSONObject} which may or may not contain the field "success"
     */
    public Observable<Response<Status>> validateAccess(String key, String pin) {
        return createValidateAccessObservable(key, pin)
                .compose(RxUtil.applySchedulersToObservable());
    }

    private Observable<Response<Status>> createValidateAccessObservable(String key, String pin) {
        return Observable.create(observableEmitter -> {
            try {
                Response<Status> response = WalletApi.validateAccess(key, pin).execute();
                if (!observableEmitter.isDisposed()) {
                    observableEmitter.onNext(response);
                    observableEmitter.onComplete();
                }
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("Incorrect PIN")) {
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onError(new InvalidCredentialsException("Incorrect PIN"));
                    }
                } else {
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onError(e);
                    }
                }
            }
        });
    }
}
