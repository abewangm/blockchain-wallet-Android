package piuk.blockchain.android.util;

import android.os.Build;

public class AndroidUtils {

    public static boolean is21OrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean is23OrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static boolean is25OrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1;
    }
}
