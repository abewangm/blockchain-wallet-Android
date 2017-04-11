package piuk.blockchain.android.data.stores;

import java.util.List;

import piuk.blockchain.android.data.contacts.ContactTransactionModel;

public class PendingTransactionListStore extends ListStore<ContactTransactionModel> {

    public PendingTransactionListStore() {
        // Empty constructor
    }

    public void insertTransaction(ContactTransactionModel transaction) {
        insertObjectIntoList(transaction);
    }

    public void insertTransactions(List<ContactTransactionModel> transactions) {
        insertBulk(transactions);
    }

    public void removeTransaction(ContactTransactionModel object) {
        removeObjectFromList(object);
    }
}

