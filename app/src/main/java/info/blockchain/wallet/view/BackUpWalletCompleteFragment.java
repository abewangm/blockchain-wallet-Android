package info.blockchain.wallet.view;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import info.blockchain.wallet.model.PendingTransaction;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.ViewUtils;
import info.blockchain.wallet.view.helpers.SecondPasswordHandler;
import info.blockchain.wallet.view.helpers.ToastCustom;
import info.blockchain.wallet.view.helpers.TransferFundsHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.annotations.Thunk;
import piuk.blockchain.android.databinding.AlertPromptTransferFundsBinding;
import piuk.blockchain.android.databinding.FragmentBackupCompleteBinding;
import piuk.blockchain.android.databinding.FragmentSendConfirmBinding;
import rx.Subscriber;

public class BackUpWalletCompleteFragment extends Fragment {

    public static final String TAG = BackUpWalletCompleteFragment.class.getSimpleName();
    private static final String KEY_CHECK_TRANSFER = "check_transfer";

    private TransferFundsHelper mFundsHelper;

    public static BackUpWalletCompleteFragment newInstance(boolean checkTransfer) {
        Bundle args = new Bundle();
        args.putBoolean(KEY_CHECK_TRANSFER, checkTransfer);
        BackUpWalletCompleteFragment fragment = new BackUpWalletCompleteFragment();
        fragment.setArguments(args);
        return fragment;
    }

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

        if (getArguments() != null && getArguments().getBoolean(KEY_CHECK_TRANSFER)) {
            mFundsHelper = new TransferFundsHelper(PayloadManager.getInstance());
            mFundsHelper.getTransferableFundTransactionList()
                    .subscribe(pendingTransactions -> {
                        if (!pendingTransactions.isEmpty()) {
                            showTransferFundsDialog(pendingTransactions,
                                    mFundsHelper.getTotalToSend(),
                                    mFundsHelper.getTotalToSend());
                        }
                    }, Throwable::printStackTrace);
        }

        return dataBinding.getRoot();
    }

    private void showTransferFundsDialog(List<PendingTransaction> pendingTransactions, long totalToTransfer, long totalFees) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle);
        AlertPromptTransferFundsBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(getActivity()),
                R.layout.alert_prompt_transfer_funds, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());

        AlertDialog alertDialog = dialogBuilder.create();
        dialogBinding.confirmDontAskAgain.setVisibility(View.GONE);

        dialogBinding.confirmCancel.setOnClickListener(v -> alertDialog.dismiss());

        dialogBinding.confirmSend.setOnClickListener(v -> {
//            transferSpendableFunds(pendingTransactions, totalToTransfer, totalFees);
            alertDialog.dismiss();

            ConfirmFundsTransferDialogFragment fragment = ConfirmFundsTransferDialogFragment.newInstance();
            fragment.show(((AppCompatActivity) getActivity()).getSupportFragmentManager(), ConfirmFundsTransferDialogFragment.TAG);
        });

        alertDialog.show();
    }

    // TODO: 20/09/2016 Move to it's own class
    private void transferSpendableFunds(List<PendingTransaction> pendingTransactionList, long totalBalance, long totalFee) {

        PrefsUtil prefsUtil = new PrefsUtil(getActivity());
        MonetaryUtil monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        FragmentSendConfirmBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(getActivity()),
                R.layout.fragment_send_confirm, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        String fiatUnit = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        String btcUnit = monetaryUtil.getBTCUnit(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        double exchangeRate = ExchangeRateFactory.getInstance().getLastPrice(fiatUnit);

        String fiatAmount = monetaryUtil.getFiatFormat(fiatUnit).format(exchangeRate * ((double) totalBalance / 1e8));
        String fiatFee = monetaryUtil.getFiatFormat(fiatUnit).format(exchangeRate * ((double) totalFee / 1e8));
        String fiatTotal = monetaryUtil.getFiatFormat(fiatUnit).format(exchangeRate * ((double) (totalBalance + totalFee) / 1e8));

        dialogBinding.confirmFromLabel.setText(pendingTransactionList.size() + " " + getResources().getString(R.string.spendable_addresses));
        int defaultIndex = PayloadManager.getInstance().getPayload().getHdWallet().getDefaultIndex();
        Account defaultAccount = PayloadManager.getInstance().getPayload().getHdWallet().getAccounts().get(defaultIndex);
        dialogBinding.confirmToLabel.setText(defaultAccount.getLabel() + " (" + getResources().getString(R.string.default_label) + ")");
        dialogBinding.confirmAmountBtcUnit.setText(btcUnit);
        dialogBinding.confirmAmountFiatUnit.setText(fiatUnit);
        dialogBinding.confirmAmountBtc.setText(monetaryUtil.getDisplayAmount(totalBalance));
        dialogBinding.confirmAmountFiat.setText(fiatAmount);
        dialogBinding.confirmFeeBtc.setText(monetaryUtil.getDisplayAmount(totalFee));
        dialogBinding.confirmFeeFiat.setText(fiatFee);
        dialogBinding.confirmTotalBtc.setText(monetaryUtil.getDisplayAmount(totalBalance + totalFee));
        dialogBinding.confirmTotalFiat.setText(fiatTotal);

        dialogBinding.tvCustomizeFee.setVisibility(View.GONE);

        dialogBinding.confirmCancel.setOnClickListener(v -> {
            if (alertDialog.isShowing()) {
                alertDialog.cancel();
            }
        });

        dialogBinding.confirmSend.setOnClickListener(v -> {
            new SecondPasswordHandler(getActivity()).validate(new SecondPasswordHandler.ResultListener() {
                @Override
                public void onNoSecondPassword() {
                    sendPayment(pendingTransactionList, null);
                }

                @Override
                public void onSecondPasswordValidated(String validateSecondPassword) {
                    sendPayment(pendingTransactionList, validateSecondPassword);
                }
            });

            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    @Thunk
    void sendPayment(List<PendingTransaction> pendingTransactions, @Nullable String secondPassword) {
        mFundsHelper.sendPayment(pendingTransactions, secondPassword)
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        ToastCustom.makeText(getActivity(), getString(R.string.transfer_confirmed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastCustom.makeText(getActivity(), getString(R.string.unexpected_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    }

                    @Override
                    public void onNext(String s) {
                        // Emits Tx hash - don't need to do anything with this
                    }
                });
    }
}
