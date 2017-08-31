package piuk.blockchain.android.util;

import android.text.Editable;
import android.widget.EditText;

import timber.log.Timber;

public final class EditTextFormatUtil {

    private EditTextFormatUtil() {
        throw new AssertionError("This class isn't intended to be instantiated");
    }

    /**
     * Formats an {@link Editable} in such a way that numbers beyond the maximal decimal length
     * are truncated and sets the cursor position to the end of the selection.
     *
     * @param s                The {@link Editable} provided by the {@link EditText}
     * @param maxLength        The max length allowed after the decimal point
     * @param editText         The {@link EditText} currently being edited
     * @param decimalSeparator The decimal separator being used for this region
     * @return A formatted {@link Editable}
     */
    public static Editable formatEditable(Editable s,
                                          int maxLength,
                                          EditText editText,
                                          String decimalSeparator) {
        try {
            String input = s.toString();
            if (input.contains(decimalSeparator)) {
                return getEditable(s, input, maxLength, editText, input.indexOf(decimalSeparator));
            }
        } catch (NumberFormatException e) {
            Timber.e(e);
        }
        return s;
    }

    private static Editable getEditable(Editable s, String input, int maxLength, EditText editText, int index) {
        String dec = input.substring(index);
        if (!dec.isEmpty()) {
            dec = dec.substring(1);
            if (dec.length() > maxLength) {
                // Substring of pre-decimal + decimal + max length after decimal
                editText.setText(input.substring(0, index + 1 + maxLength));
                editText.setSelection(editText.getText().length());
                s = editText.getEditableText();
            }
        }
        return s;
    }

}
