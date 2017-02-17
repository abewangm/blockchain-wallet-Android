package piuk.blockchain.android.ui.transactions;

public class RecipientModel {

    private String address;
    private String value;
    private String displayUnits;

    RecipientModel(String address, String value, String displayUnits) {
        this.address = address;
        this.value = value;
        this.displayUnits = displayUnits;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayUnits() {
        return displayUnits;
    }
}
