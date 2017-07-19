package piuk.blockchain.android.data.exchange.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by justin on 5/1/17.
 */

public class ExchangeData {

    public ExchangeData() {
        // Empty constructor
    }

    @JsonProperty("coinify")
    private CoinifyData coinify = null;

    @JsonProperty("sfox")
    private SfoxData sfox = null;

    public CoinifyData getCoinify() {
        return coinify;
    }

    public SfoxData getSfox() {
        return sfox;
    }
}
