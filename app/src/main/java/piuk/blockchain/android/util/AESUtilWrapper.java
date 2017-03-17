package piuk.blockchain.android.util;

import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.exceptions.DecryptionException;

import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.UnsupportedEncodingException;

public class AESUtilWrapper {

    public String decrypt(String ciphertext, String password, int iterations) throws UnsupportedEncodingException, InvalidCipherTextException, DecryptionException {
        return AESUtil.decrypt(ciphertext, password, iterations);
    }

    public String encrypt(String plaintext, String password, int iterations) throws Exception {
        return AESUtil.encrypt(plaintext, password, iterations);
    }

}
