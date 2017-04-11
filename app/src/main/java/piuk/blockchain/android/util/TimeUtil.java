package piuk.blockchain.android.util;

public final class TimeUtil {

    public static final long HOURS_24 = 24 * 60 * 60 * 1000;

    /**
     * Returns true if the timestamp + the time you specified is in the past
     *
     * @param timestamp   The timestamp you wish to check
     * @param elapsedTime The time elapsed since the timestamp you wish to check against
     * @return A boolean value
     */
    public static boolean getIfTimeElapsed(long timestamp, long elapsedTime) {
        return System.currentTimeMillis() - (timestamp + elapsedTime) > 0;
    }

}
