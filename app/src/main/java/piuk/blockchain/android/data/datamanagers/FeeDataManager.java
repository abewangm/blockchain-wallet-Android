package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.api.FeeApi;
import info.blockchain.wallet.api.data.FeeOptions;

import io.reactivex.Observable;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxPinning;
import piuk.blockchain.android.data.rxjava.RxUtil;

public class FeeDataManager {

    private final RxPinning rxPinning;
    private FeeApi feeApi;

    public FeeDataManager(FeeApi feeApi, RxBus rxBus) {
        this.feeApi = feeApi;
        rxPinning = new RxPinning(rxBus);
    }

    /**
     * Returns a {@link FeeOptions} object which contains both a "regular" and a "priority" fee
     * option, both listed in Satoshis/byte.
     *
     * @return An {@link Observable} wrapping a {@link FeeOptions} object
     */
    public Observable<FeeOptions> getFeeOptions() {
        return rxPinning.call(() -> feeApi.getFeeOptions())
                .compose(RxUtil.applySchedulersToObservable());
    }

}
