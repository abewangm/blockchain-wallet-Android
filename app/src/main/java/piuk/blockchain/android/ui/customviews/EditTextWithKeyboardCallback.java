package piuk.blockchain.android.ui.customviews;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.KeyEvent;

/**
 * This class allows you to intercept keyboard close events and react accordingly.
 */
public class EditTextWithKeyboardCallback extends AppCompatEditText {

    private KeyboardDismissListener listener;

    public EditTextWithKeyboardCallback(Context context) {
        super(context);
    }

    public EditTextWithKeyboardCallback(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextWithKeyboardCallback(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (listener != null) listener.onImeBack();
        }
        return super.dispatchKeyEvent(event);
    }

    public void setOnEditTextImeBackListener(KeyboardDismissListener listener) {
        this.listener = listener;
    }

    public interface KeyboardDismissListener {

        void onImeBack();

    }
}
