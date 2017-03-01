package piuk.blockchain.android.data.services;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.data.LegacyAddress;

import java.util.Collections;

import io.reactivex.Observable;
import piuk.blockchain.android.data.rxjava.RxUtil;

public class AddressInfoService {

    private MultiAddrFactory multiAddrFactory;

    public AddressInfoService(MultiAddrFactory multiAddrFactory) {
        this.multiAddrFactory = multiAddrFactory;
    }

    /**
     * Returns the current balance for a given {@link LegacyAddress}
     *
     * @param address The {@link LegacyAddress} which you want to know the balance of
     * @return An {@link Observable<Long>} wrapping the balance of the address
     */
    public Observable<Long> getAddressBalance(LegacyAddress address) {
        return Observable.fromCallable(() -> multiAddrFactory.getAddressBalanceFromApi(
                Collections.singletonList(address.getAddress())))
                .map(map -> map.get(address.getAddress()))
                .compose(RxUtil.applySchedulersToObservable());
    }
}
