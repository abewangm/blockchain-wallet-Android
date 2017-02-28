package piuk.blockchain.android.data.services;

import info.blockchain.api.blockexplorer.BlockExplorer;
import info.blockchain.api.data.Address;
import info.blockchain.wallet.payload.data.LegacyAddress;
import io.reactivex.Observable;
import piuk.blockchain.android.data.rxjava.RxUtil;

public class BlockExplorerService {

    // TODO: 28/02/2017 WIP 
    private BlockExplorer blockExplorer;

    public BlockExplorerService(BlockExplorer blockExplorer) {
        this.blockExplorer = blockExplorer;
    }

    /**
     * Returns the current balance for a given {@link LegacyAddress}
     *
     * @param address   The address which you want to know the balance of
     * @param limit
     * @param offset
     * @return          An {@link Observable <Long>} wrapping the balance of the address
     */
    public Observable<Address> getAddressBalance(String address, int limit, int offset) {
        return Observable.fromCallable(() -> blockExplorer.getAddress(address, limit, offset))
                .flatMap(call -> Observable.fromCallable(() -> call.execute().body()))
                .compose(RxUtil.applySchedulersToObservable());
    }
}
