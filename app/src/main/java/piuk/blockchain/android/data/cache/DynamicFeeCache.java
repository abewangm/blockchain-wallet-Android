package piuk.blockchain.android.data.cache;

import android.support.annotation.Nullable;

import info.blockchain.wallet.api.data.FeeList;
import info.blockchain.wallet.api.data.FeeOptions;

public class DynamicFeeCache {

    private FeeList feeList;
    private FeeOptions feeOptions;

    public DynamicFeeCache() {
        // No-op
    }

    @Deprecated
    public FeeList getCachedDynamicFee() {
        return feeList;
    }

    @Nullable
    public FeeOptions getFeeOptions() {
        return feeOptions;
    }

    public void setCachedDynamicFee(FeeList feeList) {
        this.feeList = feeList;
    }

    public void setFeeOptions(FeeOptions feeOptions) {
        this.feeOptions = feeOptions;
    }

    public void destroy() {
        feeList = null;
        feeOptions = null;
    }
}
