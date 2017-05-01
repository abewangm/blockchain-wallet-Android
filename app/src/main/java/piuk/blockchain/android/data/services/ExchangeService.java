package piuk.blockchain.android.data.services;

import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import info.blockchain.wallet.metadata.Metadata;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.MetadataUtil;

import org.bitcoinj.crypto.DeterministicKey;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.subjects.ReplaySubject;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.exchange.ExchangeData;
import piuk.blockchain.android.data.exchange.TradeData;
import piuk.blockchain.android.injection.Injector;

/**
 * Created by justin on 5/1/17.
 */

public class ExchangeService {
    private static ExchangeService instance;
    private static final int METADATA_TYPE_EXCHANGE = 3;

    public static final String TAG = ExchangeService.class.getSimpleName();

    private PayloadManager payloadManager;
    private ReplaySubject<Metadata> metadataSubject;
    private boolean didStartLoad;

    @Inject PayloadDataManager payloadDataManager;

    private ExchangeService() {
        this.payloadManager = PayloadManager.getInstance();
        this.metadataSubject = ReplaySubject.create(1);
        Injector.getInstance().getDataManagerComponent().inject(this);
    }

    public static ExchangeService getInstance() {
        if (instance == null) {
            instance = new ExchangeService();
        }
        return instance;
    }

    public Observable<Metadata> getExchangeData() {
        if (!didStartLoad) {
            reloadExchangeData();
            didStartLoad = true;
        }
        return this.metadataSubject;
    }

    public Observable<String> getPendingTradeAddresses() {
        Log.d(TAG, "getPendingTradeAddresses: called");
        return this.getExchangeData()
                .flatMap(metadata ->
                        Observable.fromCallable(metadata::getMetadata)
                )
                .filter(metadata ->
                        metadata != null
                )
                .flatMapIterable(exchangeData -> {
                    ObjectMapper mapper = new ObjectMapper();
                    ExchangeData data = mapper.readValue(exchangeData, ExchangeData.class);

                    List<TradeData> trades = new ArrayList<TradeData>();
                    if (data.getCoinify() != null) {
                        trades.addAll(data.getCoinify().getTrades());
                    } else if (data.getSfox() != null) {
                        trades.addAll(data.getSfox().getTrades());
                    }

                    return trades;
                })
                .filter(tradeData ->
                        !tradeData.isConfirmed()
                )
                .map(tradeData ->
                        payloadDataManager.getReceiveAddressAtPosition(
                                payloadDataManager.getAccount(tradeData.getAccountIndex()),
                                tradeData.getReceiveIndex()
                        )
                )
                .map(address -> {
                    Log.d(TAG, "getPendingTradeAddresses: found address: " + address);
                    return address;
                })
                .distinct();
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
