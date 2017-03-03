package piuk.blockchain.android.ui.account;


import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import java.util.ArrayList;
import java.util.List;

public class ConsolidatedAccount extends Account {

    public enum Type {
        ALL_ACCOUNTS, ALL_IMPORTED_ADDRESSES
    }

    private List<LegacyAddress> legacyAddresses = null;
    private long amount;
    private Type type;

    public ConsolidatedAccount() {
        super();
        this.legacyAddresses = new ArrayList<LegacyAddress>();
    }

    public ConsolidatedAccount(String label, List<LegacyAddress> legacyAddresses, long amount) {
        setArchived(false);
        setLabel(label);
        setLegacyAddresses(legacyAddresses);
       setAmount(amount);
    }

    public List<LegacyAddress> getLegacyAddresses() {
        return this.legacyAddresses;
    }

    public void setLegacyAddresses(List<LegacyAddress> addrs) {
        this.legacyAddresses = addrs;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
