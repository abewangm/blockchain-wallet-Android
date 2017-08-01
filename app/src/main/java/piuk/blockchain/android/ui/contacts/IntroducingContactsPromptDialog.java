package piuk.blockchain.android.ui.contacts;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import piuk.blockchain.android.R;
import piuk.blockchain.android.util.PrefsUtil;

public class IntroducingContactsPromptDialog extends AppCompatDialogFragment {

    public static final String TAG = IntroducingContactsPromptDialog.class.getSimpleName();

    private static final String KEY_TITLE = "title";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_SUBTITLE = "subtitle";
    private static final String KEY_ICON = "icon";

    private ImageView mIcon1;
    private ImageView mIcon2;
    private ImageView mIcon3;
    private TextView mTitle;
    private TextView mMessage;
    private TextView mSubtitle;
    private Button mDismissButton;

    private View.OnClickListener dismissClickListener;

    public IntroducingContactsPromptDialog() {
        // Empty constructor
    }

    public static IntroducingContactsPromptDialog newInstance() {

        Bundle args = new Bundle();
        args.putInt(KEY_TITLE, R.string.contacts_introducing_title);
        args.putInt(KEY_MESSAGE, R.string.contacts_introducing_message);
        args.putInt(KEY_SUBTITLE, R.string.contacts_introducing_subtitle);
        args.putInt(KEY_ICON, R.drawable.vector_contact_card);

        IntroducingContactsPromptDialog fragment = new IntroducingContactsPromptDialog();
        fragment.setArguments(args);
        return fragment;
    }

    public void showDialog(FragmentManager manager) {
        show(manager, TAG);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_introducing_contacts, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mIcon1 = view.findViewById(R.id.icon1);
        mIcon2 = view.findViewById(R.id.icon2);
        mIcon3 = view.findViewById(R.id.icon3);
        mTitle = view.findViewById(R.id.title);
        mMessage = view.findViewById(R.id.message);
        mSubtitle = view.findViewById(R.id.subtitle);
        mDismissButton = view.findViewById(R.id.dismiss);

        Bundle args = getArguments();
        if (args.containsKey(KEY_TITLE)) {
            setTitle(args.getInt(KEY_TITLE));
        }

        if (args.containsKey(KEY_MESSAGE)) {
            setMessage(args.getInt(KEY_MESSAGE));
        }

        if (args.containsKey(KEY_TITLE)) {
            setTitle(args.getInt(KEY_TITLE));
        }

        if (args.containsKey(KEY_SUBTITLE)) {
            setSubtitle(args.getInt(KEY_SUBTITLE));
        }

        if (args.containsKey(KEY_ICON)) {
            setIcon(args.getInt(KEY_ICON));
        }

        mDismissButton.setOnClickListener(v -> {
            if (dismissClickListener != null) dismissClickListener.onClick(mDismissButton);
        });
    }

    public void setDismissButtonListener(View.OnClickListener click) {
        dismissClickListener = click;
    }

    private void setTitle(@StringRes int titleStringId) {
        mTitle.setText(titleStringId);
    }

    private void setMessage(@StringRes int  message) {
        mMessage.setText(message);
    }

    private void setSubtitle(@StringRes int  message) {
        mSubtitle.setText(message);
    }

    private void setIcon(@DrawableRes int icon) {
        mIcon1.setImageResource(icon);
        mIcon2.setImageResource(icon);
        mIcon3.setImageResource(icon);
    }
}
