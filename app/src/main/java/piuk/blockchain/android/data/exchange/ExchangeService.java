package piuk.blockchain.android.data.exchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import info.blockchain.wallet.metadata.Metadata;
import info.blockchain.wallet.metadata.MetadataNodeFactory;
import info.blockchain.wallet.payload.PayloadManager;

import org.bitcoinj.crypto.DeterministicKey;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.subjects.ReplaySubject;
import piuk.blockchain.android.data.exchange.models.ExchangeData;
import piuk.blockchain.android.data.exchange.models.TradeData;
import piuk.blockchain.android.data.exchange.models.WebViewLoginDetails;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.websocket.WebSocketReceiveEvent;
import timber.log.Timber;

/**
 * Created by justin on 5/1/17.
 */

public class ExchangeService {

    private static final int METADATA_TYPE_EXCHANGE = 3;

    private PayloadManager payloadManager;
    private RxBus rxBus;

    private ReplaySubject<Metadata> metadataSubject;
    private boolean didStartLoad;

    public ExchangeService(PayloadManager payloadManager,
                           RxBus rxBus) {
        this.payloadManager = payloadManager;
        this.rxBus = rxBus;

        metadataSubject = ReplaySubject.create(1);
    }

    void wipe() {
        metadataSubject = ReplaySubject.create(1);
        didStartLoad = false;
    }

    Observable<WebViewLoginDetails> getWebViewLoginDetails() {
        return Observable.zip(
                getExchangeData().flatMap(buyMetadata -> Observable
                        .fromCallable(() -> {
                            String metadata = buyMetadata.getMetadata();
                            return metadata == null ? "" : metadata;
                        })
                        .compose(RxUtil.applySchedulersToObservable())
                ),
                getExchangeData().flatMap(buyMetadata -> Observable
                        .fromCallable(() -> {
                            buyMetadata.fetchMagic();
                            byte[] magicHash = buyMetadata.getMagicHash();
                            return magicHash == null ? "" : Hex.toHexString(magicHash);
                        })
                        .compose(RxUtil.applySchedulersToObservable())
                ),
                (externalJson, magicHash) -> {
                    String walletJson = payloadManager.getPayload().toJson();
                    String password = payloadManager.getTempPassword();
                    return new WebViewLoginDetails(walletJson, password, externalJson, magicHash);
                }
        );
    }

    private Observable<Metadata> getExchangeData() {
        if (!didStartLoad) {
            reloadExchangeData();
            didStartLoad = true;
        }
        return metadataSubject;
    }

    Observable<String> watchPendingTrades() {
        Observable<WebSocketReceiveEvent> receiveEvents = rxBus.register(WebSocketReceiveEvent.class);

        return getPendingTradeAddresses()
                .doOnNext(address ->
                        Timber.d("watchPendingTrades: watching receive address: " + address))
                .flatMap(address -> receiveEvents
                        .filter(event -> event.getAddress().equals(address))
                        .map(WebSocketReceiveEvent::getHash));
    }

    private Observable<String> getPendingTradeAddresses() {
        return getExchangeData()
                .flatMap(metadata -> Observable
                        .fromCallable(() -> {
                            String exchangeData = metadata.getMetadata();
                            return exchangeData == null ? "" : exchangeData;
                        })
                        .compose(RxUtil.applySchedulersToObservable()))
                .flatMapIterable(exchangeData -> {

                    if (exchangeData.isEmpty()) return new ArrayList<>();

                    ObjectMapper mapper = new ObjectMapper();
                    ExchangeData data = mapper.readValue(exchangeData, ExchangeData.class);

                    List<TradeData> trades = new ArrayList<>();
                    if (data.getCoinify() != null) {
                        trades.addAll(data.getCoinify().getTrades());
                    } else if (data.getSfox() != null) {
                        trades.addAll(data.getSfox().getTrades());
                    } else if (data.getUnocoin() != null) {
                        trades.addAll(data.getUnocoin().getTrades());
                    }

                    return trades;
                })
                .filter(tradeData -> tradeData.isBuy() && !tradeData.isConfirmed())
                .map(tradeData ->
                        payloadManager.getReceiveAddressAtArbitraryPosition(
                                payloadManager.getPayload().getHdWallets().get(0).getAccount(tradeData.getAccountIndex()),
                                tradeData.getReceiveIndex()))
                .distinct();
    }

    void reloadExchangeData() {
        MetadataNodeFactory metadataNodeFactory = payloadManager.getMetadataNodeFactory();

        if (metadataNodeFactory != null) {
            DeterministicKey metadataNode = metadataNodeFactory.getMetadataNode();

            if (metadataNode != null) {
                Observable<Metadata> exchangeDataStream = getMetadata(metadataNode);
                exchangeDataStream.subscribeWith(metadataSubject);
            } else {
                Timber.e("MetadataNode not generated yet. Wallet possibly double encrypted.");
            }
        } else {
            //PayloadManager likely to have been cleared at this point.
            //TODO This avoids high velocity crash. A proper analyses why this happens would be beneficial.
            Timber.e("ExchangeService.reloadExchangeData - MetadataNodeFactory is null.");
        }
    }

    private Observable<Metadata> getMetadata(DeterministicKey metadataHDNode) {
        return Observable.fromCallable(() ->
                new Metadata.Builder(metadataHDNode, METADATA_TYPE_EXCHANGE).build()
        ).compose(RxUtil.applySchedulersToObservable());
    }

    Observable<ExchangeData> getExchangeMetaData() {
        return getExchangeData()
                .flatMap(metadata -> Observable
                        .fromCallable(() -> {
                            String exchangeData = metadata.getMetadata();
                            return exchangeData == null ? "" : exchangeData;
                        })
                        .compose(RxUtil.applySchedulersToObservable()))
                .map(exchangeData -> {

                    exchangeData = "{\n" +
                            "  \"coinify\": {\n" +
                            "    \"user\": 1137,\n" +
                            "    \"offline_token\": \"uCUevN5R/7yEBFP90ME3R6+1pcJurI2z/ZTN5dt6uP1RryRLtOD6DaGzIqK9FyGT\",\n" +
                            "    \"auto_login\": true,\n" +
                            "    \"trades\": [\n" +
                            "      {\n" +
                            "        \"id\": 2559,\n" +
                            "        \"state\": \"completed\",\n" +
                            "        \"tx_hash\": \"f02c6c063bcb066f09ee317f0e1895d583cc7c41e66dd9a83d52078695351188\",\n" +
                            "        \"confirmed\": true,\n" +
                            "        \"is_buy\": true,\n" +
                            "        \"account_index\": 1,\n" +
                            "        \"receive_index\": 40\n" +
                            "      },\n" +
                            "      {\n" +
                            "        \"id\": 6411,\n" +
                            "        \"state\": \"completed\",\n" +
                            "        \"tx_hash\": \"39c49510083f1a02b60fc26131d553a489b356b2f882ff037e0ab0d8c5f59cf0\",\n" +
                            "        \"confirmed\": true,\n" +
                            "        \"is_buy\": true,\n" +
                            "        \"account_index\": 1,\n" +
                            "        \"receive_index\": 55\n" +
                            "      }\n" +
                            "    ]\n" +
                            "  },\n" +
                            "  \"sfox\": {\n" +
                            "    \"has_seen\": false,\n" +
                            "    \"auto_login\": true,\n" +
                            "    \"user\": \"riri\",\n" +
                            "    \"trades\": []\n" +
                            "  },\n" +
                            "  \"unocoin\": {\n" +
                            "    \"auto_login\": true,\n" +
                            "    \"trades\": []\n" +
                            "  }\n" +
                            "}";


                    if (exchangeData.isEmpty()) return new ExchangeData();

                    ObjectMapper mapper = new ObjectMapper();
                    ExchangeData data = mapper.readValue(exchangeData, ExchangeData.class);

                    return data;
                });
    }

}
