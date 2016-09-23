package piuk.blockchain.android.util;

import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.util.CharSequenceX;

public class AESUtilWrapper {

    public String decrypt(String ciphertext, CharSequenceX password, int iterations) {
        return AESUtil.decrypt(ciphertext, password, iterations);
    }

}
