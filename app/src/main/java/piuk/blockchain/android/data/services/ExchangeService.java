package piuk.blockchain.android.data.services;

import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import info.blockchain.wallet.api.trade.coinify.CoinifyApi;
import info.blockchain.wallet.api.trade.coinify.data.CoinifyTrade;
import info.blockchain.wallet.api.trade.sfox.SFOXApi;
import info.blockchain.wallet.api.trade.sfox.data.SFOXTransaction;
import info.blockchain.wallet.metadata.Metadata;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.MetadataUtil;

import org.bitcoinj.crypto.DeterministicKey;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.subjects.ReplaySubject;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.exchange.ExchangeData;
import piuk.blockchain.android.data.exchange.TradeData;
import piuk.blockchain.android.data.exchange.WebLoginDetails;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.websocket.WebSocketReceiveEvent;
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
    private CoinifyApi coinifyApi;
    private SFOXApi sfoxApi;
    private boolean didStartLoad;

    @Inject PayloadDataManager payloadDataManager;
    @Inject RxBus rxBus;

    private ExchangeService() {
        this.payloadManager = PayloadManager.getInstance();
        this.metadataSubject = ReplaySubject.create(1);
        this.coinifyApi = new CoinifyApi();
        this.sfoxApi = new SFOXApi();
        Injector.getInstance().getDataManagerComponent().inject(this);
    }

    public static ExchangeService getInstance() {
        if (instance == null) {
            instance = new ExchangeService();
        }
        return instance;
    }

    public Observable<WebLoginDetails> getWebLoginDetails() {
        return Observable.zip(
                getExchangeData().flatMap(buyMetadata -> Observable
                        .fromCallable(buyMetadata::getMetadata)
                        .compose(RxUtil.applySchedulersToObservable())
                ),
                getExchangeData().map(Metadata::getMagicHash),
                (walletJson, magicHash) ->
                        new WebLoginDetails(
                                payloadManager.getPayload().toJson(),
                                walletJson == null ? "" : walletJson,
                                magicHash == null ? "" : Hex.toHexString(magicHash),
                                payloadManager.getTempPassword()
                        )
        );
    }

    public Observable<Metadata> getExchangeData() {
        if (!didStartLoad) {
            reloadExchangeData();
            didStartLoad = true;
        }
        return this.metadataSubject;
    }

    public Observable<String> watchPendingTrades() {
        Observable<WebSocketReceiveEvent> receiveEvents = rxBus.register(WebSocketReceiveEvent.class);

        return getPendingTradeAddresses()
                .doOnNext(address ->
                    Log.d(TAG, "watchPendingTrades: watching receive address: " + address)
                )
                .flatMap(address -> receiveEvents
                        .filter(event -> event.getAddress().equals(address))
                        .map(event -> event.getHash())
                );
    }

    public Observable<String> getPendingTradeAddresses() {
        return this.getExchangeData()
                .flatMap(metadata -> Observable
                        .fromCallable(metadata::getMetadata)
                        .compose(RxUtil.applySchedulersToObservable())
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
                        // TODO: This gets the wrong receive address since "position" is relative to current receive index, not absolute. Must fix.
                        payloadDataManager.getReceiveAddressAtPosition(
                                payloadDataManager.getAccount(tradeData.getAccountIndex()),
                                tradeData.getReceiveIndex()
                        )
                )
                .distinct();
    }

    public void reloadExchangeData() {
        Observable<Metadata> exchangeDataStream = getMetadata();
        exchangeDataStream.subscribeWith(metadataSubject);
    }

    private Observable<Metadata> getMetadata() {
        return Observable.fromCallable(() -> {
            DeterministicKey masterKey = this.payloadManager
                    .getPayload()
                    .getHdWallets().get(0)
                    .getMasterKey();
            DeterministicKey metadataHDNode = MetadataUtil.deriveMetadataNode(masterKey);
            return new Metadata.Builder(metadataHDNode, METADATA_TYPE_EXCHANGE).build();
        }).compose(RxUtil.applySchedulersToObservable());
    }

    private Observable<List<CoinifyTrade>> getCoinifyTrades(String accessToken) {
        return coinifyApi.getTrades(accessToken)
                .compose(RxUtil.applySchedulersToObservable());
    }

    public Observable<List<SFOXTransaction>> getSfoxTransactions(String accountToken) {
        return sfoxApi.getTransactions(accountToken)
                .compose(RxUtil.applySchedulersToObservable());
    }
}
