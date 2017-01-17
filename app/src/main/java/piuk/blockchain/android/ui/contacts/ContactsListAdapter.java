package piuk.blockchain.android.ui.contacts;

import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.util.StringUtils;
import piuk.blockchain.android.util.TimeUtil;

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

        holder.name.setText(listItem.getContactName());
        holder.itemView.setOnClickListener(view -> {
            if (contactsClickListener != null) contactsClickListener.onClick(listItem.getId());
        });

        if (listItem.getStatus().equals(ContactsListItem.Status.PENDING)) {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.indicator.setVisibility(View.GONE);
        } else {
            holder.progressBar.setVisibility(View.GONE);
            // TODO: 16/01/2017 Set this sensibly
            holder.indicator.setVisibility(View.VISIBLE);
        }

        switch (listItem.getStatus()) {
            case ContactsListItem.Status.PENDING:
                holder.status.setText(stringUtils.getString(R.string.contacts_request_sent));
                break;
            case ContactsListItem.Status.TRUSTED:
                if (TimeUtil.getIfTimeElapsed(listItem.getInviteTime(), TimeUtil.HOURS_24)) {
                    holder.status.setText(stringUtils.getString(R.string.contacts_request_trusted));
                } else {
                    holder.status.setText(stringUtils.getString(R.string.contacts_request_just_added));
                }
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
        ProgressBar progressBar;

        ContactsViewHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.contact_name);
            status = (TextView) itemView.findViewById(R.id.contact_status);
            indicator = (ImageView) itemView.findViewById(R.id.imageview_indicator);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progress_bar);
        }
    }

    interface ContactsClickListener {

        void onClick(String id);

    }
}
