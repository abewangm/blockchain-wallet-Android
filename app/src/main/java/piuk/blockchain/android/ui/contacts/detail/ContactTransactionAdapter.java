package piuk.blockchain.android.ui.contacts.detail;

import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import info.blockchain.wallet.contacts.data.FacilitatedTransaction;

import java.util.ArrayList;
import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.util.StringUtils;

class ContactTransactionAdapter extends RecyclerView.Adapter<ContactTransactionAdapter.TransactionViewHolder> {


    private List<FacilitatedTransaction> facilitatedTransactions;
    private final StringUtils stringUtils;

    ContactTransactionAdapter(ArrayList<FacilitatedTransaction> facilitatedTransactions, StringUtils stringUtils) {
        this.facilitatedTransactions = facilitatedTransactions;
        this.stringUtils = stringUtils;
    }

    @Override
    public TransactionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_transactions, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TransactionViewHolder holder, int position) {
        FacilitatedTransaction transaction = facilitatedTransactions.get(position);

        holder.title.setText(String.valueOf(transaction.getIntended_amount()));
        holder.subtitle.setText(transaction.getState());

    }

    @Override
    public int getItemCount() {
        return facilitatedTransactions != null ? facilitatedTransactions.size() : 0;
    }

    void onTransactionsUpdated(List<FacilitatedTransaction> transactions) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ContactTransactionDiffUtil(facilitatedTransactions, transactions));
        facilitatedTransactions = transactions;
        diffResult.dispatchUpdatesTo(this);
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {

        ImageView indicator;
        TextView title;
        TextView subtitle;

        public TransactionViewHolder(View itemView) {
            super(itemView);
            indicator = (ImageView) itemView.findViewById(R.id.imageview_indicator);
            title = (TextView) itemView.findViewById(R.id.transaction_title);
            subtitle = (TextView) itemView.findViewById(R.id.transaction_subtitle);
        }
    }
}
