package piuk.blockchain.android.data.cache;

import info.blockchain.wallet.api.data.FeesList;

public class DynamicFeeCache {

    private static DynamicFeeCache instance;

    private FeesList suggestedFee;

    private DynamicFeeCache() {
        // No-op
    }

    public static DynamicFeeCache getInstance() {
        if (instance == null) {
            instance = new DynamicFeeCache();
        }
        return instance;
    }

    public FeesList getSuggestedFee() {
        return suggestedFee;
    }

    public void setSuggestedFee(FeesList suggestedFee) {
        this.suggestedFee = suggestedFee;
    }

    public void destroy() {
        instance = null;
    }
}
