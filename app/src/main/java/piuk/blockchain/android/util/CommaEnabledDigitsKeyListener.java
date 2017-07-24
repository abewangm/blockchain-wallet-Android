package piuk.blockchain.android.util;

import android.support.annotation.NonNull;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.NumberKeyListener;
import android.view.KeyEvent;

import java.text.DecimalFormatSymbols;

/**
 * A {@link NumberKeyListener} which also allows commas for decimal points. Credit to <a
 * href="https://stackoverflow.com/a/10498873/3245482">stackoverflow.com/a/10498873/3245482</a>
 *
 * This file is intentionally left in Java with Hungarian notation as to make it easier to compare
 * with/maintain against the AOSP implementation of {@link android.text.method.DigitsKeyListener}.
 */
@SuppressWarnings("WeakerAccess")
public class CommaEnabledDigitsKeyListener extends NumberKeyListener {

    private static final char DECIMAL_POINT = DecimalFormatSymbols.getInstance().getDecimalSeparator();

    /**
     * The characters that are used.
     *
     * @see KeyEvent#getMatch
     * @see #getAcceptedChars
     */
    private static final char[][] CHARACTERS = new char[][]{
            new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'},
            new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-'},
            new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', ','},
            new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '.', ','},
    };

    private char[] mAccepted;
    private boolean mSign;
    private boolean mDecimal;

    private static final int SIGN = 1;
    private static final int DECIMAL = 2;

    private static CommaEnabledDigitsKeyListener[] sInstance = new CommaEnabledDigitsKeyListener[4];

    @NonNull
    @Override
    protected char[] getAcceptedChars() {
        return mAccepted;
    }

    private static boolean isSignChar(final char c) {
        return c == '-' || c == '+';
    }

    private static boolean isDecimalPointChar(final char c) {
        return c == DECIMAL_POINT;
    }

    /**
     * Allocates a DigitsKeyListener that accepts the digits 0 through 9.
     */
    public CommaEnabledDigitsKeyListener() {
        this(false, false);
    }

    /**
     * Allocates a DigitsKeyListener that accepts the digits 0 through 9,
     * plus the minus sign (only at the beginning) and/or decimal point
     * (only one per field) if specified.
     */
    public CommaEnabledDigitsKeyListener(boolean sign, boolean decimal) {
        mSign = sign;
        mDecimal = decimal;

        int kind = (sign ? SIGN : 0) | (decimal ? DECIMAL : 0);
        mAccepted = CHARACTERS[kind];
    }

    /**
     * Returns a DigitsKeyListener that accepts the digits 0 through 9.
     */
    public static CommaEnabledDigitsKeyListener getInstance() {
        return getInstance(false, false);
    }

    /**
     * Returns a DigitsKeyListener that accepts the digits 0 through 9,
     * plus the minus sign (only at the beginning) and/or decimal point
     * (only one per field) if specified.
     */
    public static CommaEnabledDigitsKeyListener getInstance(boolean sign, boolean decimal) {
        int kind = (sign ? SIGN : 0) | (decimal ? DECIMAL : 0);

        if (sInstance[kind] != null)
            return sInstance[kind];

        sInstance[kind] = new CommaEnabledDigitsKeyListener(sign, decimal);
        return sInstance[kind];
    }

    /**
     * Returns a DigitsKeyListener that accepts only the characters
     * that appear in the specified String.  Note that not all characters
     * may be available on every keyboard.
     */
    public static CommaEnabledDigitsKeyListener getInstance(String accepted) {
        // TODO: do we need a cache of these to avoid allocating?

        CommaEnabledDigitsKeyListener dim = new CommaEnabledDigitsKeyListener();

        dim.mAccepted = new char[accepted.length()];
        accepted.getChars(0, accepted.length(), dim.mAccepted, 0);

        return dim;
    }

    public int getInputType() {
        int contentType = InputType.TYPE_CLASS_NUMBER;
        if (mSign) {
            contentType |= InputType.TYPE_NUMBER_FLAG_SIGNED;
        }
        if (mDecimal) {
            contentType |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
        }
        return contentType;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {
        CharSequence out = super.filter(source, start, end, dest, dstart, dend);

        if (!mSign && !mDecimal) {
            return out;
        }

        if (out != null) {
            source = out;
            start = 0;
            end = out.length();
        }

        int sign = -1;
        int decimal = -1;
        int dlen = dest.length();

        /*
         * Find out if the existing text has '-' or '.' characters.
         */

        for (int i = 0; i < dstart; i++) {
            char c = dest.charAt(i);

            if (isSignChar(c)) {
                sign = i;
            } else if (isDecimalPointChar(c)) {
                decimal = i;
            }
        }
        for (int i = dend; i < dlen; i++) {
            char c = dest.charAt(i);

            if (isSignChar(c)) {
                return "";    // Nothing can be inserted in front of a '-'.
            } else if (isDecimalPointChar(c)) {
                decimal = i;
            }
        }

        /*
         * If it does, we must strip them out from the source.
         * In addition, '-' must be the very first character,
         * and nothing can be inserted before an existing '-'.
         * Go in reverse order so the offsets are stable.
         */

        SpannableStringBuilder stripped = null;

        for (int i = end - 1; i >= start; i--) {
            char c = source.charAt(i);
            boolean strip = false;

            if (isSignChar(c)) {
                if (i != start || dstart != 0) {
                    strip = true;
                } else if (sign >= 0) {
                    strip = true;
                } else {
                    sign = i;
                }
            } else if (isDecimalPointChar(c)) {
                if (decimal >= 0) {
                    strip = true;
                } else {
                    decimal = i;
                }
            }

            if (strip) {
                if (end == start + 1) {
                    return "";  // Only one character, and it was stripped.
                }

                if (stripped == null) {
                    stripped = new SpannableStringBuilder(source, start, end);
                }

                stripped.delete(i - start, i + 1 - start);
            }
        }

        if (stripped != null) {
            return stripped;
        } else if (out != null) {
            return out;
        } else {
            return null;
        }
    }
}
