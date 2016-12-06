package piuk.blockchain.android.data.metadata;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.data.services.SharedMetadataService;
import piuk.blockchain.android.injection.Injector;

@SuppressWarnings("WeakerAccess")
public class TokenWebStore implements TokenStore {

    @Inject SharedMetadataService metaDataService;

    {
        Injector.getInstance().getMetaDataComponent().inject(this);
    }

    @Override
    public Observable<String> getToken() {
        return metaDataService.getToken();
    }

}
