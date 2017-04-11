package piuk.blockchain.android.ui.balance;

import android.support.v7.util.DiffUtil;

import java.util.List;

// TODO: 20/03/2017 This isn't working correctly at the moment. BalanceViewModel needs re-writing.
class BalanceDiffUtil extends DiffUtil.Callback {

    private List<Object> oldItems;
    private List<Object> newItems;

    BalanceDiffUtil(List<Object> oldItems, List<Object> newItems) {
        this.oldItems = oldItems;
        this.newItems = newItems;
    }

    @Override
    public int getOldListSize() {
        return oldItems != null ? oldItems.size() : 0;
    }

    @Override
    public int getNewListSize() {
        return newItems != null ? newItems.size() : 0;
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldItems.getClass().equals(newItems.getClass())
                && oldItems.get(oldItemPosition).hashCode() == newItems.get(newItemPosition).hashCode();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Object oldItem = oldItems.get(oldItemPosition);
        Object newItem = newItems.get(newItemPosition);
        return oldItem.equals(newItem);
    }

}
