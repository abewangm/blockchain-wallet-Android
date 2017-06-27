package piuk.blockchain.android.data.api;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;

public interface ConnectionEndpoint {

    @GET
    Observable<ResponseBody> pingExplorer();

}
