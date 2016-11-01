package piuk.blockchain.android.injection;

import info.blockchain.wallet.payload.PayloadManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import piuk.blockchain.android.data.stores.TransactionListStore;

/**
 * Created by adambennett on 08/08/2016.
 */

@Module
public class ApiModule {

    @Provides
    protected PayloadManager providePayloadManager() {
        return PayloadManager.getInstance();
    }

    @Provides
    @Singleton
    protected TransactionListStore provideTransactionListStore() {
        return new TransactionListStore();
    }

}
