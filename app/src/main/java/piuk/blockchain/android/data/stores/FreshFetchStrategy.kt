package piuk.blockchain.android.data.stores

import io.reactivex.Observable
import piuk.blockchain.android.data.rxjava.RxUtil

/**
 * Fetches data from the web and then stores it in memory
 */
class FreshFetchStrategy<T>(
        private val webSource: Observable<T>,
        private val memoryStore: PersistentStore<T>
) : FetchStrategy<T>() {

    override fun fetch(): Observable<T> {
       return webSource.flatMap(memoryStore::store)
                .compose(RxUtil.applySchedulersToObservable())
    }

}