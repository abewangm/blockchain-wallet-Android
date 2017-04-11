package piuk.blockchain.android.ui.account;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import piuk.blockchain.android.R;

import static piuk.blockchain.android.ui.account.AccountItem.TYPE_ACCOUNT;
import static piuk.blockchain.android.ui.account.AccountItem.TYPE_CREATE_NEW_WALLET_BUTTON;
import static piuk.blockchain.android.ui.account.AccountItem.TYPE_IMPORT_ADDRESS_BUTTON;

class AccountAdapter extends RecyclerView.Adapter {

    private ArrayList<AccountItem> items;
    private AccountHeadersListener listener;

    AccountAdapter(ArrayList<AccountItem> myAccountItems) {
        items = myAccountItems;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_CREATE_NEW_WALLET_BUTTON || viewType == TYPE_IMPORT_ADDRESS_BUTTON) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_accounts_row_header, parent, false);
            return new HeaderViewHolder(v);
        } else if (viewType == TYPE_ACCOUNT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_accounts_row, parent, false);
            return new AccountViewHolder(v);
        } else {
            throw new IllegalArgumentException("Unknown ViewType " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_CREATE_NEW_WALLET_BUTTON) {
            // Create new wallet button
            HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
            headerViewHolder.title.setText(R.string.wallets);

            headerViewHolder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCreateNewClicked();
            });
        } else if (getItemViewType(position) == TYPE_IMPORT_ADDRESS_BUTTON) {
            // Import address button
            HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
            headerViewHolder.title.setText(R.string.imported_addresses);

            headerViewHolder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onImportAddressClicked();
            });
        } else {
            AccountViewHolder accountViewHolder = (AccountViewHolder) holder;

            AccountItem accountItem = items.get(position);

            // Normal account view
            accountViewHolder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAccountClicked(accountItem.getCorrectPosition());
            });

            accountViewHolder.title.setText(accountItem.getLabel());

            if (!accountItem.getAddress().isEmpty()) {
                accountViewHolder.address.setVisibility(View.VISIBLE);
                accountViewHolder.address.setText(accountItem.getAddress());
            } else {
                accountViewHolder.address.setVisibility(View.GONE);
            }

            if (accountItem.isArchived()) {
                accountViewHolder.amount.setText(R.string.archived_label);
                accountViewHolder.amount.setTextColor(ContextCompat.getColor(accountViewHolder.itemView.getContext(), R.color.product_gray_transferred));
            } else {
                accountViewHolder.amount.setText(accountItem.getAmount());
                accountViewHolder.amount.setTextColor(ContextCompat.getColor(accountViewHolder.itemView.getContext(), R.color.product_green_medium));
            }

            if (accountItem.isWatchOnly()) {
                accountViewHolder.tag.setText(accountViewHolder.itemView.getContext().getString(R.string.watch_only));
                accountViewHolder.tag.setTextColor(ContextCompat.getColor(accountViewHolder.itemView.getContext(), R.color.product_red_medium));
            }

            if (accountItem.isDefault()) {
                accountViewHolder.tag.setText(accountViewHolder.itemView.getContext().getString(R.string.default_label));
                accountViewHolder.tag.setTextColor(ContextCompat.getColor(accountViewHolder.itemView.getContext(), R.color.product_gray_transferred));
            }

            if (!accountItem.isWatchOnly() && !accountItem.isDefault()) {
                accountViewHolder.tag.setVisibility(View.GONE);
            } else {
                accountViewHolder.tag.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return !items.isEmpty() ? items.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    void setAccountHeaderListener(AccountHeadersListener accountHeadersListener) {
        listener = accountHeadersListener;
    }

    private static class AccountViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        TextView address;
        TextView amount;
        TextView tag;

        AccountViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.my_account_row_label);
            address = (TextView) view.findViewById(R.id.my_account_row_address);
            amount = (TextView) view.findViewById(R.id.my_account_row_amount);
            tag = (TextView) view.findViewById(R.id.my_account_row_tag);
        }
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {

        TextView title;

        HeaderViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.header_name);
        }
    }

    interface AccountHeadersListener {

        void onCreateNewClicked();

        void onImportAddressClicked();

        void onAccountClicked(int correctedPosition);
    }
}