package piuk.blockchain.android.ui.send;

import info.blockchain.api.data.UnspentOutputs;
import info.blockchain.wallet.api.data.FeeList;
import java.math.BigInteger;
import java.util.HashMap;

public class SendModel {

    // Vars used for warning user of large tx
    /**
     * Large TX Size limit in KB
     */
    public static final int LARGE_TX_SIZE = 516;

    /**
     * Large TX limit fee in USD
     */
    public static final long LARGE_TX_FEE = 80000;

    /**
     * Large TX limit expressed as percentage
     */
    public static final double LARGE_TX_PERCENTAGE = 1.0;

    String btcUnit;
    int btcUniti;
    String fiatUnit;
    double exchangeRate;

    /**
     * Currently selected <from address, unspent api response>, stored so we don't need to call API
     * repeatedly
     */
    FeeList dynamicFeeList;

    PendingTransaction pendingTransaction;
    BigInteger maxAvailable;
    HashMap<String, UnspentOutputs> unspentApiResponses;
    public BigInteger[] absoluteSuggestedFeeEstimates;

    /**
     * Fee for 2nd block inclusion
     */
    BigInteger absoluteSuggestedFee;

    String verifiedSecondPassword;
    boolean isBTC;
    double btcExchange;

    /**
     * For storing a Contact name, account name etc
     */
    String recipient;

}
