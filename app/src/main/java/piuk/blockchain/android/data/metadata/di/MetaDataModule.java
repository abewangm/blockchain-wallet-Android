package piuk.blockchain.android.data.metadata.di;

import info.blockchain.api.metadata.Metadata;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import piuk.blockchain.android.data.metadata.TokenMemoryStore;
import piuk.blockchain.android.data.metadata.TokenWebStore;
import piuk.blockchain.android.data.services.SharedMetaDataService;

@Module
public class MetaDataModule {

    @Provides
    @Singleton
    protected SharedMetaDataService provideSharedMetaDataService() {
        return new SharedMetaDataService(new Metadata());
    }

    @Provides
    @Singleton
    protected TokenMemoryStore provideTokenMemoryStore() {
        return new TokenMemoryStore();
    }

    @Provides
    @Singleton
    protected TokenWebStore provideTokenWebStore() {
        return new TokenWebStore();
    }
}
