package piuk.blockchain.android.ui.account;

import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import piuk.blockchain.android.R;

class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.ViewHolder> {

    private static final int TYPE_IMPORTED_HEADER = -1;
    private static final int TYPE_CREATE_NEW_WALLET_BUTTON = -2;
    private static final int TYPE_IMPORT_ADDRESS_BUTTON = -3;
    private ArrayList<AccountItem> items;
    private boolean isUpgraded;
    private AccountHeadersListener listener;

    AccountAdapter(ArrayList<AccountItem> myAccountItems, boolean isUpgraded) {
        this.items = myAccountItems;
        this.isUpgraded = isUpgraded;
    }

    @Override
    public AccountAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_accounts_row, parent, false);

        if (viewType == TYPE_IMPORTED_HEADER) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_accounts_row_header, parent, false);
            TextView header = (TextView) v.findViewById(R.id.my_account_row_header);
            header.setText(AccountActivity.IMPORTED_HEADER);

        } else if (viewType == TYPE_CREATE_NEW_WALLET_BUTTON || viewType == TYPE_IMPORT_ADDRESS_BUTTON) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_accounts_row_buttons, parent, false);
        }

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        if (holder.getItemViewType() == TYPE_IMPORTED_HEADER)
            return;

        CardView cardView = (CardView) holder.itemView.findViewById(R.id.card_view);

        if (holder.getItemViewType() == TYPE_CREATE_NEW_WALLET_BUTTON) {
            TextView description = (TextView) holder.itemView.findViewById(R.id.description);
            if (isUpgraded) {
                description.setText(R.string.create_new);
            } else {
                description.setText(R.string.create_new_address);
            }

            cardView.setOnClickListener(v -> {
                if (listener != null) listener.onCreateNewClicked();
            });
            return;
        }

        if (holder.getItemViewType() == TYPE_IMPORT_ADDRESS_BUTTON) {
            TextView description = (TextView) holder.itemView.findViewById(R.id.description);
            description.setText(R.string.import_address);

            cardView.setOnClickListener(v -> {
                if (listener != null) listener.onImportAddressClicked();
            });
            return;
        }

        cardView.setOnClickListener(v -> {
            if (listener != null) listener.onCardClicked(items.get(position).getCorrectPosition());
        });

        TextView title = (TextView) holder.itemView.findViewById(R.id.my_account_row_label);
        TextView address = (TextView) holder.itemView.findViewById(R.id.my_account_row_address);
        ImageView icon = (ImageView) holder.itemView.findViewById(R.id.my_account_row_icon);
        TextView amount = (TextView) holder.itemView.findViewById(R.id.my_account_row_amount);
        TextView tag = (TextView) holder.itemView.findViewById(R.id.my_account_row_tag);

        title.setText(items.get(position).getLabel());

        if (items.get(position).getAddress() != null) {
            address.setText(items.get(position).getAddress());
        } else {
            address.setVisibility(View.GONE);
        }

        if (items.get(position).isArchived()) {
            amount.setText(R.string.archived_label);
            amount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blockchain_transfer_blue));
        } else {
            amount.setText(items.get(position).getAmount());
            amount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blockchain_receive_green));
        }

        if (items.get(position).isWatchOnly()) {
            tag.setText(holder.itemView.getContext().getString(R.string.watch_only));
            tag.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blockchain_send_red));
        }

        if (items.get(position).isDefault()) {
            tag.setText(holder.itemView.getContext().getString(R.string.default_label));
            tag.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blockchain_grey));
        }

        if (!items.get(position).isWatchOnly() && !items.get(position).isDefault()) {
            tag.setVisibility(View.INVISIBLE);
        }

        Drawable drawable = items.get(position).getIcon();
        if (drawable != null)
            icon.setImageDrawable(drawable);
    }

    @Override
    public int getItemCount() {
        return !items.isEmpty() ? items.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        String title = items.get(position).getLabel();
        if (title.equals(AccountActivity.IMPORTED_HEADER)) {
            return TYPE_IMPORTED_HEADER;
        } else if (title.equals(AccountActivity.IMPORT_ADDRESS)) {
            return TYPE_IMPORT_ADDRESS_BUTTON;
        } else if (title.equals(AccountActivity.CREATE_NEW)) {
            return TYPE_CREATE_NEW_WALLET_BUTTON;
        }

        return position;
    }

    void setAccountHeaderListener(AccountHeadersListener accountHeadersListener) {
        listener = accountHeadersListener;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ViewHolder(View view) {
            super(view);
        }
    }

    interface AccountHeadersListener {

        void onCreateNewClicked();

        void onImportAddressClicked();

        void onCardClicked(int correctedPosition);
    }
}