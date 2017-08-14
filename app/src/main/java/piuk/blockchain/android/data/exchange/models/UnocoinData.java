package piuk.blockchain.android.data.exchange.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnocoinData implements ExchangeAccount {

    public UnocoinData() {
        // Empty constructor
    }

    @JsonProperty("user")
    private String user = null;

    @JsonProperty("account_token")
    private String token = null;

    @JsonProperty("trades")
    private List<TradeData> trades = new ArrayList<>();

    public String getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public List<TradeData> getTrades() {
        return trades;
    }
}
