package piuk.blockchain.android.data.services;

import info.blockchain.api.PinStore;

import org.json.JSONObject;

import piuk.blockchain.android.data.rxjava.RxUtil;
import rx.Observable;

public class PinStoreService {

    private PinStore pinStore;

    public PinStoreService(PinStore pinStore) {
        this.pinStore = pinStore;
    }


    /**
     * Sends the access key to the server
     *
     * @param key       The PIN identifier
     * @param value     The value, randomly generated
     * @param pin       The user's PIN
     * @return          An {@link Observable<Boolean>} where the boolean represents success
     */
    public Observable<Boolean> setAccessKey(String key, String value, String pin) {
        return Observable.fromCallable(() -> pinStore.setAccess(key, value, pin))
                .map(jsonObject -> jsonObject.has("success"))
                .compose(RxUtil.applySchedulers());
    }

    /**
     * Validates a user's PIN with the server
     *
     * @param key   The PIN identifier
     * @param pin   The user's PIN
     * @return      A {@link JSONObject} which may or may not contain the field "success"
     */
    public Observable<JSONObject> validateAccess(String key, String pin) {
        return Observable.fromCallable(() -> pinStore.validateAccess(key, pin))
                .compose(RxUtil.applySchedulers());
    }
}
