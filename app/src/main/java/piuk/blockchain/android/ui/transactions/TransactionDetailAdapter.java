package piuk.blockchain.android.ui.transactions;

import android.annotation.SuppressLint;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.List;

import piuk.blockchain.android.R;

public class TransactionDetailAdapter extends BaseAdapter implements SpinnerAdapter {

    private List<TransactionDetailModel> mRecipients;

    public TransactionDetailAdapter(List<TransactionDetailModel> recipients) {
        mRecipients = recipients;
    }

    @Override
    public int getCount() {
        return mRecipients != null ? mRecipients.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return mRecipients.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mRecipients.hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    // Specifying parent not allowed in AdapterView
    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewholder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.spinner_item_transaction_detail, null);
            viewholder = new ViewHolder();
            viewholder.address = convertView.findViewById(R.id.address);
            viewholder.amount = convertView.findViewById(R.id.amount);

            convertView.setTag(viewholder);
        } else {
            viewholder = (ViewHolder) convertView.getTag();
        }

        TransactionDetailModel recipient = (TransactionDetailModel) getItem(position);
        viewholder.address.setText(recipient.getAddress());
        viewholder.amount.setText(
                (recipient.getValue()
                        + " "
                        + recipient.getDisplayUnits()));

        if (recipient.hasAddressDecodeError()) {
            viewholder.address.setTextColor(ResourcesCompat.getColor(parent.getContext().getResources(),
                    R.color.product_red_medium, parent.getContext().getTheme()));
        }

        return convertView;
    }

    private static class ViewHolder {

        TextView address;
        TextView amount;

        ViewHolder() {
            // Empty constructor
        }
    }

}
