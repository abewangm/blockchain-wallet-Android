package piuk.blockchain.android.data.exchange;

/**
 * Created by justin on 5/9/17.
 */

public class WebLoginDetails {
    private String walletJson;
    private String password;
    private String externalJson;
    private String magicHash;

    public WebLoginDetails(String walletJson, String password, String externalJson, String magicHash) {
        this.walletJson = walletJson;
        this.password = password;
        this.externalJson = externalJson;
        this.magicHash = magicHash;
    }

    public String getWalletJson() {
        return walletJson;
    }

    public String getPassword() {
        return password;
    }

    public String getExternalJson() {
        return externalJson;
    }

    public String getMagicHash() {
        return magicHash;
    }
}
