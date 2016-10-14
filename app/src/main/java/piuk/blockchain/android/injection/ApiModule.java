package piuk.blockchain.android.injection;

import info.blockchain.wallet.payload.PayloadManager;

import dagger.Module;
import dagger.Provides;

/**
 * Created by adambennett on 08/08/2016.
 */

@Module
public class ApiModule {

    @Provides
    protected PayloadManager providePayloadManager() {
        return PayloadManager.getInstance();
    }

}
