package piuk.blockchain.android.ui.chooser;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.LegacyAddress;

import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.account.ItemAccount;

class AccountChooserAdapter extends RecyclerView.Adapter {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_CONTACT = 1;
    private static final int VIEW_TYPE_ACCOUNT = 2;
    private static final int VIEW_TYPE_LEGACY = 3;

    private List<ItemAccount> items;
    private AccountClickListener clickListener;

    AccountChooserAdapter(List<ItemAccount> items, AccountClickListener clickListener) {
        this.items = items;
        this.clickListener = clickListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                View header = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_accounts_row_header, parent, false);
                return new HeaderViewHolder(header);
            case VIEW_TYPE_CONTACT:
                View contact = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
                return new ContactViewHolder(contact);
            case VIEW_TYPE_ACCOUNT:
            case VIEW_TYPE_LEGACY:
                View account = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_accounts_row, parent, false);
                return new AccountViewHolder(account);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ItemAccount itemAccount = items.get(position);
        switch (getItemViewType(position)) {
            case VIEW_TYPE_HEADER:
                HeaderViewHolder headerViewHolder = ((HeaderViewHolder) holder);
                headerViewHolder.header.setText(itemAccount.label);
                holder.itemView.setOnClickListener(null);
                break;
            case VIEW_TYPE_CONTACT:
                ContactViewHolder contactViewHolder = (ContactViewHolder) holder;
                contactViewHolder.name.setText(((Contact) itemAccount.accountObject).getName());
                holder.itemView.setOnClickListener(v -> clickListener.onClick(itemAccount.accountObject));
                break;
            case VIEW_TYPE_ACCOUNT:
            case VIEW_TYPE_LEGACY:
                AccountViewHolder accountViewHolder = ((AccountViewHolder) holder);
                accountViewHolder.label.setText(itemAccount.label);
                accountViewHolder.balance.setText(itemAccount.displayBalance);

                if (itemAccount.accountObject instanceof LegacyAddress) {
                    accountViewHolder.address.setText(((LegacyAddress) itemAccount.accountObject).getAddress());
                    if (((LegacyAddress) itemAccount.accountObject).isWatchOnly()) {
                        accountViewHolder.tag.setText(holder.itemView.getContext().getString(R.string.watch_only));
                        accountViewHolder.tag.setVisibility(View.VISIBLE);
                    } else {
                        accountViewHolder.tag.setVisibility(View.GONE);
                    }
                    accountViewHolder.address.setVisibility(View.VISIBLE);
                } else {
                    accountViewHolder.address.setText(null);
                    accountViewHolder.tag.setVisibility(View.GONE);
                    accountViewHolder.address.setVisibility(View.GONE);
                }
                holder.itemView.setOnClickListener(v -> clickListener.onClick(itemAccount.accountObject));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        ItemAccount object = items.get(position);

        if (object.accountObject instanceof Contact) {
            return VIEW_TYPE_CONTACT;
        } else if (object.accountObject instanceof Account) {
            return VIEW_TYPE_ACCOUNT;
        } else if (object.accountObject instanceof LegacyAddress) {
            return VIEW_TYPE_LEGACY;
        } else {
            return VIEW_TYPE_HEADER;
        }
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {

        TextView header;

        HeaderViewHolder(View itemView) {
            super(itemView);
            header = (TextView) itemView.findViewById(R.id.header_name);
            itemView.findViewById(R.id.imageview_plus).setVisibility(View.GONE);
        }
    }

    private static class ContactViewHolder extends RecyclerView.ViewHolder {

        TextView name;

        ContactViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.contact_name);
            itemView.findViewById(R.id.contact_status).setVisibility(View.GONE);
        }
    }

    private static class AccountViewHolder extends RecyclerView.ViewHolder {

        TextView label;
        TextView tag;
        TextView balance;
        TextView address;

        AccountViewHolder(View itemView) {
            super(itemView);
            label = (TextView) itemView.findViewById(R.id.my_account_row_label);
            tag = (TextView) itemView.findViewById(R.id.my_account_row_tag);
            balance = (TextView) itemView.findViewById(R.id.my_account_row_amount);
            address = (TextView) itemView.findViewById(R.id.my_account_row_address);
        }
    }

    interface AccountClickListener {

        void onClick(Object object);

    }
}
