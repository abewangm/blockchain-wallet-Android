package piuk.blockchain.android.ui.receive;

import info.blockchain.api.data.MultiAddress;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import java.math.BigInteger;
import piuk.blockchain.android.util.MonetaryUtil;

class AddressBalanceHelper {

    private MonetaryUtil monetaryUtil;
    private PayloadManager payloadManager;

    AddressBalanceHelper(MonetaryUtil monetaryUtil, PayloadManager payloadManager) {
        this.monetaryUtil = monetaryUtil;
        this.payloadManager = payloadManager;
    }

    /**
     * Returns the balance of an {@link Account} in Satoshis
     */
    long getAccountAbsoluteBalance(Account account) {
        return payloadManager.getAddressBalance(account.getXpub()).longValue();
    }

    /**
     * Returns the balance of an {@link Account}, formatted for display
     */
    String getAccountBalance(Account account, boolean isBTC, double btcExchange, String fiatUnit, String btcUnit) {

        long btcBalance = getAccountAbsoluteBalance(account);

        if (!isBTC) {
            double fiatBalance = btcExchange * (btcBalance / 1e8);
            return "(" + monetaryUtil.getFiatFormat(fiatUnit).format(fiatBalance) + " " + fiatUnit + ")";
        } else {
            return "(" + monetaryUtil.getDisplayAmount(btcBalance) + " " + btcUnit + ")";
        }
    }

    /**
     * Returns the balance of a {@link LegacyAddress} in Satoshis
     */
    long getAddressAbsoluteBalance(LegacyAddress legacyAddress) {
        return payloadManager.getAddressBalance(legacyAddress.getAddress()).longValue();
    }

    /**
     * Returns the balance of a {@link LegacyAddress}, formatted for display
     */
    String getAddressBalance(LegacyAddress legacyAddress, boolean isBTC, double btcExchange, String fiatUnit, String btcUnit) {

        long btcBalance = getAddressAbsoluteBalance(legacyAddress);

        if (!isBTC) {
            double fiatBalance = btcExchange * (btcBalance / 1e8);
            return "(" + monetaryUtil.getFiatFormat(fiatUnit).format(fiatBalance) + " " + fiatUnit + ")";
        } else {
            return "(" + monetaryUtil.getDisplayAmount(btcBalance) + " " + btcUnit + ")";
        }
    }
}
