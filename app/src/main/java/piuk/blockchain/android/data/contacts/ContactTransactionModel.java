package piuk.blockchain.android.data.contacts;

import android.support.annotation.NonNull;

import info.blockchain.wallet.contacts.data.FacilitatedTransaction;

/**
 * Simple wrapper object for convenient list display
 */
public class ContactTransactionModel {

    private String contactName;
    private FacilitatedTransaction facilitatedTransaction;

    public ContactTransactionModel(@NonNull String contactName, @NonNull FacilitatedTransaction facilitatedTransaction) {
        this.contactName = contactName;
        this.facilitatedTransaction = facilitatedTransaction;
    }

    @NonNull
    public String getContactName() {
        return contactName;
    }

    @NonNull
    public FacilitatedTransaction getFacilitatedTransaction() {
        return facilitatedTransaction;
    }
}
