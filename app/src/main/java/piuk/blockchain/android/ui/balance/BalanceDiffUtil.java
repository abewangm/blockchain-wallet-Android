package piuk.blockchain.android.ui.balance;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import info.blockchain.wallet.contacts.data.FacilitatedTransaction;
import info.blockchain.wallet.transaction.Tx;

import java.util.List;

import piuk.blockchain.android.data.contacts.ContactTransactionModel;

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
        return oldItems.get(oldItemPosition).hashCode() == newItems.get(newItemPosition).hashCode();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Object oldItem = oldItems.get(oldItemPosition);
        Object newItem = newItems.get(newItemPosition);

        if (!oldItem.getClass().equals(newItem.getClass())) {
            return false;
        } else {
            if (oldItem instanceof String) {
                return oldItem.equals(newItem);
            } else if (oldItem instanceof ContactTransactionModel) {
                ContactTransactionModel oldContactTxModel = (ContactTransactionModel) oldItem;
                ContactTransactionModel newContactTxModel = (ContactTransactionModel) newItem;
                FacilitatedTransaction oldFctx = oldContactTxModel.getFacilitatedTransaction();
                FacilitatedTransaction newFctx = newContactTxModel.getFacilitatedTransaction();

                return
                        // Contact Name
                        oldContactTxModel.getContactName().equals(newContactTxModel.getContactName())
                                // Amount
                                && oldFctx.getIntended_amount() == newFctx.getIntended_amount()
                                // Created
                                && oldFctx.getCreated() == newFctx.getCreated()
                                // ID
                                && (oldFctx.getId() != null ? oldFctx.getId() : "")
                                .equals((newFctx.getId() != null ? newFctx.getId() : ""))
                                // Hash
                                && (oldFctx.getTx_hash() != null ? oldFctx.getTx_hash() : "")
                                .equals((newFctx.getTx_hash() != null ? newFctx.getTx_hash() : ""))
                                // Role
                                && (oldFctx.getRole() != null ? oldFctx.getRole() : "")
                                .equals((newFctx.getRole() != null ? newFctx.getRole() : ""))
                                // State
                                && (oldFctx.getState() != null ? oldFctx.getState() : "")
                                .equals((newFctx.getState() != null ? newFctx.getState() : ""))
                                // Address
                                && (oldFctx.getAddress() != null ? oldFctx.getAddress() : "")
                                .equals((newFctx.getAddress() != null ? newFctx.getAddress() : ""))
                                // Note
                                && (oldFctx.getNote() != null ? oldFctx.getNote() : "")
                                .equals((newFctx.getNote() != null ? newFctx.getNote() : ""));
            } else if (oldItem instanceof Tx) {
                Tx oldTx = (Tx) oldItem;
                Tx newTx = (Tx) newItem;

                return
                        // Direction
                        oldTx.getDirection().equals(newTx.getDirection())
                                // Amount
                                && oldTx.getAmount() == newTx.getAmount()
                                // Confirmations
                                && oldTx.getConfirmations() == newTx.getConfirmations()
                                // Timestamp
                                && oldTx.getTS() == newTx.getTS()
                                // Watch only
                                && oldTx.isWatchOnly() == newTx.isWatchOnly();
            } else {
                throw new IllegalArgumentException("Object list contained unexpected object");
            }
        }
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }

}
