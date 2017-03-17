package piuk.blockchain.android.ui.backup;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentBackupWalletVerifyBinding;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.BackupWalletUtil;
import piuk.blockchain.android.util.annotations.Thunk;

public class BackupWalletVerifyFragment extends Fragment implements BackupVerifyViewModel.DataListener {

    private BackupVerifyViewModel viewModel;
    @Thunk FragmentBackupWalletVerifyBinding binding;
    @Thunk MaterialProgressDialog mProgressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_backup_wallet_verify, container, false);
        viewModel = new BackupVerifyViewModel(this);

        Bundle bundle = getArguments();
        String secondPassword = null;
        if (bundle != null) {
            secondPassword = bundle.getString("second_password");
        }

        final List<Pair<Integer, String>> confirmSequence = new BackupWalletUtil().getConfirmSequence(secondPassword);
        String[] mnemonicRequestHint = getResources().getStringArray(R.array.mnemonic_word_requests);

        binding.etFirstRequest.setHint(mnemonicRequestHint[confirmSequence.get(0).first]);
        binding.etSecondRequest.setHint(mnemonicRequestHint[confirmSequence.get(1).first]);
        binding.etThirdRequest.setHint(mnemonicRequestHint[confirmSequence.get(2).first]);

        binding.verifyAction.setOnClickListener(v -> {
            if (binding.etFirstRequest.getText().toString().trim().equals(confirmSequence.get(0).second)
                    && binding.etSecondRequest.getText().toString().trim().equalsIgnoreCase(confirmSequence.get(1).second)
                    && binding.etThirdRequest.getText().toString().trim().equalsIgnoreCase(confirmSequence.get(2).second)) {

                viewModel.onVerifyClicked();
            } else {
                ToastCustom.makeText(getActivity(), getString(R.string.backup_word_mismatch), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.onViewReady();
    }

    @Thunk
    void popAllAndStartFragment(Fragment fragment, String tag) {
        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack(tag)
                .commit();
    }

    @Override
    public void showProgressDialog() {
        mProgressDialog = new MaterialProgressDialog(getActivity());
        mProgressDialog.setMessage(getString(R.string.please_wait) + "…");
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    @Override
    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void showCompletedFragment() {
        popAllAndStartFragment(BackupWalletCompletedFragment.newInstance(true), BackupWalletCompletedFragment.TAG);
    }

    @Override
    public void showStartingFragment() {
        popAllAndStartFragment(new BackupWalletStartingFragment(), BackupWalletStartingFragment.TAG);
    }

    @Override
    public void showToast(int message, String toastType) {
        ToastCustom.makeText(getContext(), getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.destroy();
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}