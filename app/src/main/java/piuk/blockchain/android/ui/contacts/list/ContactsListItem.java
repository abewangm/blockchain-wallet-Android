package piuk.blockchain.android.ui.contacts.list;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ContactsListItem implements Comparable<ContactsListItem>{

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
    String getStatus() {
        return status;
    }

    long getInviteTime() {
        return inviteTime;
    }

    boolean requiresResponse() {
        return requiresResponse;
    }

    @Override
    public int compareTo(@NonNull ContactsListItem contactsListItem) {
        if(this.contactName != null && contactsListItem.contactName != null){
            return this.contactName.compareToIgnoreCase(contactsListItem.contactName);
        }
        return 0;
    }

    @SuppressWarnings("WeakerAccess")
    public static class Status {
        public static final String PENDING = "Pending";
        public static final String TRUSTED = "Trusted";
    }
}
