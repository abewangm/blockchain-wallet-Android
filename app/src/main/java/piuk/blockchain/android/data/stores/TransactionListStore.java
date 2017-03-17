package piuk.blockchain.android.data.stores;

import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.multiaddress.TransactionSummary.TxMostRecentDateComparator;
import java.util.List;

public class TransactionListStore extends ListStore<TransactionSummary> {

    public TransactionListStore() {
        // Empty constructor
    }

    public void insertTransactionIntoListAndSort(TransactionSummary transaction) {
        insertObjectIntoList(transaction);
        sort(new TxMostRecentDateComparator());
    }

    public void insertTransactions(List<TransactionSummary> transactions) {
        insertBulk(transactions);
        sort(new TxMostRecentDateComparator());
    }
}
