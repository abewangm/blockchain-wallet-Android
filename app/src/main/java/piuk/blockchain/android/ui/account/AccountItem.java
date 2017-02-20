package piuk.blockchain.android.ui.account;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class AccountItem {

    static final int TYPE_CREATE_NEW_WALLET_BUTTON = 0;
    static final int TYPE_IMPORT_ADDRESS_BUTTON = 1;
    static final int TYPE_ACCOUNT = 2;

    private String label;
    private String address;
    private String amount;
    private boolean isArchived;
    private boolean isWatchOnly;
    private boolean isDefault;
    private Integer correctedPosition;
    private int type;

    AccountItem(@Nullable Integer correctedPosition,
                String label,
                String address,
                String amount,
                boolean isArchived,
                boolean isWatchOnly,
                boolean isDefault,
                int type) {
        this.correctedPosition = correctedPosition;
        this.label = label;
        this.address = address;
        this.amount = amount;
        this.isArchived = isArchived;
        this.isWatchOnly = isWatchOnly;
        this.isDefault = isDefault;
        this.type = type;
    }

    AccountItem(int type) {
        this.type = type;
    }

    @NonNull
    public String getLabel() {
        return label != null ? label : "";
    }

    @NonNull
    public String getAddress() {
        return address != null ? address : "";
    }

    @NonNull
    public String getAmount() {
        return amount != null ? amount : "";
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setIsArchived(boolean isArchived) {
        this.isArchived = isArchived;
    }

    public boolean isWatchOnly() {
        return isWatchOnly;
    }

    public void setIsWatchOnly(boolean isWatchOnly) {
        this.isWatchOnly = isWatchOnly;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public int getType() {
        return type;
    }

    @NonNull
    Integer getCorrectPosition() {
        return correctedPosition != null ? correctedPosition : -1;
    }
}