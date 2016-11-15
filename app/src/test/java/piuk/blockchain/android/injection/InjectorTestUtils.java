package piuk.blockchain.android.injection;

import piuk.blockchain.android.data.metadata.di.MetaDataModule;

/**
 * Created by adambennett on 08/08/2016.
 *
 * A utils class for injecting mock modules during testing
 */
public class InjectorTestUtils {

    public static void initApplicationComponent(Injector injector,
                                                ApplicationModule applicationModule,
                                                ApiModule apiModule,
                                                DataManagerModule managerModule) {
        injector.initAppComponent(applicationModule, apiModule, managerModule);
    }

    public static void initMetaDataComponent(Injector injector,
                                                ApplicationModule applicationModule,
                                                ApiModule apiModule,
                                                MetaDataModule metaDataModule) {
        injector.initMetaDataComponent(applicationModule, apiModule, metaDataModule);
    }

}