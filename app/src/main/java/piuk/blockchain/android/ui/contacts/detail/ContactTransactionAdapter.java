package piuk.blockchain.android.ui.contacts.detail;

import android.support.v4.content.ContextCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import info.blockchain.wallet.contacts.data.FacilitatedTransaction;

import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.SpanFormatter;
import piuk.blockchain.android.util.StringUtils;

import static piuk.blockchain.android.ui.balance.BalanceFragment.SHOW_BTC;
import static piuk.blockchain.android.ui.send.SendViewModel.SHOW_FIAT;

class ContactTransactionAdapter extends RecyclerView.Adapter<ContactTransactionAdapter.TransactionViewHolder> {

    private List<FacilitatedTransaction> facilitatedTransactions;
    private String contactName;
    private final StringUtils stringUtils;
    private TransactionClickListener listener;
    private final MonetaryUtil monetaryUtil;
    private PrefsUtil prefsUtil;
    private double lastPrice;
    private boolean isBtc;

    ContactTransactionAdapter(List<FacilitatedTransaction> facilitatedTransactions,
                              String contactName,
                              StringUtils stringUtils,
                              PrefsUtil prefsUtil,
                              double lastPrice) {

        this.facilitatedTransactions = facilitatedTransactions;
        this.contactName = contactName;
        this.stringUtils = stringUtils;
        this.prefsUtil = prefsUtil;
        this.lastPrice = lastPrice;
        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        int balanceDisplayState = prefsUtil.getValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, SHOW_BTC);
        isBtc = balanceDisplayState != SHOW_FIAT;
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

        holder.itemView.setOnLongClickListener(view -> {
            if (listener != null ) listener.onLongClick(transaction.getId());
            return true;
        });

        holder.indicator.setVisibility(View.GONE);
        holder.title.setTextColor(ContextCompat.getColor(holder.title.getContext(), R.color.black));

        double btcBalance = transaction.getIntendedAmount() / 1e8;
        double fiatBalance = lastPrice * btcBalance;

        String fiatString = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        Spannable amountSpannable = getDisplaySpannable(transaction.getIntendedAmount(), fiatBalance, fiatString);

        if (transaction.getState() != null
                && transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS)) {
            if (transaction.getRole() != null) {
                if (transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_RECEIVER)) {
                    Spanned display = SpanFormatter.format(
                            stringUtils.getString(R.string.contacts_sending_to_contact_waiting),
                            amountSpannable,
                            contactName);
                    holder.title.setText(display);
                    holder.indicator.setVisibility(View.VISIBLE);

                } else if (transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER)) {
                    Spanned display = SpanFormatter.format(
                            stringUtils.getString(R.string.contacts_receiving_from_contact_waiting_to_accept),
                            amountSpannable,
                            contactName);
                    holder.title.setText(display);
                    holder.indicator.setVisibility(View.VISIBLE);

                } else if (transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_INITIATOR)) {
                    Spanned display = SpanFormatter.format(
                            stringUtils.getString(R.string.contacts_requesting_from_contact_waiting_to_accept),
                            amountSpannable,
                            contactName);
                    holder.title.setText(display);

                } else if (transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_INITIATOR)) {
                    Spanned display = SpanFormatter.format(
                            stringUtils.getString(R.string.contacts_sending_to_contact_waiting),
                            amountSpannable,
                            contactName);
                    holder.title.setText(display);
                }
            }

        } else if (transaction.getState() != null
                && transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)) {
            if (transaction.getRole() != null) {
                if (transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_RECEIVER)) {
                    Spanned display = SpanFormatter.format(
                            stringUtils.getString(R.string.contacts_sending_to_contact_ready_to_send),
                            amountSpannable,
                            contactName);
                    holder.title.setText(display);
                    holder.indicator.setVisibility(View.VISIBLE);


                } else if (transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER)) {
                    Spanned display = SpanFormatter.format(
                            stringUtils.getString(R.string.contacts_requesting_from_contact_waiting_for_payment),
                            amountSpannable,
                            contactName);
                    holder.title.setText(display);
                    holder.indicator.setVisibility(View.VISIBLE);

                } else if (transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_INITIATOR)) {
                    Spanned display = SpanFormatter.format(
                            stringUtils.getString(R.string.contacts_requesting_from_contact_waiting_for_payment),
                            amountSpannable,
                            contactName);
                    holder.title.setText(display);

                } else if (transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_INITIATOR)) {
                    Spanned display = SpanFormatter.format(
                            stringUtils.getString(R.string.contacts_sending_to_contact_ready_to_send),
                            amountSpannable,
                            contactName);
                    holder.title.setText(display);
                }
            }
        } else if (transaction.getState() != null
                && transaction.getState().equals(FacilitatedTransaction.STATE_PAYMENT_BROADCASTED)) {
            if (transaction.getRole() != null
                    && (transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_RECEIVER)
                    || transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER))) {

                Spanned display = SpanFormatter.format(
                        stringUtils.getString(R.string.contacts_send_complete),
                        amountSpannable,
                        contactName);

                holder.title.setText(display);
                holder.title.setTextColor(ContextCompat.getColor(
                        holder.title.getContext(),
                        R.color.product_red_sent));
            } else {
                Spanned display = SpanFormatter.format(
                        stringUtils.getString(R.string.contacts_receive_complete),
                        amountSpannable,
                        contactName);

                holder.title.setText(display);
                holder.title.setTextColor(ContextCompat.getColor(
                        holder.title.getContext(),
                        R.color.product_green_received));
            }

            // Can't delete completed transactions
            holder.itemView.setOnLongClickListener(null);
        }

        holder.subtitle.setText(transaction.getNote());
    }

    void setClickListener(TransactionClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemCount() {
        return facilitatedTransactions != null ? facilitatedTransactions.size() : 0;
    }

    void onTransactionsUpdated(List<FacilitatedTransaction> transactions) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new ContactTransactionDiffUtil(facilitatedTransactions, transactions));
        facilitatedTransactions = transactions;
        diffResult.dispatchUpdatesTo(this);
    }

    void onNameUpdated(String name) {
        contactName = name;
        notifyDataSetChanged();
    }

    private String getDisplayUnits() {
        return (String) monetaryUtil.getBTCUnits()[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];
    }

    private Spannable getDisplaySpannable(double btcAmount, double fiatAmount, String fiatString) {
        Spannable spannable;
        if (isBtc) {
            spannable = Spannable.Factory.getInstance().newSpannable(
                    monetaryUtil.getDisplayAmountWithFormatting(Math.abs(btcAmount)) + " " + getDisplayUnits());
            spannable.setSpan(
                    new RelativeSizeSpan(0.67f),
                    spannable.length() - getDisplayUnits().length(),
                    spannable.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            spannable = Spannable.Factory.getInstance().newSpannable(
                    monetaryUtil.getFiatFormat(fiatString).format(Math.abs(fiatAmount)) + " " + fiatString);
            spannable.setSpan(
                    new RelativeSizeSpan(0.67f),
                    spannable.length() - 3,
                    spannable.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
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

        void onClick(String fctxId);

        void onLongClick(String fctxId);

    }
}
