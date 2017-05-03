package piuk.blockchain.android.data.websocket;

/**
 * Created by justin on 5/2/17.
 */

public class WebSocketReceiveEvent {
    private String address;

    public WebSocketReceiveEvent (String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }
}
