package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import io.reactivex.Observable;
import piuk.blockchain.android.data.rxjava.RxUtil;

public class ReceiveDataManager {

    private PayloadManager payloadManager;

    public ReceiveDataManager(PayloadManager payloadManager) {
        this.payloadManager = payloadManager;
    }

    public Observable<String> getNextReceiveAddress(Account account) {
        return Observable.fromCallable(() -> payloadManager.getNextReceiveAddress(account))
            .compose(RxUtil.applySchedulersToObservable());
    }

    public Observable<String> getNextChangeAddress(Account account) {
        return Observable.fromCallable(() -> payloadManager.getNextChangeAddress(account))
            .compose(RxUtil.applySchedulersToObservable());
    }
}
