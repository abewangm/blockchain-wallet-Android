package piuk.blockchain.android.util;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpanFormatter {

    private static final Pattern FORMAT_SEQUENCE =
            Pattern.compile("%([0-9]+\\$|<?)([^a-zA-z%]*)([[a-zA-Z%]&&[^tT]]|[tT][a-zA-Z])");

    private SpanFormatter() {
        throw new AssertionError("This class is not meant to be instantiated");
    }

    public static SpannedString format(CharSequence format, Object... args) {
        return format(Locale.getDefault(), format, args);
    }

    public static SpannedString format(Locale locale, CharSequence format, Object... args) {
        SpannableStringBuilder out = new SpannableStringBuilder(format);

        int i = 0;
        int argAt = -1;

        while (i < out.length()) {
            Matcher m = FORMAT_SEQUENCE.matcher(out);
            if (!m.find(i)) break;
            i = m.start();
            int exprEnd = m.end();

            String argTerm = m.group(1);
            String modTerm = m.group(2);
            String typeTerm = m.group(3);

            CharSequence cookedArg;

            switch (typeTerm) {
                case "%":
                    cookedArg = "%";
                    break;
                case "n":
                    cookedArg = "\n";
                    break;
                default:
                    int argIdx;
                    switch (argTerm) {
                        case "":
                            argIdx = ++argAt;
                            break;
                        case "<":
                            argIdx = argAt;
                            break;
                        default:
                            argIdx = Integer.parseInt(argTerm.substring(0, argTerm.length() - 1)) - 1;
                            break;
                    }

                    Object argItem = args[argIdx];

                    if (typeTerm.equals("s") && argItem instanceof Spanned) {
                        cookedArg = (Spanned) argItem;
                    } else {
                        cookedArg = String.format(locale, "%" + modTerm + typeTerm, argItem);
                    }
                    break;
            }

            out.replace(i, exprEnd, cookedArg);
            i += cookedArg.length();
        }

        return new SpannedString(out);
    }

}