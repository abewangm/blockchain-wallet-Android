package piuk.blockchain.android.data.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by justin on 5/1/17.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoinifyData implements ExchangeAccount {
    @JsonProperty("user")
    private int user = 0;

    @JsonProperty("offline_token")
    private String token = null;

    @JsonProperty("trades")
    private List<TradeData> trades = null;

    public int getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public List<TradeData> getTrades() {
        return trades;
    }
}
