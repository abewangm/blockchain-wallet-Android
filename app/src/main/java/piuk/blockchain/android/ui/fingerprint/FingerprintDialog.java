package piuk.blockchain.android.ui.fingerprint;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import info.blockchain.wallet.util.CharSequenceX;

import piuk.blockchain.android.R;
import piuk.blockchain.android.util.annotations.Thunk;

public class FingerprintDialog extends AppCompatDialogFragment {

    public static final String TAG = "FingerprintDialog";
    public static final String KEY_PIN_CODE = "pin_code";
    public static final String KEY_STAGE = "stage";
    private static final long ERROR_TIMEOUT_MILLIS = 1600;
    private static final long SUCCESS_DELAY_MILLIS = 1300;

    private String currentStage = Stage.REGISTER_FINGERPRINT;

    private Button cancelButton;
    @Thunk ImageView fingerprintIcon;
    @Thunk TextView statusTextView;
    @Thunk FingerprintHelper fingerprintHelper;
    private FingerprintAuthCallback authCallback;

    public static FingerprintDialog newInstance(CharSequenceX pin, String stage) {
        Bundle args = new Bundle();
        args.putString(KEY_PIN_CODE, pin.toString());
        args.putString(KEY_STAGE, stage);
        FingerprintDialog fragment = new FingerprintDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: 10/10/2016 Refactor this out
        if (fingerprintHelper == null) {
            throw new RuntimeException("Fingerprint helper is null, have you passed in into the dialog via setFingerprintHelper?");
        }

        if (authCallback == null) {
            throw new RuntimeException("Auth Callback is null, have you passed in into the dialog via setAuthCallback?");
        }
        setStyle(AppCompatDialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
    }

    public void setFingerprintHelper(FingerprintHelper fingerprintHelper) {
        this.fingerprintHelper = fingerprintHelper;
    }

    public void setAuthCallback(FingerprintAuthCallback authCallback) {
        this.authCallback = authCallback;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        getDialog().setTitle(getString(R.string.fingerprint_login_title));
        getDialog().setCancelable(false);
        View view = inflater.inflate(R.layout.dialog_fingerprint, container, false);

        statusTextView = (TextView) view.findViewById(R.id.fingerprint_status);

        cancelButton = (Button) view.findViewById(R.id.action_cancel);
        cancelButton.setOnClickListener(v -> authCallback.onCanceled());

        fingerprintIcon = (ImageView) view.findViewById(R.id.icon_fingerprint);

        if (getArguments() == null
                || getArguments().getString(KEY_PIN_CODE) == null
                || getArguments().getString(KEY_STAGE) == null) {
            dismiss();
        } else {

            // Check stage
            if (getArguments().getString(KEY_STAGE).equals(Stage.REGISTER_FINGERPRINT)) {
                cancelButton.setText(android.R.string.cancel);

                fingerprintHelper.encryptString(
                        FingerprintHelper.KEY_PIN_CODE,
                        getArguments().getString(KEY_PIN_CODE),
                        new FingerprintHelper.AuthCallback() {
                            @Override
                            public void onFailure() {
                                onAuthenticationFailed();
                            }

                            @Override
                            public void onHelp(String message) {
                                onAuthenticationHelp(message);
                            }

                            @Override
                            public void onAuthenticated(@Nullable CharSequenceX data) {
                                onAuthenticationSucceeded();
                            }

                            @Override
                            public void onKeyInvalidated() {
                                // This should never happen at this stage
                            }
                        });
            } else if (getArguments().getString(KEY_STAGE).equals(Stage.AUTHENTICATE)) {
                cancelButton.setText(R.string.fingerprint_use_pin);

                fingerprintHelper.decryptString(
                        FingerprintHelper.KEY_PIN_CODE,
                        getArguments().getString(KEY_PIN_CODE),
                        new FingerprintHelper.AuthCallback() {
                            @Override
                            public void onFailure() {
                                onAuthenticationFailed();
                            }

                            @Override
                            public void onHelp(String message) {
                                onAuthenticationHelp(message);
                            }

                            @Override
                            public void onAuthenticated(@Nullable CharSequenceX data) {
                                fingerprintHelper.storeEncryptedData(KEY_PIN_CODE, data);
                                onAuthenticationSucceeded();
                            }

                            @Override
                            public void onKeyInvalidated() {
                                // This should never happen at this stage
                            }
                        });
            }
        }

        return view;
    }

    public void onAuthenticationHelp(String helpString) {
        showError(helpString);
    }

    public void onAuthenticationFailed() {
        showError(getString(R.string.fingerprint_not_recognized));
    }

    public void onAuthenticationSucceeded() {
        statusTextView.removeCallbacks(mResetErrorTextRunnable);
        fingerprintIcon.setImageResource(R.drawable.ic_fingerprint_success);
        statusTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.blockchain_blue));
        statusTextView.setText(getString(R.string.fingerprint_success));
        fingerprintIcon.postDelayed(() -> authCallback.onAuthenticated(), SUCCESS_DELAY_MILLIS);
    }

    private void showError(CharSequence error) {
        fingerprintIcon.setImageResource(R.drawable.ic_fingerprint_error);
        Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.fingerprint_failed_shake);
        fingerprintIcon.setAnimation(shake);
        fingerprintIcon.animate();
        statusTextView.setText(error);
        statusTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.warning_color));
        statusTextView.removeCallbacks(mResetErrorTextRunnable);
        statusTextView.postDelayed(mResetErrorTextRunnable, ERROR_TIMEOUT_MILLIS);
    }

    private Runnable mResetErrorTextRunnable = new Runnable() {
        @Override
        public void run() {
            statusTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.light_grey_text));
            statusTextView.setText(getString(R.string.fingerprint_hint));
            fingerprintIcon.setImageResource(R.drawable.ic_fingerprint_logo);
        }
    };

    /**
     * Enum to indicate which authentication method the user is trying to authenticate with.
     */
    public static class Stage {
        public static final String REGISTER_FINGERPRINT = "new_fingerprint";
        public static final String AUTHENTICATE = "AUTHENTICATE";
    }

    public interface FingerprintAuthCallback {

        void onAuthenticated();

        void onCanceled();
    }
}
