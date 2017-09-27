package piuk.blockchain.android.data.stores

import io.reactivex.Observable

interface PersistentStore<T> {

    fun store(data: T): Observable<T>

    fun invalidate()

}