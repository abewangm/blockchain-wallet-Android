package piuk.blockchain.android.injection;

import info.blockchain.api.Notifications;
import info.blockchain.wallet.payload.PayloadManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.notifications.NotificationTokenManager;
import piuk.blockchain.android.data.services.NotificationService;
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

    @Provides
    @Singleton
    protected NotificationTokenManager provideNotificationTokenManager(AccessState accessState,
                                                                       PayloadManager payloadManager) {

        return new NotificationTokenManager(
                new NotificationService(new Notifications()), accessState, payloadManager);
    }

}
