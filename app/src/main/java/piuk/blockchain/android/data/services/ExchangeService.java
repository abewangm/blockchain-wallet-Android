package piuk.blockchain.android.data.services;

import android.util.Log;

import info.blockchain.wallet.metadata.Metadata;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.HDWallet;
import info.blockchain.wallet.util.MetadataUtil;

import org.bitcoinj.crypto.DeterministicKey;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by justin on 5/1/17.
 */

public class ExchangeService {
    private static ExchangeService instance;
    private static final int METADATA_TYPE_EXCHANGE = 3;

    public static final String TAG = ExchangeService.class.getSimpleName();

    private PayloadManager payloadManager;
    private PublishSubject<Metadata> metadataSubject;
    private boolean didStartLoad;

    private ExchangeService() {
        this.payloadManager = PayloadManager.getInstance();
        this.metadataSubject = PublishSubject.create();
    }

    public static ExchangeService getInstance() {
        if (instance == null) {
            instance = new ExchangeService();
        }
        return instance;
    }

    public Observable<Metadata> getExchangeData() {
        return this.metadataSubject;
    }

    public void loadExchangeData() {
        if (!didStartLoad) {
            reloadExchangeData();
            didStartLoad = true;
        }
    }

    public void reloadExchangeData() {
        new Thread() {
            @Override
            public void run() {
                try {
                    Metadata exchangeData = getMetadata();
                    metadataSubject.onNext(exchangeData);
                } catch (Exception e) {
                    Log.d(TAG, "reloadExchangeMetadata error: " + e.getMessage());
                }

            }
        }.start();
    }

    private Metadata getMetadata() throws Exception {
        DeterministicKey masterKey = this.payloadManager
                .getPayload()
                .getHdWallets().get(0)
                .getMasterKey();
        DeterministicKey metadataHDNode = MetadataUtil.deriveMetadataNode(masterKey);
        return new Metadata.Builder(metadataHDNode, METADATA_TYPE_EXCHANGE).build();
    }
}
