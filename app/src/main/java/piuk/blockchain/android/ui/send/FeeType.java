package piuk.blockchain.android.ui.send;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

class FeeType {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FEE_OPTION_REGULAR, FEE_OPTION_PRIORITY, FEE_OPTION_CUSTOM})
    @interface FeePriorityDef {
    }

    static final int FEE_OPTION_REGULAR = 0;
    static final int FEE_OPTION_PRIORITY = 1;
    static final int FEE_OPTION_CUSTOM = 2;

}
