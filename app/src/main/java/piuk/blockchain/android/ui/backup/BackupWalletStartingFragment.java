package piuk.blockchain.android.ui.backup;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import info.blockchain.wallet.payload.PayloadManager;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentBackupStartBinding;
import piuk.blockchain.android.ui.account.SecondPasswordHandler;
import piuk.blockchain.android.util.annotations.Thunk;

import static piuk.blockchain.android.ui.backup.BackupWalletWordListFragment.ARGUMENT_SECOND_PASSWORD;

public class BackupWalletStartingFragment extends Fragment {

    public static final String TAG = BackupWalletStartingFragment.class.getSimpleName();

    private PayloadManager mPayloadManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentBackupStartBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_backup_start, container, false);

        binding.backupWalletAction.setOnClickListener(v -> {

            mPayloadManager = PayloadManager.getInstance();

            // Wallet is double encrypted
            if (mPayloadManager.getPayload().isDoubleEncryption()) {
                new SecondPasswordHandler(getActivity()).validate(new SecondPasswordHandler.ResultListener() {
                    @Override
                    public void onNoSecondPassword() {
                        // No-op
                    }

                    @Override
                    public void onSecondPasswordValidated(String validateSecondPassword) {
                        Fragment fragment = new BackupWalletWordListFragment();
                        Bundle args = new Bundle();
                        args.putString(ARGUMENT_SECOND_PASSWORD, validateSecondPassword);
                        fragment.setArguments(args);
                        loadFragment(fragment);
                    }
                });

            } else {
                loadFragment(new BackupWalletWordListFragment());
            }
        });

        return binding.getRoot();
    }

    @Thunk
    void loadFragment(Fragment fragment) {
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit();
    }

}