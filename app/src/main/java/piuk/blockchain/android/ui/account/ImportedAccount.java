package piuk.blockchain.android.ui.account;


import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import java.util.ArrayList;
import java.util.List;

public class ImportedAccount extends Account {

    private final String KEY_LABEL = "label";
    private final String KEY_AMOUNT = "amount";
    private final String KEY_ARCHIVED = "archived";

    private List<LegacyAddress> legacyAddresses = null;
    private long amount;

    public ImportedAccount() {
        super();
        this.legacyAddresses = new ArrayList<LegacyAddress>();
    }

    public ImportedAccount(String label, List<LegacyAddress> legacyAddresses, long amount) {
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
}
