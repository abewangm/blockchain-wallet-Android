package piuk.blockchain.android.data.metadata;

import info.blockchain.wallet.payload.PayloadManager;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.data.services.SharedMetaDataService;
import piuk.blockchain.android.injection.Injector;

public class TokenWebStore implements TokenStore {

    @Inject SharedMetaDataService metaDataService;
    @Inject PayloadManager payloadManager;

    {
        Injector.getInstance().getMetaDataComponent().inject(this);
    }

    @Override
    public Observable<String> getToken() {
        DeterministicKey recipientKey = HDKeyDerivation.createMasterPrivateKey(payloadManager.getHDSeed());
        ECKey key = ECKey.fromPrivate(recipientKey.getPrivKey());

        return metaDataService.getToken(key);
    }

}
