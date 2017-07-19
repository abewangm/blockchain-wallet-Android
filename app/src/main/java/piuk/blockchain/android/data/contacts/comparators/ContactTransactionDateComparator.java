package piuk.blockchain.android.data.contacts.comparators;

import java.util.Comparator;

import piuk.blockchain.android.data.contacts.models.ContactTransactionModel;

/**
 * Compares {@link ContactTransactionModel} objects and sorts them in descending order of date
 * created
 */
public class ContactTransactionDateComparator implements Comparator<ContactTransactionModel> {

    @Override
    public int compare(ContactTransactionModel transaction1, ContactTransactionModel transaction2) {
        return Long.compare(
                transaction1.getFacilitatedTransaction().getLastUpdated(),
                transaction2.getFacilitatedTransaction().getLastUpdated());
    }

}
