package info.blockchain.wallet.view;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import info.blockchain.wallet.payload.PayloadBridge;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.BackupWalletUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.view.customviews.MaterialProgressDialog;
import info.blockchain.wallet.view.helpers.ToastCustom;

import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.annotations.Thunk;
import piuk.blockchain.android.databinding.FragmentBackupWalletVerifyBinding;

public class BackupWalletVerifyFragment extends Fragment {

    @Thunk FragmentBackupWalletVerifyBinding binding;
    @Thunk MaterialProgressDialog mProgressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_backup_wallet_verify, container, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setElevation(0F);
        }

        Bundle bundle = getArguments();
        String secondPassword = null;
        if (bundle != null) {
            secondPassword = bundle.getString("second_password");
        }

        final List<Pair<Integer, String>> confirmSequence = new BackupWalletUtil(getActivity()).getConfirmSequence(secondPassword);
        String[] mnemonicRequestHint = getResources().getStringArray(R.array.mnemonic_word_requests);

        binding.etFirstRequest.setHint(mnemonicRequestHint[confirmSequence.get(0).first]);
        binding.etSecondRequest.setHint(mnemonicRequestHint[confirmSequence.get(1).first]);
        binding.etThirdRequest.setHint(mnemonicRequestHint[confirmSequence.get(2).first]);

        binding.verifyAction.setOnClickListener(v -> {
            if (binding.etFirstRequest.getText().toString().trim().equals(confirmSequence.get(0).second)
                    && binding.etSecondRequest.getText().toString().trim().equalsIgnoreCase(confirmSequence.get(1).second)
                    && binding.etThirdRequest.getText().toString().trim().equalsIgnoreCase(confirmSequence.get(2).second)) {

                showProgressDialog();

                PayloadManager.getInstance().getPayload().getHdWallet().mnemonic_verified(true);
                PayloadBridge.getInstance().remoteSaveThread(new PayloadBridge.PayloadSaveListener() {
                    @Override
                    public void onSaveSuccess() {
                        hideProgressDialog();
                        new PrefsUtil(getActivity()).setValue(BackupWalletActivity.BACKUP_DATE_KEY, (int) (System.currentTimeMillis() / 1000));
                        ToastCustom.makeText(getActivity(), getString(R.string.backup_confirmed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                        popAllAndStartFragment(BackUpWalletCompleteFragment.newInstance(true), BackUpWalletCompleteFragment.TAG);
                    }

                    @Override
                    public void onSaveFail() {
                        hideProgressDialog();
                        ToastCustom.makeText(getActivity(), getActivity().getString(R.string.api_fail), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    }
                });

            } else {
                ToastCustom.makeText(getActivity(), getString(R.string.backup_word_mismatch), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }
        });

        return binding.getRoot();
    }

    @Thunk
    void popAllAndStartFragment(Fragment fragment, String tag) {
        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack(tag)
                .commit();
    }

    private void showProgressDialog() {
        mProgressDialog = new MaterialProgressDialog(getActivity());
        mProgressDialog.setMessage(getString(R.string.please_wait) + "…");
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    @Thunk
    void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}