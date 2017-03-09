package piuk.blockchain.android.ui.transactions;

import android.support.annotation.NonNull;
import info.blockchain.wallet.payload.PayloadManager;

public class PayloadDataManager {

    private PayloadManager payloadManager;

    public PayloadDataManager(PayloadManager payloadManager) {
        this.payloadManager = payloadManager;
    }

    /**
     * Converts any address to a label.
     * @param address Accepts account receive or change chain address, as well as legacy address.
     * @return Either the label associated with the address, or the original address
     */
    @NonNull
    public String addressToLabel(String address) {
        return payloadManager.getLabelFromAddress(address);
    }

}
