package piuk.blockchain.android.data.contacts;

import info.blockchain.wallet.contacts.data.FacilitatedTransaction;

import java.util.Comparator;

/**
 * Compares {@link FacilitatedTransaction} objects and sorts them in descending order of date created
 */
public class FctxDateComparator implements Comparator<FacilitatedTransaction> {

    @Override
    public int compare(FacilitatedTransaction transaction1, FacilitatedTransaction transaction2) {
        return Long.compare(transaction1.getCreated(), transaction2.getCreated());
    }

}