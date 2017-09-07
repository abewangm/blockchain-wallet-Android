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

    @JsonIgnore
    public boolean isHD() {
        return (sendingObject.getAccountObject() instanceof Account);
    }

    @Override
    public String toString() {
        return "PendingTransaction{" +
                "unspentOutputBundle=" + unspentOutputBundle +
                ", sendingObject=" + sendingObject +
                ", receivingObject=" + receivingObject +
                ", note='" + note + '\'' +
                ", receivingAddress='" + receivingAddress + '\'' +
                ", bigIntFee=" + bigIntFee +
                ", bigIntAmount=" + bigIntAmount +
                ", addressToReceiveIndex=" + addressToReceiveIndex +
                '}';
    }
}
