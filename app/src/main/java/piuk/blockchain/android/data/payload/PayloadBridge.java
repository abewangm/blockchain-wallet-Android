package piuk.blockchain.android.data.payload;

import android.os.Looper;
import android.support.annotation.Nullable;

import info.blockchain.wallet.exceptions.EncryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.payload.PayloadManager;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * PayloadBridge.java : singleton class for remote save of payload
 */
public class PayloadBridge {

    private static PayloadBridge instance;
    private static PayloadManager payloadManager;

    private PayloadBridge() {
    }

    public static PayloadBridge getInstance() {

        if (instance == null) {
            instance = new PayloadBridge();
            payloadManager = PayloadManager.getInstance();
        }

        return instance;
    }

    public interface PayloadSaveListener {
        void onSaveSuccess();

        void onSaveFail();
    }

    /**
     * Thread for remote save of payload to server.
     */
    public void remoteSaveThread(@Nullable PayloadSaveListener listener) {

        new Thread(() -> {
            Looper.prepare();

            try {
                if (payloadManager.save()) {
                    if (listener != null) listener.onSaveSuccess();
                } else {
                    if (listener != null) listener.onSaveFail();
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (listener != null) listener.onSaveFail();
            }

            Looper.loop();

        }).start();
    }
}