package piuk.blockchain.android.injection;

import javax.inject.Singleton;

import dagger.Component;
import piuk.blockchain.android.data.datamanagers.MetaDataManager;
import piuk.blockchain.android.data.metadata.TokenWebStore;

@Singleton
@Component(modules = {
        ApplicationModule.class,
        ApiModule.class,
        MetaDataModule.class
})
public interface MetaDataComponent {

    void inject(TokenWebStore tokenWebStore);

    void inject(MetaDataManager metaDataManager);
}
