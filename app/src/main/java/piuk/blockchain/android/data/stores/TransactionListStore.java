package piuk.blockchain.android.data.stores;

import info.blockchain.wallet.multiaddress.TransactionSummary;
import java.util.List;

public class TransactionListStore extends ListStore<TransactionSummary> {

    public TransactionListStore() {
        // Empty constructor
    }

    public void insertTransactionIntoListAndSort(TransactionSummary transaction) {
        insertObjectIntoList(transaction);
    }

    public void insertTransactions(List<TransactionSummary> transactions) {
        insertBulk(transactions);
    }
}
