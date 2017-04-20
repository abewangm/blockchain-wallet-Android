package piuk.blockchain.android.util;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class MonetaryUtil {

    public static final int UNIT_BTC = 0;
    public static final int MILLI_BTC = 1;
    public static final int MICRO_BTC = 2;

    private static final double MILLI_DOUBLE = 1000.0;
    private static final double MICRO_DOUBLE = 1000000.0;
    private static final long MILLI_LONG = 1000L;
    private static final long MICRO_LONG = 1000000L;
    private static final double BTC_DEC = 1e8;
    private static final CharSequence[] BTC_UNITS = {"BTC", "mBTC", "bits"};

    private DecimalFormat btcFormat;
    private DecimalFormat fiatFormat;
    private int unit;

    public MonetaryUtil(int unit) {
        this.unit = unit;

        fiatFormat = (DecimalFormat) NumberFormat.getInstance(Locale.getDefault());
        fiatFormat.setMaximumFractionDigits(2);
        fiatFormat.setMinimumFractionDigits(2);

        btcFormat = (DecimalFormat) NumberFormat.getInstance(Locale.getDefault());
        btcFormat.setMinimumFractionDigits(1);
        switch (unit) {
            case MonetaryUtil.MICRO_BTC:
                btcFormat.setMaximumFractionDigits(2);
                break;
            case MonetaryUtil.MILLI_BTC:
                btcFormat.setMaximumFractionDigits(5);
                break;
            default:
                btcFormat.setMaximumFractionDigits(8);
                break;
        }
    }

    public void updateUnit(int unit) {
        this.unit = unit;
    }

    public NumberFormat getBTCFormat() {
        return btcFormat;
    }

    public NumberFormat getFiatFormat(String fiat) {
        fiatFormat.setCurrency(Currency.getInstance(fiat));
        return fiatFormat;
    }

    public CharSequence[] getBTCUnits() {
        return BTC_UNITS.clone();
    }

    public String getBTCUnit(int unit) {
        return (String) BTC_UNITS[unit];
    }

    public String getDisplayAmount(long value) {
        switch (unit) {
            case MonetaryUtil.MICRO_BTC:
                return Double.toString((double) (value * MICRO_LONG) / BTC_DEC);
            case MonetaryUtil.MILLI_BTC:
                return Double.toString((double) (value * MILLI_LONG) / BTC_DEC);
            default:
                return getBTCFormat().format(value / BTC_DEC);
        }
    }

    public BigInteger getUndenominatedAmount(long value) {
        switch (unit) {
            case MonetaryUtil.MICRO_BTC:
                return BigInteger.valueOf(value / MICRO_LONG);
            case MonetaryUtil.MILLI_BTC:
                return BigInteger.valueOf(value / MILLI_LONG);
            default:
                return BigInteger.valueOf(value);
        }
    }

    public double getUndenominatedAmount(double value) {
        switch (unit) {
            case MonetaryUtil.MICRO_BTC:
                return value / MICRO_DOUBLE;
            case MonetaryUtil.MILLI_BTC:
                return value / MILLI_DOUBLE;
            default:
                return value;
        }
    }

    public double getDenominatedAmount(double value) {
        switch (unit) {
            case MonetaryUtil.MICRO_BTC:
                return value * MICRO_DOUBLE;
            case MonetaryUtil.MILLI_BTC:
                return value * MILLI_DOUBLE;
            default:
                return value;
        }
    }

    public String getDisplayAmountWithFormatting(long value) {
        DecimalFormat df = new DecimalFormat("#");
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(8);

        switch (unit) {
            case MonetaryUtil.MICRO_BTC:
                return df.format((double) (value * MICRO_LONG) / BTC_DEC);
            case MonetaryUtil.MILLI_BTC:
                return df.format((double) (value * MILLI_LONG) / BTC_DEC);
            default:
                return getBTCFormat().format(value / BTC_DEC);
        }
    }

    public String getDisplayAmountWithFormatting(double value) {
        DecimalFormat df = new DecimalFormat("#");
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(8);

        switch (unit) {
            case MonetaryUtil.MICRO_BTC:
                return df.format(value * MICRO_DOUBLE / BTC_DEC);
            case MonetaryUtil.MILLI_BTC:
                return df.format(value * MILLI_DOUBLE / BTC_DEC);
            default:
                return getBTCFormat().format(value / BTC_DEC);
        }
    }

}
