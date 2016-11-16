package piuk.blockchain.android.ui.contacts;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class ContactsListItem {

    private String mdid;
    private String contactName;
    private String status;

    ContactsListItem(@NonNull String mdid, @NonNull String contactName, @NonNull String status) {
        this.mdid = mdid;
        this.contactName = contactName;
        this.status = status;
    }

    @Nullable
    String getContactName() {
        return contactName;
    }

    @NonNull
    String getMdid() {
        return mdid;
    }

    @NonNull
    public String getStatus() {
        return status;
    }
}
