package piuk.blockchain.android.ui.receive;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import piuk.blockchain.android.R;

class ShareReceiveIntentAdapter extends RecyclerView.Adapter<ShareReceiveIntentAdapter.ViewHolder> {

    private final List<SendPaymentCodeData> paymentCodeData;
    private OnItemClickedListener itemClickedListener;
    private Context context;

    ShareReceiveIntentAdapter(List<SendPaymentCodeData> repoDataArrayList) {
        paymentCodeData = repoDataArrayList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View row = inflater.inflate(R.layout.receive_share_row, parent, false);

        return new ViewHolder(row);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        SendPaymentCodeData data = paymentCodeData.get(position);

        holder.titleTextView.setText(data.getTitle());
        holder.imageView.setImageDrawable(data.getLogo());

        holder.rootView.setOnClickListener(view -> {
            if (itemClickedListener != null) itemClickedListener.onItemClicked();
            context.startActivity(data.getIntent());
        });
    }

    @Override
    public int getItemCount() {
        return paymentCodeData != null ? paymentCodeData.size() : 0;
    }

    void setItemClickedListener(OnItemClickedListener itemClickedListener) {
        this.itemClickedListener = itemClickedListener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        TextView titleTextView;
        View rootView;

        ViewHolder(View itemView) {
            super(itemView);
            rootView = itemView;
            imageView = itemView.findViewById(R.id.share_app_image);
            titleTextView = itemView.findViewById(R.id.share_app_title);
        }
    }

    interface OnItemClickedListener {

        void onItemClicked();

    }
}
