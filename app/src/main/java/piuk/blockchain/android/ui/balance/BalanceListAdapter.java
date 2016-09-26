package piuk.blockchain.android.ui.balance;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Tx;

import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.util.DateUtil;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;

class BalanceListAdapter extends RecyclerView.Adapter<BalanceListAdapter.ViewHolder> {

    private List<Tx> mTransactions;
    private PrefsUtil mPrefsUtil;
    private MonetaryUtil mMonetaryUtil;
    private DateUtil mDateUtil;
    private double mBtcExchangeRate;
    private boolean mIsBtc;
    private TxListClickListener mListClickListener;

    BalanceListAdapter(List<Tx> transactions, PrefsUtil prefsUtil, MonetaryUtil monetaryUtil, DateUtil dateUtil, double btcExchangeRate, boolean isBtc) {
        mTransactions = transactions;
        mPrefsUtil = prefsUtil;
        mMonetaryUtil = monetaryUtil;
        mDateUtil = dateUtil;
        mBtcExchangeRate = btcExchangeRate;
        mIsBtc = isBtc;
    }

    @Override
    public BalanceListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.txs_layout_expandable, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        String strFiat = mPrefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);

        if (mTransactions != null) {
            final Tx tx = mTransactions.get(position);
            double btcBalance = tx.getAmount() / 1e8;
            double fiatBalance = mBtcExchangeRate * btcBalance;

            holder.result.setTextColor(Color.WHITE);
            holder.timeSince.setText(mDateUtil.formatted(tx.getTS()));

            String dirText = tx.getDirection();
            if (dirText.equals(MultiAddrFactory.MOVED))
                holder.direction.setText(holder.direction.getContext().getResources().getString(R.string.MOVED));
            if (dirText.equals(MultiAddrFactory.RECEIVED))
                holder.direction.setText(holder.direction.getContext().getResources().getString(R.string.RECEIVED));
            if (dirText.equals(MultiAddrFactory.SENT))
                holder.direction.setText(holder.direction.getContext().getResources().getString(R.string.SENT));

            Spannable span1;
            if (mIsBtc) {
                span1 = Spannable.Factory.getInstance().newSpannable(mMonetaryUtil.getDisplayAmountWithFormatting(Math.abs(tx.getAmount())) + " " + getDisplayUnits());
                span1.setSpan(new RelativeSizeSpan(0.67f), span1.length() - getDisplayUnits().length(), span1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                span1 = Spannable.Factory.getInstance().newSpannable(mMonetaryUtil.getFiatFormat(strFiat).format(Math.abs(fiatBalance)) + " " + strFiat);
                span1.setSpan(new RelativeSizeSpan(0.67f), span1.length() - 3, span1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            int nbConfirmations = 3;
            if (tx.isMove()) {
                holder.result.setBackgroundResource(tx.getConfirmations() < nbConfirmations ? R.drawable.rounded_view_lighter_blue_50 : R.drawable.rounded_view_lighter_blue);
                holder.direction.setTextColor(holder.direction.getContext().getResources().getColor(tx.getConfirmations() < nbConfirmations ? R.color.blockchain_transfer_blue_50 : R.color.blockchain_transfer_blue));
            } else if (btcBalance < 0.0) {
                holder.result.setBackgroundResource(tx.getConfirmations() < nbConfirmations ? R.drawable.rounded_view_red_50 : R.drawable.rounded_view_red);
                holder.direction.setTextColor(holder.direction.getContext().getResources().getColor(tx.getConfirmations() < nbConfirmations ? R.color.blockchain_red_50 : R.color.blockchain_send_red));
            } else {
                holder.result.setBackgroundResource(tx.getConfirmations() < nbConfirmations ? R.drawable.rounded_view_green_50 : R.drawable.rounded_view_green);
                holder.direction.setTextColor(holder.direction.getContext().getResources().getColor(tx.getConfirmations() < nbConfirmations ? R.color.blockchain_green_50 : R.color.blockchain_receive_green));
            }

            TextView tvWatchOnly = (TextView) holder.itemView.findViewById(R.id.watch_only);
            if (tx.isWatchOnly()) {
                tvWatchOnly.setVisibility(View.VISIBLE);
            } else {
                tvWatchOnly.setVisibility(View.GONE);
            }

            holder.result.setText(span1);

            holder.result.setOnTouchListener((v, event) -> {
                View parent = (View) v.getParent();
                event.setLocation(v.getX() + (v.getWidth() / 2), v.getY() + (v.getHeight() / 2));
                parent.onTouchEvent(event);

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    mIsBtc = !mIsBtc;
                    if (mListClickListener != null) mListClickListener.onValueClicked(mIsBtc);
                }
                return true;
            });

            holder.touchView.setOnTouchListener((v, event) -> {

                View parent = (View) v.getParent();
                event.setLocation(event.getX(), v.getY() + (v.getHeight() / 2));
                parent.onTouchEvent(event);

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (mListClickListener != null) mListClickListener.onRowClicked(position);
                }
                return true;
            });
        }
    }

    private String getDisplayUnits() {
        return (String) mMonetaryUtil.getBTCUnits()[mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];
    }

    @Override
    public int getItemCount() {
        return mTransactions != null ? mTransactions.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    void onTransactionsUpdated(List<Tx> transactions) {
        mTransactions = transactions;
        notifyDataSetChanged();
    }

    void setTxListClickListener(TxListClickListener listClickListener) {
        mListClickListener = listClickListener;
    }

    interface TxListClickListener {

        void onRowClicked(int position);

        void onValueClicked(boolean isBtc);

    }

    class ViewHolder extends RecyclerView.ViewHolder {

        View touchView;
        TextView result;
        TextView timeSince;
        TextView direction;
        TextView watchOnly;

        ViewHolder(View view) {
            super(view);
            touchView = view.findViewById(R.id.tx_touch_view);
            result = (TextView) view.findViewById(R.id.result);
            timeSince = (TextView) view.findViewById(R.id.ts);
            direction = (TextView) view.findViewById(R.id.direction);
            watchOnly = (TextView) view.findViewById(R.id.watch_only);
        }
    }
}
