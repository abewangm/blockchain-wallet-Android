package piuk.blockchain.android.injection;

import android.app.Application;
import android.content.Context;

/**
 * Created by adambennett on 08/08/2016.
 */

public enum Injector {

    INSTANCE;

    private ApplicationComponent applicationComponent;
    private DataManagerComponent dataManagerComponent;
    private MetaDataComponent metaDataComponent;

    public static Injector getInstance() {
        return INSTANCE;
    }

    public void init(Context applicationContext) {

        ApplicationModule applicationModule = new ApplicationModule((Application) applicationContext);
        ApiModule apiModule = new ApiModule();
        DataManagerModule managerModule = new DataManagerModule();
        MetaDataModule metaDataModule = new MetaDataModule();

        initAppComponent(applicationModule, apiModule, managerModule);
        initMetaDataComponent(metaDataModule);
    }

    protected void initAppComponent(ApplicationModule applicationModule, ApiModule apiModule, DataManagerModule managerModule) {

        applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(applicationModule)
                .apiModule(apiModule)
                .build();

        dataManagerComponent = applicationComponent.plus(managerModule);
    }

    protected void initMetaDataComponent(MetaDataModule metaDataModule) {

        metaDataComponent = DaggerMetaDataComponent.builder()
                .metaDataModule(metaDataModule)
                .build();
    }

    public ApplicationComponent getAppComponent() {
        return applicationComponent;
    }

    public MetaDataComponent getMetaDataComponent() {
        return metaDataComponent;
    }

    public DataManagerComponent getDataManagerComponent() {
        if (dataManagerComponent == null) {
            dataManagerComponent = applicationComponent.plus(new DataManagerModule());
        }
        return dataManagerComponent;
    }

    public void releaseViewModelScope() {
        dataManagerComponent = null;
    }
}
