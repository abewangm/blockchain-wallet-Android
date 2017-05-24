package piuk.blockchain.android.ui.send;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ItemFeePriorityDropdownBinding;

public class FeePriorityAdapter extends ArrayAdapter<DisplayFeeOptions> {

    private Context context;
    private List<DisplayFeeOptions> feeOptions;

    FeePriorityAdapter(@NonNull Context context, @NonNull List<DisplayFeeOptions> feeOptions) {
        super(context, 0, feeOptions);
        this.context = context;
        this.feeOptions = feeOptions;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, parent, false);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, parent, true);
    }

    @NonNull
    private View getCustomView(int position, ViewGroup parent, boolean isDropdownView) {
        if (isDropdownView) {

            ItemFeePriorityDropdownBinding binding = DataBindingUtil.inflate(
                    LayoutInflater.from(getContext()),
                    R.layout.item_fee_priority_dropdown,
                    parent,
                    false);

            DisplayFeeOptions option = feeOptions.get(position);
            binding.title.setText(option.getTitle());
            binding.description.setText(option.getDescription());
            return binding.getRoot();

        } else {
            return new View(context);
        }
    }
}
