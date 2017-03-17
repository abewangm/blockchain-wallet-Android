package piuk.blockchain.android.util;

import android.util.Log;
import android.util.Pair;

import info.blockchain.wallet.payload.PayloadManager;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BackupWalletUtil {

    // TODO: 16/03/2017 Inject PayloadManager through constructor and access this class via ViewModels
    public BackupWalletUtil() {
        // Empty Constructor
    }

    /**
     * Return ordered list of integer, string pairs which can be used to confirm mnemonic.
     *
     * @return List<Pair<Integer,String>>
     */
    public List<Pair<Integer, String>> getConfirmSequence(String secondPassword) {

        List<Pair<Integer, String>> toBeConfirmed = new ArrayList<>();
        List<String> s = getMnemonic(secondPassword);
        SecureRandom random = new SecureRandom();
        List<Integer> seen = new ArrayList<>();

        int sel;
        int i = 0;
        while (i < 3) {
            sel = random.nextInt(s.size());
            if (!seen.contains(sel)) {
                seen.add(sel);
                i++;
            }
        }

        Collections.sort(seen);

        for (int ii = 0; ii < 3; ii++) {
            toBeConfirmed.add(new Pair<>(seen.get(ii), s.get(seen.get(ii))));
        }

        return toBeConfirmed;
    }

    /**
     * Return mnemonic in the form of a string array.
     *
     * @return String[]
     */
    public List<String> getMnemonic(String secondPassword) {

        try {
            PayloadManager.getInstance().getPayload().decryptHDWallet(0, secondPassword);
            return PayloadManager.getInstance().getPayload().getHdWallets().get(0).getMnemonic();
        } catch (Exception e) {
            Log.e(BackupWalletUtil.class.getSimpleName(), "getMnemonic: ", e);
            return null;
        }
    }
}
