package info.blockchain.wallet.viewModel;

import android.databinding.BaseObservable;

import info.blockchain.wallet.model.ItemAccount;

public class ItemSendAddressViewModel extends BaseObservable implements ViewModel {

    private ItemAccount addressItem;

    public ItemSendAddressViewModel(ItemAccount address) {
        this.addressItem = address;
    }

    public String getLabel() {
        return addressItem.label;
    }

    public String getBalance() {
        return addressItem.balance;
    }

    public void setAddress(ItemAccount address) {
        this.addressItem = address;
        notifyChange();
    }

    @Override
    public void destroy() {
        addressItem = null;
    }

}
