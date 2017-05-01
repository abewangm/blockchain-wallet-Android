package piuk.blockchain.android.data.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by justin on 5/1/17.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeData {
    @JsonProperty("id")
    private int id = 0;

    @JsonProperty("state")
    private String state = null;

    @JsonProperty("confirmed")
    private boolean confirmed = false;

    @JsonProperty("is_buy")
    private boolean isBuy = true;

    @JsonProperty("account_index")
    private int accountIndex = 0;

    @JsonProperty("receive_index")
    private int receiveIndex = 0;

    public int getId() {
        return id;
    }

    public String getState() {
        return state;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean isBuy() {
        return isBuy;
    }

    public int getAccountIndex() {
        return accountIndex;
    }

    public int getReceiveIndex() {
        return receiveIndex;
    }
}
