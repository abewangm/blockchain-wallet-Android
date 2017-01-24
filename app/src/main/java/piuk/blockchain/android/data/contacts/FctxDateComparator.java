package piuk.blockchain.android.data.contacts;

import info.blockchain.wallet.contacts.data.FacilitatedTransaction;

import java.util.Comparator;

/**
 * Compares {@link FacilitatedTransaction} objects and sorts them in ascending order of date created
 */
public class FctxDateComparator implements Comparator<FacilitatedTransaction> {

    @Override
    public int compare(FacilitatedTransaction transaction1, FacilitatedTransaction transaction2) {
        final int before = -1;
        final int equal = 0;
        final int after = 1;

        if (transaction1.getCreated() < transaction2.getCreated()) {
            return before;
        } else if (transaction1.getCreated() < transaction2.getCreated()) {
            return after;
        } else {
            return equal;
        }
    }

}