package piuk.blockchain.android.injection;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import piuk.blockchain.android.data.settings.SettingsService;
import piuk.blockchain.android.data.settings.datastore.SettingsDataStore;
import piuk.blockchain.android.data.settings.datastore.SettingsMemoryStore;

@Module
class PersistentStoreModule {

    @Provides
    @Singleton
    SettingsDataStore provideSettingsDataStore(SettingsService settingsService) {
        return new SettingsDataStore(new SettingsMemoryStore(), settingsService.getSettingsObservable());
    }

}
