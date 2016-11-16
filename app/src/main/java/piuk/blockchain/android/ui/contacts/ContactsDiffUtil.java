package piuk.blockchain.android.ui.contacts;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import java.util.List;

public class ContactsDiffUtil extends DiffUtil.Callback {

    private List<ContactsListItem> oldContacts;
    private List<ContactsListItem> newContacts;

    ContactsDiffUtil(List<ContactsListItem> oldContacts, List<ContactsListItem> newContacts) {
        this.oldContacts = oldContacts;
        this.newContacts = newContacts;
    }

    @Override
    public int getOldListSize() {
        return oldContacts != null ? oldContacts.size() : 0;
    }

    @Override
    public int getNewListSize() {
        return newContacts != null ? newContacts.size() : 0;
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldContacts.get(oldItemPosition).getMdid().equals(
                newContacts.get(newItemPosition).getMdid());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        ContactsListItem oldContact = oldContacts.get(oldItemPosition);
        ContactsListItem newContact = newContacts.get(newItemPosition);

        // Temporary, Nonnull annotations aren't applicable right now in testing
        //noinspection ConstantConditions
        return (oldContact.getMdid() != null ? oldContact.getMdid() : "").equals(newContact.getMdid() != null ? newContact.getMdid() : "")
                && (oldContact.getContactName() != null ? oldContact.getContactName() : "").equals(newContact.getContactName() != null ? newContact.getContactName() : "")
                && (oldContact.getStatus() != null ? oldContact.getStatus() : "").equals(newContact.getStatus() != null ? newContact.getStatus() : "");
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}
