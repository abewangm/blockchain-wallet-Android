package piuk.blockchain.android.ui.send;

import com.fasterxml.jackson.annotation.JsonIgnore;

import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
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
    public BigInteger getTotal() {
        return bigIntAmount.add(bigIntFee);
    }

    @JsonIgnore
    public boolean isHD() {
        return (sendingObject.getAccountObject() instanceof Account);
    }

    @JsonIgnore
    public boolean isWatchOnly() {

        boolean watchOnly = false;

        if(!isHD()) {
            if(sendingObject.getAccountObject() instanceof LegacyAddress) {
                LegacyAddress legacyAddress = (LegacyAddress)sendingObject.getAccountObject();
                watchOnly = legacyAddress.isWatchOnly() && (legacyAddress.getPrivateKey() == null ||  legacyAddress.getPrivateKey().isEmpty());
            }
        }

        return watchOnly;
    }

    @JsonIgnore
    public String getDisplayableReceivingLabel() {
        if (receivingObject != null && receivingObject.getLabel() != null && !receivingObject.getLabel().isEmpty()) {
            return receivingObject.getLabel();
        } else {
            return receivingAddress;
        }
    }

    @JsonIgnore
    public void clear() {
        unspentOutputBundle = null;
        sendingObject = null;
        receivingAddress = null;
        note = null;
        receivingAddress = null;
        bigIntFee = null;
        bigIntAmount = null;
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
