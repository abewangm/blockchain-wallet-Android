package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.api.FeeApi;
import info.blockchain.wallet.api.data.FeeOptions;

import io.reactivex.Observable;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxPinning;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.walletoptions.WalletOptionsDataManager;

public class FeeDataManager {

    private final RxPinning rxPinning;
    private FeeApi feeApi;

    //Bitcoin cash fees are temporarily getched from wallet-options until an endpoint can be provided
    private WalletOptionsDataManager walletOptionsDataManager;

    public FeeDataManager(FeeApi feeApi, WalletOptionsDataManager walletOptionsDataManager, RxBus rxBus) {
        this.feeApi = feeApi;
        this.walletOptionsDataManager = walletOptionsDataManager;
        rxPinning = new RxPinning(rxBus);
    }

    /**
     * Returns a {@link FeeOptions} object which contains both a "regular" and a "priority" fee
     * option, both listed in Satoshis/byte.
     *
     * @return An {@link Observable} wrapping a {@link FeeOptions} object
     */
    public Observable<FeeOptions> getBtcFeeOptions() {
        return rxPinning.call(() -> feeApi.getFeeOptions())
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Returns a {@link FeeOptions} object which contains both a "regular" and a "priority" fee
     * option for Ethereum.
     *
     * @return An {@link Observable} wrapping a {@link FeeOptions} object
     */
    public Observable<FeeOptions> getEthFeeOptions() {
        return rxPinning.call(() -> feeApi.getEthFeeOptions())
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Returns a {@link FeeOptions} object which contains a "regular" fee
     * option, both listed in Satoshis/byte.
     *
     * @return An {@link Observable} wrapping a {@link FeeOptions} object
     */
    public Observable<FeeOptions> getBchFeeOptions() {
        return Observable.just(createBchFeeOptions());
    }

    private FeeOptions createBchFeeOptions() {
        FeeOptions feeOptions = new FeeOptions();
        feeOptions.setRegularFee(walletOptionsDataManager.getBchFee());
        return feeOptions;
    }
}
