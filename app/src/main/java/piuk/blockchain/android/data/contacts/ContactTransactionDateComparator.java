package piuk.blockchain.android.data.contacts;

import java.util.Comparator;

/**
 * Compares {@link ContactTransactionModel} objects and sorts them in descending order of date
 * created
 */
public class ContactTransactionDateComparator implements Comparator<ContactTransactionModel> {

    @Override
    public int compare(ContactTransactionModel transaction1, ContactTransactionModel transaction2) {
        return Long.compare(
                transaction1.getFacilitatedTransaction().getCreated(),
                transaction2.getFacilitatedTransaction().getCreated());
    }

}
