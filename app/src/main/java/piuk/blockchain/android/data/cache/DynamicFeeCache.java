package piuk.blockchain.android.data.cache;

import info.blockchain.wallet.api.data.FeeList;

public class DynamicFeeCache {

    private FeeList feeList;

    public DynamicFeeCache() {
        // No-op
    }

    public FeeList getCachedDynamicFee() {
        return feeList;
    }

    public void setCachedDynamicFee(FeeList feeList) {
        this.feeList = feeList;
    }

    public void destroy() {
        feeList = null;
    }
}
