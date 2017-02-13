package piuk.blockchain.android.ui.contacts.list;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import java.util.List;

class ContactsDiffUtil extends DiffUtil.Callback {

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
        return oldContacts.get(oldItemPosition).getId().equals(
                newContacts.get(newItemPosition).getId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        ContactsListItem oldContact = oldContacts.get(oldItemPosition);
        ContactsListItem newContact = newContacts.get(newItemPosition);

        return oldContact.equals(newContact);
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}
