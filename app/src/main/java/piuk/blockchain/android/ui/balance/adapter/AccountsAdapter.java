package piuk.blockchain.android.ui.balance.adapter;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ItemBalanceAccountDropdownBinding;
import piuk.blockchain.android.databinding.SpinnerBalanceHeaderBinding;
import piuk.blockchain.android.ui.account.ItemAccount;

public class AccountsAdapter extends ArrayAdapter<ItemAccount> {

    private List<ItemAccount> accountList;

    public AccountsAdapter(Context context,
                    int textViewResourceId,
                    List<ItemAccount> accountList) {
        super(context, textViewResourceId, accountList);
        this.accountList = accountList;
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, parent, true);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, parent, false);
    }

    private View getCustomView(int position, ViewGroup parent, boolean isDropdownView) {
        if (isDropdownView) {
            ItemBalanceAccountDropdownBinding binding = DataBindingUtil.inflate(
                    LayoutInflater.from(getContext()),
                    R.layout.item_balance_account_dropdown,
                    parent,
                    false);

            ItemAccount item = getItem(position);

            binding.accountName.setText(item.getLabel());
            binding.balance.setText(item.getDisplayBalance());

            return binding.getRoot();

        } else {
            SpinnerBalanceHeaderBinding binding = DataBindingUtil.inflate(
                    LayoutInflater.from(getContext()),
                    R.layout.spinner_balance_header,
                    parent,
                    false);

            ItemAccount item = getItem(position);

            binding.text.setText(item.getLabel());
            return binding.getRoot();
        }
    }

    public void updateAccountList(List<ItemAccount> accountList) {
        this.accountList.clear();
        this.accountList.addAll(accountList);
        notifyDataSetChanged();
    }

    public boolean isNotEmpty() {
        return !accountList.isEmpty();
    }

    public boolean showSpinner() {
        return accountList.size() > 1;
    }
}