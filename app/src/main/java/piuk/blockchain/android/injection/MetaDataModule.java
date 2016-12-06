package piuk.blockchain.android.injection;

import info.blockchain.wallet.metadata.Metadata;
import info.blockchain.wallet.metadata.MetadataShared;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import piuk.blockchain.android.data.metadata.TokenMemoryStore;
import piuk.blockchain.android.data.metadata.TokenWebStore;
import piuk.blockchain.android.data.services.MetadataService;
import piuk.blockchain.android.data.services.SharedMetadataService;

@Module
public class MetaDataModule {

    @Provides
    @Singleton
    protected SharedMetadataService provideSharedMetaDataService() {
        return new SharedMetadataService(new MetadataShared());
    }

    @Provides
    @Singleton
    protected MetadataService provideMetaDataService() {
        return new MetadataService(new Metadata());
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
