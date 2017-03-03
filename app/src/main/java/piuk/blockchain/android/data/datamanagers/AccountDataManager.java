package piuk.blockchain.android.data.datamanagers;

import android.support.annotation.Nullable;

import info.blockchain.api.data.MultiAddress;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.util.PrivateKeyFactory;

import org.bitcoinj.core.ECKey;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.BlockExplorerService;

public class AccountDataManager {

    private PayloadManager payloadManager;
    private BlockExplorerService blockExplorerService;

    public AccountDataManager(PayloadManager payload, BlockExplorerService blockExplorer) {
        payloadManager = payload;
        blockExplorerService = blockExplorer;
    }

    /**
     * Derives new {@link Account} from the master seed
     *
     * @param accountLabel   A label for the account
     * @param secondPassword An optional double encryption password
     * @return An {@link Observable<Account>} wrapping the newly created Account
     */
    public Observable<Account> createNewAccount(String accountLabel, @Nullable String secondPassword) {
        return Observable.fromCallable(() -> payloadManager.addAccount(accountLabel, secondPassword))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Sets a private key for an associated {@link LegacyAddress} which is already in the {@link
     * info.blockchain.wallet.payload.data.Wallet} as a watch only address
     *
     * @param key            An {@link ECKey}
     * @param secondPassword An optional double encryption password
     * @return An {@link Observable<Boolean>} representing a successful save
     */
    public Observable<LegacyAddress> setPrivateKey(ECKey key, @Nullable String secondPassword) {
        return Observable.fromCallable(() -> payloadManager.setKeyForLegacyAddress(key, secondPassword))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Sets a private key for a {@link LegacyAddress}
     *
     * @param key            The {@link ECKey} for the address
     * @param secondPassword An optional double encryption password
     */
    public Observable<LegacyAddress> setKeyForLegacyAddress(ECKey key, @Nullable String secondPassword) throws Exception {
        return Observable.fromCallable(() -> payloadManager.setKeyForLegacyAddress(key, secondPassword))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Allows you to propagate changes to a {@link LegacyAddress} through the {@link info.blockchain.wallet.payload.data.Wallet}
     *
     * @param legacyAddress The updated address
     * @return {@link Observable<Boolean>} representing a successful save
     */
    public Completable updateLegacyAddress(LegacyAddress legacyAddress) {

        return Completable.fromCallable(() -> {
            payloadManager.addLegacyAddress(legacyAddress);
            return Void.TYPE;
        });
    }


    public Observable<ECKey> getKeyFromImportedData(String format, String data) {
        return Observable.fromCallable(() -> PrivateKeyFactory.getKey(format, data))
                .compose(RxUtil.applySchedulersToObservable());
    }
}
