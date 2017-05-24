package piuk.blockchain.android.ui.send;

import android.support.annotation.Nullable;

public class DisplayFeeOptions {

    private String title;
    private String description;
    private String fee;

    DisplayFeeOptions(String title, String description, @Nullable String fee) {
        this.title = title;
        this.description = description;
        this.fee = fee;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    @Nullable
    public String getFee() {
        return fee;
    }

}
