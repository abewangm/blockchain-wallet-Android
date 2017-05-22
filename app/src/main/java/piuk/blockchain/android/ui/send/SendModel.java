package piuk.blockchain.android.ui.send;

import info.blockchain.api.data.UnspentOutputs;
import info.blockchain.wallet.api.data.FeeOptions;

import java.math.BigInteger;
import java.util.HashMap;

@SuppressWarnings("WeakerAccess")
public class SendModel {

    // Vars used for warning user of large tx
    /**
     * Large TX Size limit in KB
     */
    public static final int LARGE_TX_SIZE = 1024;

    /**
     * Large TX limit fee in USD
     */
    public static final double LARGE_TX_FEE = 0.5;

    /**
     * Large TX limit expressed as percentage, where percentage is found by diving the fee by the
     * amount
     */
    public static final double LARGE_TX_PERCENTAGE = 1.0;

    String btcUnit;
    int btcUniti;
    String fiatUnit;
    double exchangeRate;

    /**
     * Priority and Regular fees, defined as Satoshis per byte
     */
    FeeOptions feeOptions;

    PendingTransaction pendingTransaction;
    BigInteger maxAvailable;
    /**
     * Currently selected <from address, unspent api response>, stored so we don't need to call API
     * repeatedly
     */
    HashMap<String, UnspentOutputs> unspentApiResponses;

    /**
     * Current total fee amount
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
