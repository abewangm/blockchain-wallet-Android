package piuk.blockchain.android.data.cache;

import info.blockchain.wallet.api.data.FeeList;

public class DynamicFeeCache {

    private static DynamicFeeCache instance;

    private FeeList feeList;

    private DynamicFeeCache() {
        // No-op
    }

    public static DynamicFeeCache getInstance() {
        if (instance == null) {
            instance = new DynamicFeeCache();
        }
        return instance;
    }

    public FeeList getCachedDynamicFee() {
        return feeList;
    }

    public void setCachedDynamicFee(FeeList feeList) {
        this.feeList = feeList;
    }

    public void destroy() {
        instance = null;
    }
}
