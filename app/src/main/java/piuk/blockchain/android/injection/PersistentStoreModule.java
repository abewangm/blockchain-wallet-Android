package piuk.blockchain.android.injection;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import piuk.blockchain.android.data.contacts.datastore.ContactsMapStore;
import piuk.blockchain.android.data.ethereum.EthService;
import piuk.blockchain.android.data.ethereum.datastore.EthDataStore;
import piuk.blockchain.android.data.ethereum.datastore.EthMemoryStore;
import piuk.blockchain.android.data.settings.SettingsService;
import piuk.blockchain.android.data.settings.datastore.SettingsDataStore;
import piuk.blockchain.android.data.settings.datastore.SettingsMemoryStore;
import piuk.blockchain.android.data.stores.PendingTransactionListStore;
import piuk.blockchain.android.data.stores.TransactionListStore;

@Module
class PersistentStoreModule {

    @Provides
    @Singleton
    SettingsDataStore provideSettingsDataStore(SettingsService settingsService) {
        return new SettingsDataStore(new SettingsMemoryStore(), settingsService.getSettingsObservable());
    }

    @Provides
    @Singleton
    EthDataStore provideEthDataStore(EthService ethService) {
        return new EthDataStore(new EthMemoryStore(), ethService.getEthAccount());
    }

    @Provides
    @Singleton
    PendingTransactionListStore providePendingTransactionListStore() {
        return new PendingTransactionListStore();
    }

    @Provides
    @Singleton
    TransactionListStore provideTransactionListStore() {
        return new TransactionListStore();
    }

    @Provides
    @Singleton
    ContactsMapStore provideContactsMapStore(){
        return new ContactsMapStore();
    }

}
