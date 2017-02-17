package piuk.blockchain.android.ui.account;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ItemAccount {

    @Nullable public String label;
    @Nullable public String displayBalance;
    @Nullable public String tag;
    @Nullable public Long absoluteBalance;

    @Nullable public Object accountObject;

    public ItemAccount(@NonNull String label,
                       @NonNull String displayBalance,
                       @Nullable String tag,
                       @Nullable Long absoluteBalance,
                       @Nullable Object accountObject) {
        this.label = label;
        this.displayBalance = displayBalance;
        this.tag = tag;
        this.absoluteBalance = absoluteBalance;
        this.accountObject = accountObject;
    }
}
