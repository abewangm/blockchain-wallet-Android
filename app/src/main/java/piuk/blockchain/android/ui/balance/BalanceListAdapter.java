package piuk.blockchain.android.ui.balance;

import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import info.blockchain.wallet.contacts.data.FacilitatedTransaction;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.transaction.Tx;

import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.util.DateUtil;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

class BalanceListAdapter extends RecyclerView.Adapter {

    private static final int ITEM_TYPE_HEADER = 0;
    private static final int ITEM_TYPE_FCTX = 1;
    private static final int ITEM_TYPE_TRANSACTION = 2;

    private List<Object> transactions;
    private PrefsUtil prefsUtil;
    private MonetaryUtil monetaryUtil;
    private StringUtils stringUtils;
    private DateUtil dateUtil;
    private double btcExchangeRate;
    private boolean isBtc;
    private TxListClickListener listClickListener;

    BalanceListAdapter(List<Object> transactions,
                       PrefsUtil prefsUtil,
                       MonetaryUtil monetaryUtil,
                       StringUtils stringUtils,
                       DateUtil dateUtil,
                       double btcExchangeRate,
                       boolean isBtc) {

        this.transactions = transactions;
        this.prefsUtil = prefsUtil;
        this.monetaryUtil = monetaryUtil;
        this.stringUtils = stringUtils;
        this.dateUtil = dateUtil;
        this.btcExchangeRate = btcExchangeRate;
        this.isBtc = isBtc;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case ITEM_TYPE_HEADER:
                View header = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_accounts_row_header, parent, false);
                return new HeaderViewHolder(header);
            case ITEM_TYPE_FCTX:
                View fctxView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_transactions, parent, false);
                return new FctxViewHolder(fctxView);
            default:
                View txView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_balance, parent, false);
                return new TxViewHolder(txView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case ITEM_TYPE_HEADER:
                bindHeaderView(holder, position);
                break;
            case ITEM_TYPE_FCTX:
                bindFctxView(holder, position);
                break;
            default:
                bindTxView(holder, position);
                break;
        }
    }

    private void bindHeaderView(RecyclerView.ViewHolder holder, int position) {
        HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
        final String header = (String) transactions.get(position);
        headerViewHolder.header.setText(header);
    }

    private void bindFctxView(RecyclerView.ViewHolder holder, int position) {
        FctxViewHolder fctxViewHolder = (FctxViewHolder) holder;
        final FacilitatedTransaction transaction = (FacilitatedTransaction) transactions.get(position);

        // Click listener
        holder.itemView.setOnClickListener(view -> {
            if (listClickListener != null) listClickListener.onFctxClicked(transaction.getId());
        });

        fctxViewHolder.indicator.setVisibility(View.GONE);
        fctxViewHolder.title.setTextColor(ContextCompat.getColor(fctxViewHolder.title.getContext(), R.color.black));

        if (transaction.getState() != null
                && transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS)) {
            fctxViewHolder.title.setText(stringUtils.getString(R.string.contacts_waiting_for_address_title));

            if (transaction.getRole() != null
                    && (transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_RECEIVER)
                    || transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER))) {
                fctxViewHolder.indicator.setVisibility(View.VISIBLE);
            }

        } else if (transaction.getState() != null
                && transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)) {
            fctxViewHolder.title.setText(stringUtils.getString(R.string.contacts_waiting_for_payment_title));

            if (transaction.getRole() != null
                    && (transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_RECEIVER)
                    || transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER))) {
                fctxViewHolder.indicator.setVisibility(View.VISIBLE);
            }

        } else if (transaction.getState() != null
                && transaction.getState().equals(FacilitatedTransaction.STATE_PAYMENT_BROADCASTED)) {
            if (transaction.getRole() != null
                    && (transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_RECEIVER)
                    || transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER))) {

                fctxViewHolder.title.setText(stringUtils.getString(R.string.SENT));
                fctxViewHolder.title.setTextColor(ContextCompat.getColor(
                        fctxViewHolder.title.getContext(),
                        R.color.blockchain_send_red));
            } else {
                fctxViewHolder.title.setText(stringUtils.getString(R.string.RECEIVED));
                fctxViewHolder.title.setTextColor(ContextCompat.getColor(
                        fctxViewHolder.title.getContext(),
                        R.color.blockchain_receive_green));
            }
        }

        fctxViewHolder.subtitle.setText(transaction.getNote());

        double btcBalance = transaction.getIntended_amount() / 1e8;
        double fiatBalance = btcExchangeRate * btcBalance;

        String fiatString = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
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

        fctxViewHolder.title.setText(spannable + " " + fctxViewHolder.title.getText());
    }

    private void bindTxView(RecyclerView.ViewHolder holder, int position) {
        TxViewHolder txViewHolder = (TxViewHolder) holder;
        String fiatString = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        final Tx tx = (Tx) transactions.get(position);
        double btcBalance = tx.getAmount() / 1e8;
        double fiatBalance = btcExchangeRate * btcBalance;

        txViewHolder.result.setTextColor(Color.WHITE);
        txViewHolder.timeSince.setText(dateUtil.formatted(tx.getTS()));

        String dirText = tx.getDirection();
        if (dirText.equals(MultiAddrFactory.MOVED))
            txViewHolder.direction.setText(txViewHolder.direction.getContext().getResources().getString(R.string.MOVED));
        if (dirText.equals(MultiAddrFactory.RECEIVED))
            txViewHolder.direction.setText(txViewHolder.direction.getContext().getResources().getString(R.string.RECEIVED));
        if (dirText.equals(MultiAddrFactory.SENT))
            txViewHolder.direction.setText(txViewHolder.direction.getContext().getResources().getString(R.string.SENT));

        Spannable spannable;
        if (isBtc) {
            spannable = Spannable.Factory.getInstance().newSpannable(
                    monetaryUtil.getDisplayAmountWithFormatting(Math.abs(tx.getAmount())) + " " + getDisplayUnits());
            spannable.setSpan(
                    new RelativeSizeSpan(0.67f), spannable.length() - getDisplayUnits().length(), spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            spannable = Spannable.Factory.getInstance().newSpannable(
                    monetaryUtil.getFiatFormat(fiatString).format(Math.abs(fiatBalance)) + " " + fiatString);
            spannable.setSpan(
                    new RelativeSizeSpan(0.67f), spannable.length() - 3, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        txViewHolder.result.setText(spannable);

        int nbConfirmations = 3;
        if (tx.isMove()) {
            txViewHolder.result.setBackgroundResource(
                    tx.getConfirmations() < nbConfirmations
                            ? R.drawable.rounded_view_lighter_blue_50 : R.drawable.rounded_view_lighter_blue);

            txViewHolder.direction.setTextColor(ContextCompat.getColor(txViewHolder.direction.getContext(),
                    tx.getConfirmations() < nbConfirmations
                            ? R.color.blockchain_transfer_blue_50 : R.color.blockchain_transfer_blue));

        } else if (btcBalance < 0.0) {
            txViewHolder.result.setBackgroundResource(
                    tx.getConfirmations() < nbConfirmations
                            ? R.drawable.rounded_view_red_50 : R.drawable.rounded_view_red);

            txViewHolder.direction.setTextColor(ContextCompat.getColor(txViewHolder.direction.getContext(),
                    tx.getConfirmations() < nbConfirmations
                            ? R.color.blockchain_red_50 : R.color.blockchain_send_red));

        } else {
            txViewHolder.result.setBackgroundResource(
                    tx.getConfirmations() < nbConfirmations
                            ? R.drawable.rounded_view_green_50 : R.drawable.rounded_view_green);

            txViewHolder.direction.setTextColor(ContextCompat.getColor(txViewHolder.direction.getContext(),
                    tx.getConfirmations() < nbConfirmations
                            ? R.color.blockchain_green_50 : R.color.blockchain_receive_green));
        }

        if (tx.isWatchOnly()) {
            txViewHolder.watchOnly.setVisibility(View.VISIBLE);
        } else {
            txViewHolder.watchOnly.setVisibility(View.GONE);
        }

        if (tx.isDoubleSpend()) {
            txViewHolder.doubleSpend.setVisibility(View.VISIBLE);
        } else {
            txViewHolder.doubleSpend.setVisibility(View.GONE);
        }

        txViewHolder.result.setOnClickListener(v -> {
            onViewFormatUpdated(!isBtc);
            if (listClickListener != null) listClickListener.onValueClicked(isBtc);
        });

        txViewHolder.touchView.setOnClickListener(v -> {
            if (listClickListener != null) listClickListener.onTransactionClicked(position);
        });
    }

    private String getDisplayUnits() {
        return (String) monetaryUtil.getBTCUnits()[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];
    }

    @Override
    public int getItemCount() {
        return transactions != null ? transactions.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (transactions.get(position) instanceof String) {
            return ITEM_TYPE_HEADER;
        } else if (transactions.get(position) instanceof FacilitatedTransaction) {
            return ITEM_TYPE_FCTX;
        } else if (transactions.get(position) instanceof Tx) {
            return ITEM_TYPE_TRANSACTION;
        } else {
            throw new IllegalArgumentException(
                    "Object list contained unsupported item: " + transactions.get(position));
        }
    }

    void onTransactionsUpdated(List<Object> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
        // TODO: 03/02/2017 This will be an absolute beast to fix
//        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new BalanceDiffUtil(this.transactions, transactions));
//        this.transactions = transactions;
//        diffResult.dispatchUpdatesTo(this);
    }

    void setTxListClickListener(TxListClickListener listClickListener) {
        this.listClickListener = listClickListener;
    }

    void onViewFormatUpdated(boolean isBtc) {
        this.isBtc = isBtc;
        notifyAdapterDataSetChanged(null);
    }

    void notifyAdapterDataSetChanged(@Nullable Double btcExchangeRate) {
        if (btcExchangeRate != null) {
            this.btcExchangeRate = btcExchangeRate;
        }
        monetaryUtil.updateUnit(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        notifyDataSetChanged();
    }

    interface TxListClickListener {

        void onTransactionClicked(int position);

        void onValueClicked(boolean isBtc);

        void onFctxClicked(String fctxId);
    }

    private static class TxViewHolder extends RecyclerView.ViewHolder {

        View touchView;
        TextView result;
        TextView timeSince;
        TextView direction;
        TextView watchOnly;
        AppCompatImageView doubleSpend;

        TxViewHolder(View view) {
            super(view);
            touchView = view.findViewById(R.id.tx_touch_view);
            result = (TextView) view.findViewById(R.id.result);
            timeSince = (TextView) view.findViewById(R.id.ts);
            direction = (TextView) view.findViewById(R.id.direction);
            watchOnly = (TextView) view.findViewById(R.id.watch_only);
            doubleSpend = (AppCompatImageView) view.findViewById(R.id.double_spend_warning);
        }
    }

    private static class FctxViewHolder extends RecyclerView.ViewHolder {

        ImageView indicator;
        TextView title;
        TextView subtitle;

        FctxViewHolder(View itemView) {
            super(itemView);
            indicator = (ImageView) itemView.findViewById(R.id.imageview_indicator);
            title = (TextView) itemView.findViewById(R.id.transaction_title);
            subtitle = (TextView) itemView.findViewById(R.id.transaction_subtitle);
        }
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {

        TextView header;

        HeaderViewHolder(View itemView) {
            super(itemView);
            header = (TextView) itemView.findViewById(R.id.header_name);
        }
    }
}
