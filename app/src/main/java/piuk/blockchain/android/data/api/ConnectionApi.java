package piuk.blockchain.android.data.api;

import javax.inject.Named;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;

public class ConnectionApi {

    private final ConnectionEndpoint connectionEndpoint;

    public ConnectionApi(@Named("server") Retrofit retrofit) {
        connectionEndpoint = retrofit.create(ConnectionEndpoint.class);
    }

    public Observable<ResponseBody> getWebiteConnection() {
        return connectionEndpoint.pingWebsite();
    }

}
