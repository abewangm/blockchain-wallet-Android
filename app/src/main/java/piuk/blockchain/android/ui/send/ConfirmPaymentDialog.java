package piuk.blockchain.android.ui.send;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.DialogConfirmTransactionBinding;

public class ConfirmPaymentDialog extends AppCompatDialogFragment {

    private DialogConfirmTransactionBinding binding;

    public static ConfirmPaymentDialog newInstance() {

        Bundle args = new Bundle();

        ConfirmPaymentDialog fragment = new ConfirmPaymentDialog();
        fragment.setArguments(args);
        fragment.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.FullscreenDialog);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_confirm_transaction, container, false);
        binding.getRoot().setFocusableInTouchMode(true);
        binding.getRoot().requestFocus();
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Window window = getDialog().getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(params);
        }
        getDialog().setCancelable(true);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> dismiss());

    }


}
