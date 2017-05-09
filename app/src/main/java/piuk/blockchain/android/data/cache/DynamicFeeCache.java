package piuk.blockchain.android.data.cache;

import android.support.annotation.Nullable;

import info.blockchain.wallet.api.data.FeeOptions;

public class DynamicFeeCache {

    private FeeOptions feeOptions;

    public DynamicFeeCache() {
        // No-op
    }

    @Nullable
    public FeeOptions getFeeOptions() {
        return feeOptions;
    }

    public void setFeeOptions(FeeOptions feeOptions) {
        this.feeOptions = feeOptions;
    }

    public void destroy() {
        feeOptions = null;
    }
}
