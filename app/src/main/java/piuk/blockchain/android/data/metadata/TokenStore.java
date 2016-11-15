package piuk.blockchain.android.data.metadata;

import io.reactivex.Observable;

interface TokenStore {

    Observable<String> getToken();
}
