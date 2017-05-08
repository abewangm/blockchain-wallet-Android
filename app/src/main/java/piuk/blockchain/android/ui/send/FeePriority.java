package piuk.blockchain.android.ui.send;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class FeePriority {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FEE_OPTION_REGULAR, FEE_OPTION_PRIORITY})
    public @interface FeePriorityDef {
    }

    public static final int FEE_OPTION_REGULAR = 0;
    public static final int FEE_OPTION_PRIORITY = 1;
}
