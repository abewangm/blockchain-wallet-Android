package piuk.blockchain.android.data.stores;

import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.multiaddress.TransactionSummary.TxMostRecentDateComparator;

import java.util.HashMap;
import java.util.List;

/**
 * Contains both a list of {@link TransactionSummary} objects and also a Map of transaction
 * confirmations keyed to their Transaction's hash.
 */
public class TransactionListStore extends ListStore<TransactionSummary> {

    private HashMap<String, Integer> txConfirmationsMap = new HashMap<>();

    public TransactionListStore() {
        // Empty constructor
    }

    public void insertTransactionIntoListAndSort(TransactionSummary transaction) {
        insertObjectIntoList(transaction);
        getTxConfirmationsMap().put(transaction.getHash(), transaction.getConfirmations());
        sort(new TxMostRecentDateComparator());
    }

    public void insertTransactions(List<TransactionSummary> transactions) {
        insertBulk(transactions);
        for (TransactionSummary summary : transactions) {
            getTxConfirmationsMap().put(summary.getHash(), summary.getConfirmations());
        }
        sort(new TxMostRecentDateComparator());
    }

    /**
     * Returns a {@link HashMap} where a {@link TransactionSummary} hash is used as a key against
     * the confirmation number. This is for displaying the confirmation number in the Contacts page.
     */
    public HashMap<String, Integer> getTxConfirmationsMap() {
        return txConfirmationsMap;
    }

}
