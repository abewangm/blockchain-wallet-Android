package piuk.blockchain.android.ui.contacts.list;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class ContactsListItem {

    private String id;
    private String contactName;
    private String status;
    private long inviteTime;
    private boolean requiresResponse;

    ContactsListItem(@NonNull String id,
                     @NonNull String contactName,
                     @NonNull String status,
                     long inviteTime,
                     boolean requiresResponse) {

        this.id = id;
        this.contactName = contactName;
        this.status = status;
        this.inviteTime = inviteTime;
        this.requiresResponse = requiresResponse;
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

    public boolean requiresResponse() {
        return requiresResponse;
    }

    @SuppressWarnings("WeakerAccess")
    public static class Status {

        public static final String PENDING = "Pending";
        public static final String ACCEPTED = "Accepted";
        public static final String TRUSTED = "Trusted";

    }
}
