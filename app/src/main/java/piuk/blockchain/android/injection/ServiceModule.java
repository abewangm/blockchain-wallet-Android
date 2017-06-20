package piuk.blockchain.android.injection;

import info.blockchain.wallet.settings.SettingsManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import piuk.blockchain.android.data.settings.SettingsService;

@Module
class ServiceModule {

    @Provides
    @Singleton
    SettingsService provideSettingsService() {
        return new SettingsService(new SettingsManager());
    }

}
