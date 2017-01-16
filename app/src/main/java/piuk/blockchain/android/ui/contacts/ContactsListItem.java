package piuk.blockchain.android.ui.contacts;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class ContactsListItem {

    private String id;
    private String contactName;
    private String status;
    private long inviteTime;

    ContactsListItem(@NonNull String id, @NonNull String contactName, @NonNull String status, long inviteTime) {
        this.id = id;
        this.contactName = contactName;
        this.status = status;
        this.inviteTime = inviteTime;
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

    public long getInviteTime() {
        return inviteTime;
    }

    @SuppressWarnings("WeakerAccess")
    public static class Status {

        public static final String PENDING = "Pending";
        public static final String ACCEPTED = "Accepted";
        public static final String TRUSTED = "Trusted";

    }
}
