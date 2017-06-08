package piuk.blockchain.android.data.exchange;

import android.support.annotation.NonNull;

/**
 * Created by justin on 5/9/17.
 */

public class WebViewLoginDetails {

    private String walletJson;
    private String password;
    private String externalJson;
    private String magicHash;

    public WebViewLoginDetails(String walletJson,
                               String password,
                               @NonNull String externalJson,
                               @NonNull String magicHash) {
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
