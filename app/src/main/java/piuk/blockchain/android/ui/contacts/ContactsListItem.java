package piuk.blockchain.android.ui.contacts;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class ContactsListItem {

    private String id;
    private String contactName;
    private String status;

    ContactsListItem(@NonNull String id, @NonNull String contactName, @NonNull String status) {
        this.id = id;
        this.contactName = contactName;
        this.status = status;
    }

    @Nullable
    String getContactName() {
        return contactName;
    }

    @NonNull
    String getId() {
        return id;
    }

    @NonNull
    public String getStatus() {
        return status;
    }
}
