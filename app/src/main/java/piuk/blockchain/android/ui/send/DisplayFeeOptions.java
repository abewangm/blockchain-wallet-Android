package piuk.blockchain.android.ui.send;

public class DisplayFeeOptions {

    private String title;
    private String description;

    DisplayFeeOptions(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

}
