package piuk.blockchain.android.ui.contacts.detail;

import android.support.v4.content.ContextCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import info.blockchain.wallet.contacts.data.FacilitatedTransaction;

import java.util.ArrayList;
import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

import static piuk.blockchain.android.ui.balance.BalanceFragment.SHOW_BTC;
import static piuk.blockchain.android.ui.send.SendViewModel.SHOW_FIAT;

class ContactTransactionAdapter extends RecyclerView.Adapter<ContactTransactionAdapter.TransactionViewHolder> {


    private List<FacilitatedTransaction> facilitatedTransactions;
    private final StringUtils stringUtils;
    private TransactionClickListener listener;
    private final MonetaryUtil monetaryUtil;
    private PrefsUtil prefsUtil;
    private double lastPrice;
    private boolean isBtc;
    private final String fiatString;

    ContactTransactionAdapter(ArrayList<FacilitatedTransaction> facilitatedTransactions,
                              StringUtils stringUtils,
                              PrefsUtil prefsUtil,
                              double lastPrice) {

        this.facilitatedTransactions = facilitatedTransactions;
        this.stringUtils = stringUtils;
        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        this.prefsUtil = prefsUtil;
        this.lastPrice = lastPrice;
        int balanceDisplayState = prefsUtil.getValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, SHOW_BTC);
        isBtc = balanceDisplayState != SHOW_FIAT;
        fiatString = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
    }

    @Override
    public TransactionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_transactions, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TransactionViewHolder holder, int position) {
        FacilitatedTransaction transaction = facilitatedTransactions.get(position);

        // Click listener
        holder.itemView.setOnClickListener(view -> {
            if (listener != null) listener.onClick(transaction.getId());
        });

        holder.indicator.setVisibility(View.GONE);
        holder.title.setTextColor(ContextCompat.getColor(holder.title.getContext(), R.color.black));

        if (transaction.getState() != null
                && transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS)) {
            holder.subtitle.setText(stringUtils.getString(R.string.contacts_waiting_for_address_title));

            if (transaction.getRole() != null
                    && (transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_RECEIVER)
                    || transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER))) {
                holder.indicator.setVisibility(View.VISIBLE);
            }

        } else if (transaction.getState() != null
                && transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)) {
            holder.subtitle.setText(stringUtils.getString(R.string.contacts_waiting_for_payment_title));

            if (transaction.getRole() != null
                    && (transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_RECEIVER)
                    || transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER))) {
                holder.indicator.setVisibility(View.VISIBLE);
            }

        } else if (transaction.getState() != null
                && transaction.getState().equals(FacilitatedTransaction.STATE_PAYMENT_BROADCASTED)) {
            if (transaction.getRole() != null
                    && (transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_RECEIVER)
                    || transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER))) {

                holder.subtitle.setText(stringUtils.getString(R.string.SENT));
                holder.subtitle.setTextColor(ContextCompat.getColor(
                        holder.title.getContext(),
                        R.color.blockchain_send_red));
            } else {
                holder.subtitle.setText(stringUtils.getString(R.string.RECEIVED));
                holder.subtitle.setTextColor(ContextCompat.getColor(
                        holder.title.getContext(),
                        R.color.blockchain_receive_green));
            }
        }

        double btcBalance = transaction.getIntended_amount() / 1e8;
        double fiatBalance = lastPrice * btcBalance;

        Spannable spannable;
        if (isBtc) {
            spannable = Spannable.Factory.getInstance().newSpannable(
                    monetaryUtil.getDisplayAmountWithFormatting(Math.abs(transaction.getIntended_amount())) + " " + getDisplayUnits());
            spannable.setSpan(
                    new RelativeSizeSpan(0.67f), spannable.length() - getDisplayUnits().length(), spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            spannable = Spannable.Factory.getInstance().newSpannable(
                    monetaryUtil.getFiatFormat(fiatString).format(Math.abs(fiatBalance)) + " " + fiatString);
            spannable.setSpan(
                    new RelativeSizeSpan(0.67f), spannable.length() - 3, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        holder.title.setText(spannable);
    }

    void setClickListener(TransactionClickListener listener) {
        this.listener = listener;
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

    private String getDisplayUnits() {
        return (String) monetaryUtil.getBTCUnits()[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {

        ImageView indicator;
        TextView title;
        TextView subtitle;

        TransactionViewHolder(View itemView) {
            super(itemView);
            indicator = (ImageView) itemView.findViewById(R.id.imageview_indicator);
            title = (TextView) itemView.findViewById(R.id.transaction_title);
            subtitle = (TextView) itemView.findViewById(R.id.transaction_subtitle);
        }
    }

    interface TransactionClickListener {

        void onClick(String id);

    }
}
