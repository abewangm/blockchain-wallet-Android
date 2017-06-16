package piuk.blockchain.android.data.settings.datastore

import info.blockchain.wallet.api.data.Settings
import io.reactivex.Observable
import piuk.blockchain.android.data.stores.Optional
import piuk.blockchain.android.data.stores.PersistentStore


class SettingsMemoryStore: SettingsStore, PersistentStore<Settings> {

    var settings: Optional<Settings> = Optional.None

    override fun store(data: Settings): io.reactivex.Observable<Settings> {
        settings = piuk.blockchain.android.data.stores.Optional.Some(data)
        return Observable.just((settings as Optional.Some<Settings>).element)
    }

    override fun getSettings(): Observable<Optional<Settings>> = Observable.just(settings)

    override fun invalidate() {
        settings = Optional.None
    }

}