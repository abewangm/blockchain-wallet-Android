package piuk.blockchain.android.data.exchange;

import org.spongycastle.util.encoders.Hex;

/**
 * Created by justin on 5/9/17.
 */

public class WebViewLoginDetails {
    private String walletJson;
    private String password;
    private String externalJson;
    private String magicHash;

    public WebViewLoginDetails(String walletJson, String password, String externalJson, byte[] magicHash) {
        this.walletJson = walletJson;
        this.password = password;
        this.externalJson = externalJson == null ? "" : externalJson;
        this.magicHash = magicHash == null ? "" : Hex.toHexString(magicHash);
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
