package piuk.blockchain.android.ui.chooser;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.ethereum.EthereumAccount;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;

import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.account.ItemAccount;

class AccountChooserAdapter extends RecyclerView.Adapter {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_CONTACT = 1;
    private static final int VIEW_TYPE_ACCOUNT = 2;
    private static final int VIEW_TYPE_LEGACY = 3;
    private static final int VIEW_TYPE_ETHEREUM = 4;

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
            case VIEW_TYPE_ETHEREUM:
                View ethereum = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_accounts_row, parent, false);
                return new EthereumViewHolder(ethereum);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ItemAccount itemAccount = items.get(position);
        switch (getItemViewType(position)) {
            case VIEW_TYPE_HEADER:
                HeaderViewHolder headerViewHolder = ((HeaderViewHolder) holder);
                headerViewHolder.header.setText(itemAccount.getLabel());
                holder.itemView.setOnClickListener(null);
                break;
            case VIEW_TYPE_CONTACT:
                ContactViewHolder contactViewHolder = (ContactViewHolder) holder;
                contactViewHolder.name.setText(((Contact) itemAccount.getAccountObject()).getName());
                holder.itemView.setOnClickListener(v -> clickListener.onClick(itemAccount.getAccountObject()));
                break;
            case VIEW_TYPE_ACCOUNT:
            case VIEW_TYPE_LEGACY:
                AccountViewHolder accountViewHolder = ((AccountViewHolder) holder);
                accountViewHolder.label.setText(itemAccount.getLabel());
                accountViewHolder.balance.setText(itemAccount.getDisplayBalance());

                if (itemAccount.getAccountObject() instanceof LegacyAddress) {
                    accountViewHolder.address.setText(((LegacyAddress) itemAccount.getAccountObject()).getAddress());
                    if (((LegacyAddress) itemAccount.getAccountObject()).isWatchOnly()) {
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
                holder.itemView.setOnClickListener(v -> clickListener.onClick(itemAccount.getAccountObject()));
                break;
            case VIEW_TYPE_ETHEREUM:
                EthereumViewHolder ethereumViewHolder = ((EthereumViewHolder) holder);
                ethereumViewHolder.label.setText(itemAccount.getLabel());
                ethereumViewHolder.balance.setText(itemAccount.getDisplayBalance());
                holder.itemView.setOnClickListener(v -> clickListener.onClick(itemAccount.getAccountObject()));
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

        if (object.getAccountObject() instanceof Contact) {
            return VIEW_TYPE_CONTACT;
        } else if (object.getAccountObject() instanceof Account) {
            return VIEW_TYPE_ACCOUNT;
        } else if (object.getAccountObject() instanceof LegacyAddress) {
            return VIEW_TYPE_LEGACY;
        } else if (object.getAccountObject() instanceof EthereumAccount) {
            return VIEW_TYPE_ETHEREUM;
        } else {
            return VIEW_TYPE_HEADER;
        }
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {

        TextView header;

        HeaderViewHolder(View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header_name);
            itemView.findViewById(R.id.imageview_plus).setVisibility(View.GONE);
        }
    }

    private static class ContactViewHolder extends RecyclerView.ViewHolder {

        TextView name;

        ContactViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.contactName);
            itemView.findViewById(R.id.contactStatus).setVisibility(View.GONE);
            itemView.findViewById(R.id.imageviewIndicator).setVisibility(View.GONE);
            itemView.findViewById(R.id.imageViewMore).setVisibility(View.GONE);
        }
    }

    private static class AccountViewHolder extends RecyclerView.ViewHolder {

        TextView label;
        TextView tag;
        TextView balance;
        TextView address;

        AccountViewHolder(View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.my_account_row_label);
            tag = itemView.findViewById(R.id.my_account_row_tag);
            balance = itemView.findViewById(R.id.my_account_row_amount);
            address = itemView.findViewById(R.id.my_account_row_address);
        }
    }

    private static class EthereumViewHolder extends RecyclerView.ViewHolder {

        TextView label;
        TextView tag;
        TextView balance;
        TextView address;

        EthereumViewHolder(View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.my_account_row_label);
            tag = itemView.findViewById(R.id.my_account_row_tag);
            balance = itemView.findViewById(R.id.my_account_row_amount);
            address = itemView.findViewById(R.id.my_account_row_address);

            tag.setVisibility(View.GONE);
            address.setVisibility(View.GONE);
        }
    }

    interface AccountClickListener {

        void onClick(Object object);

    }
}
