package info.blockchain.wallet.view;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.ViewUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentBackupCompleteBinding;

public class BackUpWalletCompleteFragment extends Fragment {

    public static final String TAG = BackUpWalletCompleteFragment.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentBackupCompleteBinding dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_backup_complete, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setElevation(ViewUtils.convertDpToPixel(5F, getActivity()));
        }

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
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new BackupWalletStartingFragment())
                    .addToBackStack(BackupWalletStartingFragment.TAG)
                    .commit();
        });

        return dataBinding.getRoot();
    }
}
