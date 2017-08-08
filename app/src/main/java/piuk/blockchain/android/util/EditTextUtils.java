package piuk.blockchain.android.util;

import android.text.InputFilter;
import android.text.SpannableStringBuilder;

public final class EditTextUtils {

    public EditTextUtils() {
        throw new AssertionError("This class isn't intended to be instantiated");
    }

    /**
     * Returns an {@link InputFilter} that accepts either a '.' or ',' but not both.
     */
    public static InputFilter getDecimalInputFilter() {
        return (source, start, end, dest, dstart, dend) -> {
            if (source instanceof SpannableStringBuilder) {
                SpannableStringBuilder sourceAsSpannableBuilder = (SpannableStringBuilder) source;
                for (int i = end - 1; i >= start; i--) {
                    char currentChar = source.charAt(i);
                    if (!Character.isLetterOrDigit(currentChar) && !Character.isSpaceChar(currentChar)) {
                        sourceAsSpannableBuilder.delete(i, i + 1);
                    }
                }
                return source;
            } else {
                StringBuilder filteredStringBuilder = new StringBuilder();
                for (int i = start; i < end; i++) {
                    char currentChar = source.charAt(i);
                    // Do not allow ',' if already present and '.' being used
                    if (currentChar == ',' && !dest.toString().contains(".") && !dest.toString().contains(",")) {
                        filteredStringBuilder.append(currentChar);
                        // Do not allow '.' if already present and ',' being used
                    } else if (currentChar == '.' && !dest.toString().contains(",") && !dest.toString().contains(".")) {
                        filteredStringBuilder.append(currentChar);
                    } else if (currentChar != ',' && currentChar != '.') {
                        filteredStringBuilder.append(currentChar);
                    }
                }
                return filteredStringBuilder.toString();
            }
        };
    }
}
