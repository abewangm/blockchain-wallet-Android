package piuk.blockchain.android.data.datamanagers;

import org.bitcoinj.core.Transaction;
import org.web3j.tx.Transfer;

import info.blockchain.wallet.api.Environment;
import info.blockchain.wallet.api.FeeApi;
import info.blockchain.wallet.api.data.FeeLimits;
import info.blockchain.wallet.api.data.FeeOptions;
import io.reactivex.Observable;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxPinning;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.walletoptions.WalletOptionsDataManager;

public class FeeDataManager {

    private final RxPinning rxPinning;
    private FeeApi feeApi;
    private EnvironmentSettings environmentSettings;

    //Bitcoin cash fees are temporarily fetched from wallet-options until an endpoint can be provided
    private WalletOptionsDataManager walletOptionsDataManager;

    public FeeDataManager(FeeApi feeApi, WalletOptionsDataManager walletOptionsDataManager, EnvironmentSettings environmentSettings, RxBus rxBus) {
        this.feeApi = feeApi;
        this.walletOptionsDataManager = walletOptionsDataManager;
        this.environmentSettings = environmentSettings;
        rxPinning = new RxPinning(rxBus);
    }

    /**
     * Returns a {@link FeeOptions} object which contains both a "regular" and a "priority" fee
     * option, both listed in Satoshis/byte.
     *
     * @return An {@link Observable} wrapping a {@link FeeOptions} object
     */
    public Observable<FeeOptions> getBtcFeeOptions() {
        if (environmentSettings.getEnvironment().equals(Environment.TESTNET)) {
            return Observable.just(createTestnetFeeOptions());
        } else {
            return rxPinning.call(() -> feeApi.getFeeOptions())
                    .compose(RxUtil.applySchedulersToObservable());
        }
    }

    /**
     * Returns a {@link FeeOptions} object which contains both a "regular" and a "priority" fee
     * option for Ethereum.
     *
     * @return An {@link Observable} wrapping a {@link FeeOptions} object
     */
    public Observable<FeeOptions> getEthFeeOptions() {
        if (environmentSettings.getEnvironment().equals(Environment.TESTNET)) {
            //No Test environment for Eth
            return Observable.just(createTestnetFeeOptions());
        } else {
            return rxPinning.call(() -> feeApi.getEthFeeOptions())
                    .compose(RxUtil.applySchedulersToObservable());
        }
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
        feeOptions.setPriorityFee(walletOptionsDataManager.getBchFee());
        return feeOptions;
    }

    private FeeOptions createTestnetFeeOptions() {
        FeeOptions feeOptions = new FeeOptions();
        feeOptions.setRegularFee(Transaction.DEFAULT_TX_FEE.longValue());
        feeOptions.setPriorityFee(Transaction.DEFAULT_TX_FEE.longValue());
        feeOptions.setLimits(new FeeLimits(23, 23));
        feeOptions.setGasLimit(Transfer.GAS_LIMIT.longValue());
        return feeOptions;
    }
}
