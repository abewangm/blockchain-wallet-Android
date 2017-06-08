package piuk.blockchain.android.data.websocket;

/**
 * Created by justin on 5/2/17.
 */

public class WebSocketReceiveEvent {
    private String address;
    private String hash;

    public WebSocketReceiveEvent (String address, String hash) {
        this.address = address;
        this.hash = hash;
    }

    public String getAddress() {
        return address;
    }

    public String getHash() {
        return hash;
    }
}
