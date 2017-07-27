package piuk.blockchain.android.ui.contacts.list;

import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.util.StringUtils;

class ContactsListAdapter extends RecyclerView.Adapter<ContactsListAdapter.ContactsViewHolder> {

    private List<ContactsListItem> contacts;
    private final StringUtils stringUtils;
    private ContactsClickListener contactsClickListener;

    ContactsListAdapter(List<ContactsListItem> contacts, StringUtils stringUtils) {
        this.contacts = contacts;
        this.stringUtils = stringUtils;
    }

    void setContactsClickListener(ContactsClickListener contactsClickListener) {
        this.contactsClickListener = contactsClickListener;
    }

    @Override
    public ContactsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ContactsViewHolder holder, int position) {
        ContactsListItem listItem = contacts.get(position);

        // Click listener
        holder.itemView.setOnClickListener(view -> {
            if (contactsClickListener != null) contactsClickListener.onContactClick(listItem.getId());
        });
        holder.more.setOnClickListener(view -> {
            if (contactsClickListener != null) contactsClickListener.onMoreClick(listItem.getId());
        });

        // Name
        holder.name.setText(listItem.getContactName());

        // Progress bar for pending contacts
        if (listItem.getStatus().equals(ContactsListItem.Status.PENDING)) {
            holder.more.setVisibility(View.VISIBLE);
            holder.name.setAlpha(0.5f);
        } else {
            holder.more.setVisibility(View.GONE);
            holder.name.setAlpha(1.0f);
        }

        // Notification indicator
        holder.indicator.setVisibility(listItem.requiresResponse() ? View.VISIBLE : View.GONE);

        // Status field
        switch (listItem.getStatus()) {
            case ContactsListItem.Status.PENDING:
                holder.status.setText("("+stringUtils.getString(R.string.pending)+")");
                break;
            case ContactsListItem.Status.TRUSTED:
                holder.status.setText("");
                break;
            default:
                holder.status.setText("");
                break;
        }
    }

    @Override
    public int getItemCount() {
        return contacts != null ? contacts.size() : 0;
    }

    void onContactsUpdated(List<ContactsListItem> contacts) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ContactsDiffUtil(this.contacts, contacts));
        this.contacts = contacts;
        diffResult.dispatchUpdatesTo(this);
    }

    static class ContactsViewHolder extends RecyclerView.ViewHolder {

        TextView name;
        TextView status;
        ImageView indicator;
        ImageView more;

        ContactsViewHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.contactName);
            status = itemView.findViewById(R.id.contactStatus);
            indicator = itemView.findViewById(R.id.imageviewIndicator);
            more = itemView.findViewById(R.id.imageViewMore);
        }
    }

    interface ContactsClickListener {

        void onContactClick(String id);

        void onMoreClick(String id);
    }
}
