package piuk.blockchain.android.data.datamanagers;

import android.util.Log;

import info.blockchain.wallet.metadata.Metadata;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.MetadataUtil;

import org.bitcoinj.crypto.DeterministicKey;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by justin on 4/28/17.
 */

public class BuyDataManager {
    public static final String TAG = BuyDataManager.class.getSimpleName();
    private static final int METADATA_TYPE_EXCHANGE = 3;

    private OnboardingDataManager onboardingDataManager;
    private SettingsDataManager settingsDataManager;
    private PayloadDataManager payloadDataManager;

    private PayloadManager payloadManager;
    private PublishSubject<Metadata> metadataSubject;

    public BuyDataManager(OnboardingDataManager onboardingDataManager, SettingsDataManager settingsDataManager, PayloadDataManager payloadDataManager) {
        this.onboardingDataManager = onboardingDataManager;
        this.settingsDataManager = settingsDataManager;
        this.payloadDataManager = payloadDataManager;

        this.payloadManager = PayloadManager.getInstance();
        this.metadataSubject = PublishSubject.create();
    }

    public Observable<Metadata> getExchangeData() {
        return this.metadataSubject;
    }

    public Observable<Boolean> getCanBuy() {
        return Observable.combineLatest(
                this.onboardingDataManager.getIfSepaCountry(),
                this.getIsInvited(),
                (isSepa, isInvited) -> isSepa && isInvited
        );
    }

    private Observable<Boolean> getIsInvited() {
        return this.settingsDataManager.initSettings(
                payloadDataManager.getWallet().getGuid(),
                payloadDataManager.getWallet().getSharedKey()
        ).map(settings -> {
            // TODO: implement settings.invited.sfox
            return true;
        });
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
