package piuk.blockchain.android.data.websocket.models

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
data class EthWebsocketResponse(
        val op: String,
        val account: String,
        val balance: String,
        val nonce: Long,
        val txHash: String,
        val tx: EthWebsocketTx
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
data class EthWebsocketTx(
        val blockHash: String,
        val blockNumber: Long,
        val from: String,
        val gas: Long,
        val gasPrice: String,
        val hash: String,
        val input: String,
        val nonce: Long,
        val to: String,
        val transactionIndex: Long,
        val value: String,
        val v: String,
        val r: String,
        val s: String
)