package piuk.blockchain.android.ui.metadata;

import android.support.annotation.NonNull;

public class ContactsListItem {

    private String mdid;
    private String contactName;
    private String status;

    public ContactsListItem(@NonNull String mdid, @NonNull String contactName, @NonNull String status) {
        this.mdid = mdid;
        this.contactName = contactName;
        this.status = status;
    }

    public String getContactName() {
        return contactName;
    }

    public String getMdid() {
        return mdid;
    }

    public String getStatus() {
        return status;
    }
}
