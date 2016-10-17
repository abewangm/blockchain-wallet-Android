package piuk.blockchain.android.data.send;

public interface OpCallback {
    void onSuccess();

    void onSuccess(String hash);

    void onFail(String error);

    void onFailPermanently(String error);
}