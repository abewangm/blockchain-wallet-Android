package piuk.blockchain.android.util;

import android.content.Context;
import android.support.annotation.PluralsRes;
import android.support.annotation.StringRes;

public class StringUtils {

    private Context mContext;

    public StringUtils(Context context) {
        mContext = context;
    }

    public String getString(@StringRes int stringId) {
        return mContext.getString(stringId);
    }

    public String getQuantityString(@PluralsRes int pluralId, int size) {
        return mContext.getResources().getQuantityString(pluralId, size, size);
    }

    public String getFormattedString(@StringRes int stringId, Object... args) {
        return mContext.getResources().getString(stringId, args);
    }
}
