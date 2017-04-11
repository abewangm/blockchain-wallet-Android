package piuk.blockchain.android.ui.contacts.detail;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import info.blockchain.wallet.contacts.data.FacilitatedTransaction;

import java.util.List;

class ContactTransactionDiffUtil extends DiffUtil.Callback {

    private List<FacilitatedTransaction> oldTransactions;
    private List<FacilitatedTransaction> newTransactions;

    ContactTransactionDiffUtil(List<FacilitatedTransaction> oldTransactions, List<FacilitatedTransaction> newTransactions) {
        this.oldTransactions = oldTransactions;
        this.newTransactions = newTransactions;
    }

    @Override
    public int getOldListSize() {
        return oldTransactions != null ? oldTransactions.size() : 0;
    }

    @Override
    public int getNewListSize() {
        return newTransactions != null ? newTransactions.size() : 0;
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldTransactions.get(oldItemPosition).getId().equals(
                newTransactions.get(newItemPosition).getId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        FacilitatedTransaction oldContact = oldTransactions.get(oldItemPosition);
        FacilitatedTransaction newContact = newTransactions.get(newItemPosition);

        return oldContact.equals(newContact);
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}

