package piuk.blockchain.android.ui.backup;

import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.reactivex.disposables.CompositeDisposable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentBackupCompleteBinding;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.annotations.Thunk;

public class BackupWalletCompletedFragment extends Fragment implements BackupWalletViewModel.DataListener {

    public static final String TAG = BackupWalletCompletedFragment.class.getSimpleName();
    private static final String KEY_CHECK_TRANSFER = "check_transfer";

    private CompositeDisposable compositeDisposable;

    @Thunk BackupWalletViewModel viewModel;

    public static BackupWalletCompletedFragment newInstance(boolean checkTransfer) {
        Bundle args = new Bundle();
        args.putBoolean(KEY_CHECK_TRANSFER, checkTransfer);
        BackupWalletCompletedFragment fragment = new BackupWalletCompletedFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentBackupCompleteBinding dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_backup_complete, container, false);
        compositeDisposable = new CompositeDisposable();

        viewModel = new BackupWalletViewModel(this);

        long lastBackup = new PrefsUtil(getActivity()).getValue(BackupWalletActivity.BACKUP_DATE_KEY, 0);

        if (lastBackup != 0) {
            @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
            String date = dateFormat.format(new Date(lastBackup * 1000));
            String message = String.format(getResources().getString(R.string.backup_last), date);

            dataBinding.subheadingDate.setText(message);
        } else {
            dataBinding.subheadingDate.setVisibility(View.GONE);
        }

        dataBinding.buttonBackupAgain.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new BackupWalletStartingFragment())
                    .addToBackStack(BackupWalletStartingFragment.TAG)
                    .commit();
        });

        if (getArguments() != null && getArguments().getBoolean(KEY_CHECK_TRANSFER)) {
            viewModel.checkTransferableFunds();
        }

        return dataBinding.getRoot();
    }

    @Override
    public void showTransferFundsPrompt() {
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.transfer_funds)
                .setMessage(getString(R.string.transfer_recommend))
                .setPositiveButton(R.string.transfer, (dialog, which) -> showTransferFundsConfirmationDialog())
                .setNegativeButton(R.string.not_now, null)
                .create();

        alertDialog.show();

        Button negative = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        negative.setTextColor(ContextCompat.getColor(getActivity(), R.color.primary_gray_dark));
    }

    private void showTransferFundsConfirmationDialog() {
        ConfirmFundsTransferDialogFragment fragment = ConfirmFundsTransferDialogFragment.newInstance();
        fragment.show(getActivity().getSupportFragmentManager(), ConfirmFundsTransferDialogFragment.TAG);
    }

    @Override
    public void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }
}
