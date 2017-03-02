package piuk.blockchain.android.data.stores;

import info.blockchain.api.data.Transaction;
import info.blockchain.wallet.multiaddress.MultiAddressFactory;

import java.util.List;

public class TransactionListStore extends ListStore<Transaction> {

    public TransactionListStore() {
        // Empty constructor
    }

    public void insertTransactionIntoListAndSort(Transaction transaction) {
        insertObjectIntoList(transaction);
        sort(new MultiAddressFactory.TxMostRecentDateComparator());
    }

    public void insertTransactions(List<Transaction> transactions) {
        insertBulk(transactions);
    }
}
