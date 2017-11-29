package piuk.blockchain.android.ui.transactions;

public class TransactionDetailModel {

    private String address;
    private String value;
    private String displayUnits;
    private boolean addressDecodeError;

    TransactionDetailModel(String address, String value, String displayUnits) {
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

    public boolean hasAddressDecodeError() {
        return addressDecodeError;
    }

    public void setAddressDecodeError(boolean addressDecodeError) {
        this.addressDecodeError = addressDecodeError;
    }
}
