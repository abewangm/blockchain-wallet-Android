package piuk.blockchain.android.data.settings.datastore

import info.blockchain.wallet.api.data.Settings
import io.reactivex.Observable
import piuk.blockchain.android.data.stores.DefaultFetchStrategy
import piuk.blockchain.android.data.stores.FreshFetchStrategy

class SettingsDataStore(
        val memoryStore: SettingsMemoryStore,
        val webSource: Observable<Settings>
) {

    fun getSettings(): Observable<Settings> =
            DefaultFetchStrategy(webSource, memoryStore.getSettings(), memoryStore).fetch()

    fun fetchSettings(): Observable<Settings> =
            FreshFetchStrategy(webSource, memoryStore).fetch()

}