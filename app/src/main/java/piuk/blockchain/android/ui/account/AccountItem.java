package piuk.blockchain.android.ui.account;

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

class AccountItem {

    private String label;
    private String address;
    private Drawable icon;
    private String amount;
    private boolean isArchived;
    private boolean isWatchOnly;
    private boolean isDefault;
    private Integer correctedPosition;

    AccountItem(@Nullable Integer correctedPosition, String title, String address, String amount, Drawable icon, boolean isArchived, boolean isWatchOnly, boolean isDefault) {
        this.correctedPosition = correctedPosition;
        this.label = title;
        this.address = address;
        this.amount = amount;
        this.icon = icon;
        this.isArchived = isArchived;
        this.isWatchOnly = isWatchOnly;
        this.isDefault = isDefault;
    }

    public String getLabel() {
        return label;
    }

    public String getAddress() {
        return address;
    }

    public Drawable getIcon() {
        return icon;
    }

    public String getAmount() {
        return amount;
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

    @Nullable
    public Integer getCorrectPosition() {
        return correctedPosition;
    }
}