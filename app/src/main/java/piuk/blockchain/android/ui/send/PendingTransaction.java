package piuk.blockchain.android.ui.send;

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

    public boolean isHD() {
        return (sendingObject.accountObject instanceof Account);
    }

    @Override
    public String toString() {
        return "PendingTransaction{" +
                "\nunspentOutputBundle.getAbsoluteFee()=" + unspentOutputBundle.getAbsoluteFee() +
                "\nunspentOutputBundle.getConsumedAmount()=" + unspentOutputBundle.getConsumedAmount() +
                "\nunspentOutputBundle.getSpendableOutputs().size()=" + unspentOutputBundle.getSpendableOutputs().size() +
                ",\nsendingObject=" + sendingObject +
                ",\nreceivingObject=" + receivingObject +
                ",\nnote='" + note + '\'' +
                ",\nreceivingAddress='" + receivingAddress + '\'' +
                ",\nbigIntFee=" + bigIntFee +
                ",\nbigIntAmount=" + bigIntAmount +
                ",\naddressToReceiveIndex=" + addressToReceiveIndex +
                '}';
    }
}
