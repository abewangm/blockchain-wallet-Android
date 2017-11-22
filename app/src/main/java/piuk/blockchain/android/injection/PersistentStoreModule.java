package piuk.blockchain.android.injection;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import piuk.blockchain.android.data.contacts.datastore.ContactsMapStore;
import piuk.blockchain.android.data.ethereum.EthDataStore;
import piuk.blockchain.android.data.settings.SettingsService;
import piuk.blockchain.android.data.settings.datastore.SettingsDataStore;
import piuk.blockchain.android.data.settings.datastore.SettingsMemoryStore;
import piuk.blockchain.android.data.shapeshift.datastore.ShapeShiftDataStore;
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
    ContactsMapStore provideContactsMapStore() {
        return new ContactsMapStore();
    }

    @Provides
    @Singleton
    EthDataStore provideEthWallet() {
        return new EthDataStore();
    }

    @Provides
    @Singleton
    ShapeShiftDataStore provideShapeShiftDataStore() {
        return new ShapeShiftDataStore();
    }

}
