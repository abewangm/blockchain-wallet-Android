package piuk.blockchain.android.ui.account;


import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class ConsolidatedAccount extends Account {

    private List<LegacyAddress> legacyAddresses = null;
    private long amount;

    public ConsolidatedAccount() {
        legacyAddresses = new ArrayList<>();
    }

    public ConsolidatedAccount(String label, List<LegacyAddress> legacyAddresses, long amount) {
        setArchived(false);
        setLabel(label);
        setLegacyAddresses(legacyAddresses);
        setAmount(amount);
    }

    public List<LegacyAddress> getLegacyAddresses() {
        return legacyAddresses;
    }

    public void setLegacyAddresses(List<LegacyAddress> addrs) {
        legacyAddresses = addrs;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

}
