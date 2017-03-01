package piuk.blockchain.android.data.datamanagers;

import android.support.annotation.Nullable;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.util.PrivateKeyFactory;

import org.bitcoinj.core.ECKey;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.AddressInfoService;

public class AccountDataManager {

    private PayloadManager payloadManager;
    private MultiAddrFactory multiAddrFactory;
    private AddressInfoService addressInfoService;

    public AccountDataManager(PayloadManager payload, MultiAddrFactory addrFactory, AddressInfoService addressService) {
        payloadManager = payload;
        multiAddrFactory = addrFactory;
        addressInfoService = addressService;
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
     * Allows you to propagate changes to a {@link LegacyAddress} through the {@link
     * info.blockchain.wallet.payload.data.Wallet} and the {@link MultiAddrFactory}
     *
     * @param legacyAddress The updated address
     * @return {@link Observable<Boolean>} representing a successful save
     */
    public Observable<Long> updateLegacyAddress(LegacyAddress legacyAddress) {
        return createUpdateLegacyAddressObservable(legacyAddress)
                .compose(RxUtil.applySchedulersToObservable());
    }

    private Observable<Long> createUpdateLegacyAddressObservable(LegacyAddress address) {
        return Completable.fromCallable(() -> {
            payloadManager.addLegacyAddress(address);
            return Void.TYPE;
        }).andThen(addAddressAndUpdate(address));
    }

    private Observable<Long> addAddressAndUpdate(LegacyAddress address) {
        return refreshMultiAddressData()
                .andThen(addressInfoService.getAddressBalance(address))
                .doOnNext(balance -> {
                    multiAddrFactory.setLegacyBalance(address.getAddress(), balance);
                    multiAddrFactory.setLegacyBalance(MultiAddrFactory.getInstance().getLegacyBalance() + balance);
                });
    }

    private Completable refreshMultiAddressData() {
        List<String> legacyAddressList = payloadManager.getPayload().getLegacyAddressStringList();
        return Completable.fromCallable(() -> {
            multiAddrFactory.refreshLegacyAddressData(legacyAddressList, false);
            return Void.TYPE;
        });
    }

    public Observable<ECKey> getKeyFromImportedData(String format, String data) {
        return Observable.fromCallable(() -> PrivateKeyFactory.getKey(format, data))
                .compose(RxUtil.applySchedulersToObservable());
    }
}
