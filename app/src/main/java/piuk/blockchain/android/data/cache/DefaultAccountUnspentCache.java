//package piuk.blockchain.android.data.cache;
//
//import info.blockchain.api.data.UnspentOutputs;
//import org.json.JSONObject;
//import retrofit2.Response;
//
//public class DefaultAccountUnspentCache {
//
//    private static DefaultAccountUnspentCache instance;
//
//    private String xpub;
//    private UnspentOutputs unspentApiResponse;
//
//    private DefaultAccountUnspentCache() {
//        // No-op
//    }
//
//    public static DefaultAccountUnspentCache getInstance() {
//        if (instance == null) {
//            instance = new DefaultAccountUnspentCache();
//        }
//        return instance;
//    }
//
//    public void destroy() {
//        instance = null;
//    }
//
//    public UnspentOutputs getUnspentApiResponse() {
//        return unspentApiResponse;
//    }
//
//    public void setUnspentApiResponse(String xpub, UnspentOutputs unspentApiResponse) {
//        this.xpub = xpub;
//        this.unspentApiResponse = unspentApiResponse;
//    }
//
//    public String getXpub() {
//        return xpub;
//    }
//
//    public void setXpub(String xpub) {
//        this.xpub = xpub;
//    }
//}
//
