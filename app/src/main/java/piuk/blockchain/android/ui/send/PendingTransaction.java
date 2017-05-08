package piuk.blockchain.android.ui.send;

import com.fasterxml.jackson.annotation.JsonIgnore;

import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payment.SpendableUnspentOutputs;

import java.math.BigInteger;

import piuk.blockchain.android.ui.account.ItemAccount;

public class PendingTransaction {

    public SpendableUnspentOutputs unspentOutputBundle;
    public ItemAccount sendingObject;
    public ItemAccount receivingObject;
    public String note;
    public String receivingAddress;
    public BigInteger bigIntFee;
    public BigInteger bigIntAmount;
    public int addressToReceiveIndex;
    public boolean isCustomFee;

    @JsonIgnore
    public boolean isHD() {
        return (sendingObject.accountObject instanceof Account);
    }
}
