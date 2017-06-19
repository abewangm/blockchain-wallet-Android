package piuk.blockchain.android.data.stores

import io.reactivex.Observable

/**
 * Attempts to fetch from local storage and if not found, triggers a webcall to obtain the data.
 */
class DefaultFetchStrategy<T>(
        private val webSource: Observable<T>,
        private val memorySource: Observable<Optional<T>>,
        private val memoryStore: PersistentStore<T>
) : FetchStrategy<T>() {

    /**
     * Return first source with data and store result
     */
    override fun fetch(): Observable<T> = memorySource.flatMap {
        optional ->
        when (optional) {
            is Optional.Some -> Observable.just(optional.element)
            else -> webSource
        }
    }.flatMap(memoryStore::store)


}