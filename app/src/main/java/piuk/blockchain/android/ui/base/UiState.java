package piuk.blockchain.android.ui.base;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class UiState {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LOADING, CONTENT, FAILURE, EMPTY})
    public @interface UiStateDef {
    }

    public static final int LOADING = 0;
    public static final int CONTENT = 1;
    public static final int FAILURE = 2;
    public static final int EMPTY = 3;

}
