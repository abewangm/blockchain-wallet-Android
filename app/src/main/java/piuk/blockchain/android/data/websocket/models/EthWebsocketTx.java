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
public class EthWebsocketTx {

    @JsonProperty("blockHash")
    private String blockHash;
    @JsonProperty("blockNumber")
    private Long blockNumber;
    @JsonProperty("from")
    private String from;
    @JsonProperty("gas")
    private Long gas;
    @JsonProperty("gasPrice")
    private BigInteger gasPrice;
    @JsonProperty("hash")
    private String hash;
    @JsonProperty("input")
    private String input;
    @JsonProperty("nonce")
    private Long nonce;
    @JsonProperty("to")
    private String to;
    @JsonProperty("transactionIndex")
    private Long transactionIndex;
    @JsonProperty("value")
    private BigInteger value;
    @JsonProperty("v")
    private String v;
    @JsonProperty("r")
    private String r;
    @JsonProperty("s")
    private String s;

    public String getBlockHash() {
        return blockHash;
    }

    public Long getBlockNumber() {
        return blockNumber;
    }

    public String getFrom() {
        return from;
    }

    public Long getGas() {
        return gas;
    }

    public BigInteger getGasPrice() {
        return gasPrice;
    }

    public String getHash() {
        return hash;
    }

    public String getInput() {
        return input;
    }

    public Long getNonce() {
        return nonce;
    }

    public String getTo() {
        return to;
    }

    public Long getTransactionIndex() {
        return transactionIndex;
    }

    public BigInteger getValue() {
        return value;
    }

    public String getV() {
        return v;
    }

    public String getR() {
        return r;
    }

    public String getS() {
        return s;
    }

}
