package piuk.blockchain.android.data.stores

import io.reactivex.Observable

abstract class FetchStrategy<T> {

    abstract fun fetch(): Observable<T>

}