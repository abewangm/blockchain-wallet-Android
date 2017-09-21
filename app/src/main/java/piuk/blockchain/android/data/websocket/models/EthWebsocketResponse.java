package piuk.blockchain.android.data.websocket.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigInteger;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public class EthWebsocketResponse {

    @JsonProperty("op")
    private String op;
    @JsonProperty("account")
    private String account;
    @JsonProperty("balance")
    private BigInteger balance;
    @JsonProperty("nonce")
    private Long nonce;
    @JsonProperty("txHash")
    private String txHash;
    @JsonProperty("tx")
    private EthWebsocketTx tx;

    public String getOp() {
        return op;
    }

    public String getAccount() {
        return account;
    }

    public BigInteger getBalance() {
        return balance;
    }

    public Long getNonce() {
        return nonce;
    }

    public String getTxHash() {
        return txHash;
    }

    public EthWebsocketTx getTx() {
        return tx;
    }

}
