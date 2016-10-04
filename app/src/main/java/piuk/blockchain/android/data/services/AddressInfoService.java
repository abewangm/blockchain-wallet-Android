package piuk.blockchain.android.data.services;

import info.blockchain.api.AddressInfo;
import info.blockchain.wallet.payload.LegacyAddress;

import org.json.JSONException;
import org.json.JSONObject;

import piuk.blockchain.android.data.rxjava.RxUtil;
import rx.Observable;

public class AddressInfoService {

    public static final String PARAMETER_FINAL_BALANCE = "&limit=0";
    private static final String KEY_FINAL_BALANCE = "final_balance";

    private AddressInfo addressInfo;

    public AddressInfoService(AddressInfo info) {
        addressInfo = info;
    }

    public Observable<Long> getAddressBalance(LegacyAddress address, String parameter) {
        return createGetAddressBalanceObservable(address, parameter)
                .compose(RxUtil.applySchedulers());
    }

    private Observable<Long> createGetAddressBalanceObservable(LegacyAddress address, String parameter) {
        return Observable.create(subscriber -> {
            JSONObject response = addressInfo.getAddressInfo(address.getAddress(), parameter);
            try {
                if (subscriber.isUnsubscribed()) return;
                subscriber.onNext(response.getLong(KEY_FINAL_BALANCE));
                subscriber.onCompleted();
            } catch (JSONException e) {
                subscriber.onError(e);
            }
        });
    }
}
