package piuk.blockchain.android.ui.swipetoreceive;

import android.support.annotation.NonNull;

import info.blockchain.api.data.Balance;
import info.blockchain.wallet.payload.data.Account;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.ui.transactions.PayloadDataManager;
import piuk.blockchain.android.util.PrefsUtil;

public class SwipeToReceiveHelper {

    public static final String KEY_SWIPE_RECEIVE_ADDRESSES = "swipe_receive_addresses";
    public static final String KEY_SWIPE_RECEIVE_ACCOUNT_NAME = "swipe_receive_account_name";

    private PayloadDataManager payloadDataManager;
    private PrefsUtil prefsUtil;

    public SwipeToReceiveHelper(PayloadDataManager payloadDataManager, PrefsUtil prefsUtil) {
        this.payloadDataManager = payloadDataManager;
        this.prefsUtil = prefsUtil;
    }

    /**
     * Derives 5 addresses from the current point on the receive chain. Stores them alongside
     * the account name in SharedPrefs. Only stores addresses if enabled in SharedPrefs.
     */
    public void updateAndStoreAddresses() {
        if (getIfSwipeEnabled()) {
            int numOfAddresses = 5;

            Account defaultAccount = payloadDataManager.getDefaultAccount();
            String receiveAccountName = defaultAccount.getLabel();
            storeAccountName(receiveAccountName);

            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 0; i < numOfAddresses; i++) {
                String receiveAddress = payloadDataManager.getReceiveAddressAtPosition(defaultAccount, i);
                if (receiveAddress == null) {
                    // Likely not initialized yet
                    break;
                }

                stringBuilder.append(receiveAddress).append(",");
            }

            storeAddresses(stringBuilder.toString());
        }
    }

    /**
     * Returns the next unused address from the list of 5 stored for swipe to receive. Can return an
     * empty String if no unused addresses are found.
     */
    Observable<String> getNextAvailableAddress() {
        return getBalanceOfAddresses(getReceiveAddresses())
                .map(map -> {
                    for (Map.Entry<String, Balance> entry : map.entrySet()) {
                        String address = entry.getKey();
                        BigInteger balance = entry.getValue().getFinalBalance();
                        if (balance.compareTo(BigInteger.ZERO) == 0) {
                            return address;
                        }
                    }
                    return "";
                });
    }

    /**
     * Returns a List of the next 5 available unused (at the time of storage) receive addresses. Can
     * return an empty list.
     */
    @NonNull
    List<String> getReceiveAddresses() {
        String addressString = prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, "");
        if (addressString.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.asList(addressString.split(","));
    }

    /**
     * Returns the account name associated with the receive addresses.
     */
    @NonNull
    String getAccountName() {
        return prefsUtil.getValue(KEY_SWIPE_RECEIVE_ACCOUNT_NAME, "");
    }

    private boolean getIfSwipeEnabled() {
        return prefsUtil.getValue(PrefsUtil.KEY_SWIPE_TO_RECEIVE_ENABLED, true);
    }

    private Observable<HashMap<String, Balance>> getBalanceOfAddresses(List<String> addresses) {
        return payloadDataManager.getBalanceOfAddresses(addresses)
                .compose(RxUtil.applySchedulersToObservable());
    }

    private void storeAddresses(String addresses) {
        prefsUtil.setValue(KEY_SWIPE_RECEIVE_ADDRESSES, addresses);
    }

    private void storeAccountName(String accountName) {
        prefsUtil.setValue(KEY_SWIPE_RECEIVE_ACCOUNT_NAME, accountName);
    }
}
