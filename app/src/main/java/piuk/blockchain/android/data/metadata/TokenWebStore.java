package piuk.blockchain.android.data.metadata;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.data.services.SharedMetaDataService;
import piuk.blockchain.android.injection.Injector;

@SuppressWarnings("WeakerAccess")
public class TokenWebStore implements TokenStore {

    @Inject SharedMetaDataService metaDataService;

    {
        Injector.getInstance().getMetaDataComponent().inject(this);
    }

    @Override
    public Observable<String> getToken() {
        return metaDataService.getToken();
    }

}
