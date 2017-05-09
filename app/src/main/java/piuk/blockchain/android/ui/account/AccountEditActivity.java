package piuk.blockchain.android.ui.account;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.ImageView;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.connectivity.ConnectivityStatus;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.databinding.ActivityAccountEditBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.send.ConfirmPaymentDialog;
import piuk.blockchain.android.ui.shortcuts.LauncherShortcutHelper;
import piuk.blockchain.android.ui.zxing.CaptureActivity;
import piuk.blockchain.android.util.AndroidUtils;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PermissionUtil;
import piuk.blockchain.android.util.ViewUtils;
import piuk.blockchain.android.util.annotations.Thunk;

public class AccountEditActivity extends BaseAuthActivity implements AccountEditViewModel.DataListener,
        ConfirmPaymentDialog.OnConfirmDialogInteractionListener {

    private static final int ADDRESS_LABEL_MAX_LENGTH = 17;
    private static final int SCAN_PRIVX = 302;

    @Thunk AccountEditViewModel viewModel;
    @Thunk AlertDialog transactionSuccessDialog;
    private ActivityAccountEditBinding binding;
    private MaterialProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_account_edit);
        viewModel = new AccountEditViewModel(new AccountEditModel(this), this);
        binding.setViewModel(viewModel);

        setupToolbar(binding.toolbarContainer.toolbarGeneral, R.string.edit);

        binding.tvTransfer.setOnClickListener(v -> {
            if (viewModel.transferFundsClickable()) {
                new SecondPasswordHandler(this).validate(new SecondPasswordHandler.ResultListener() {
                    @Override
                    public void onNoSecondPassword() {
                        viewModel.onClickTransferFunds();
                    }

                    @Override
                    public void onSecondPasswordValidated(String validateSecondPassword) {
                        viewModel.setSecondPassword(validateSecondPassword);
                        viewModel.onClickTransferFunds();
                    }
                });
            }
        });

        viewModel.onViewReady();
    }

    @Override
    public void promptAccountLabel(@Nullable String label) {
        final AppCompatEditText etLabel = new AppCompatEditText(this);
        etLabel.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etLabel.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH)});
        etLabel.setHint(R.string.name);
        if (label != null && label.length() <= ADDRESS_LABEL_MAX_LENGTH) {
            etLabel.setText(label);
            etLabel.setSelection(label.length());
        }

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.name)
                .setMessage(R.string.assign_display_name)
                .setView(ViewUtils.getAlertDialogPaddedView(this, etLabel))
                .setCancelable(false)
                .setPositiveButton(R.string.save_name, (dialog, whichButton) -> {
                    if (!ConnectivityStatus.hasConnectivity(this)) {
                        onConnectivityLost();
                    } else {
                        viewModel.updateAccountLabel(etLabel.getText().toString());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void onConnectivityLost() {
        ToastCustom.makeText(AccountEditActivity.this, getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String type) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, type);
    }

    @Override
    public void setActivityResult(int resultCode) {
        setResult(resultCode);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void sendBroadcast(String key, String data) {
        Intent intent = new Intent(WebSocketService.ACTION_INTENT);
        intent.putExtra(key, data);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        viewModel.destroy();
        super.onDestroy();
    }

    @Override
    public void startScanActivity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            PermissionUtil.requestCameraPermissionFromActivity(binding.mainLayout, this);
        } else {
            if (!new AppUtil(this).isCameraOpen()) {
                Intent intent = new Intent(this, CaptureActivity.class);
                startActivityForResult(intent, SCAN_PRIVX);
            } else {
                ToastCustom.makeText(this, getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }
        }
    }

    @Override
    public void promptPrivateKey(String message) {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.privx_required)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) ->
                        new SecondPasswordHandler(this).validate(new SecondPasswordHandler.ResultListener() {
                            @Override
                            public void onNoSecondPassword() {
                                startScanActivity();
                            }

                            @Override
                            public void onSecondPasswordValidated(String validateSecondPassword) {
                                viewModel.setSecondPassword(validateSecondPassword);
                                startScanActivity();
                            }
                        }))
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    @Override
    public void showPaymentDetails(PaymentConfirmationDetails details) {
        ConfirmPaymentDialog.newInstance(details, false)
                .show(getSupportFragmentManager(), ConfirmPaymentDialog.class.getSimpleName());

        if (details.isLargeTransaction) {
            binding.getRoot().postDelayed(this::onShowLargeTransactionWarning, 500);
        }
    }

    @Override
    public void onChangeFeeClicked(String feeInBtc, String btcUnit) {
        // No-op
    }

    @Override
    public void onSendClicked() {
        viewModel.submitPayment();
    }

    private void onShowLargeTransactionWarning() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setCancelable(false)
                .setTitle(R.string.warning)
                .setMessage(R.string.large_tx_warning)
                .setPositiveButton(R.string.accept_higher_fee, null)
                .create()
                .show();
    }

    @Override
    public void promptArchive(String title, String message) {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    if (!ConnectivityStatus.hasConnectivity(this)) {
                        onConnectivityLost();
                    } else {
                        viewModel.archiveAccount();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    public void promptBIP38Password(final String data) {
        final AppCompatEditText password = new AppCompatEditText(this);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.bip38_password_entry)
                .setView(ViewUtils.getAlertDialogPaddedView(this, password))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) ->
                        viewModel.importBIP38Address(data, password.getText().toString()))
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    @Override
    public void privateKeyImportMismatch() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(getString(R.string.warning))
                .setMessage(getString(R.string.private_key_successfully_imported) + "\n\n" + getString(R.string.private_key_not_matching_address))
                .setPositiveButton(R.string.try_again, (dialog, whichButton) -> viewModel.onClickScanXpriv(null))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void privateKeyImportSuccess() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.success)
                .setMessage(R.string.private_key_successfully_imported)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    @Override
    public void showXpubSharingWarning() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.warning)
                .setMessage(R.string.xpub_sharing_warning)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_continue, (dialog, whichButton) ->
                        viewModel.showAddressDetails()).setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void showAddressDetails(String heading, String note, String copy, Bitmap bitmap, String qrString) {
        View view = View.inflate(this, R.layout.dialog_view_qr, null);
        ImageView imageView = (ImageView) view.findViewById(R.id.imageview_qr);
        imageView.setImageBitmap(bitmap);

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(heading)
                .setMessage(note)
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(copy, (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip;
                    clip = ClipData.newPlainText("Send address", qrString);
                    ToastCustom.makeText(this,
                            getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                    clipboard.setPrimaryClip(clip);
                })
                .create()
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCAN_PRIVX && resultCode == Activity.RESULT_OK) {
            viewModel.handleIncomingScanIntent(data);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanActivity();
            } else {
                // Permission request was denied.
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void showTransactionSuccess() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        View dialogView = View.inflate(this, R.layout.modal_transaction_success, null);
        transactionSuccessDialog = dialogBuilder.setView(dialogView)
                .setPositiveButton(getString(R.string.done), (dialog, which) -> dialog.dismiss())
                .setOnDismissListener(dialogInterface -> finish())
                .create();

        transactionSuccessDialog.show();

        dialogView.postDelayed(dialogRunnable, 5 * 1000);
    }

    private final Runnable dialogRunnable = new Runnable() {
        @Override
        public void run() {
            if (transactionSuccessDialog != null && transactionSuccessDialog.isShowing()) {
                transactionSuccessDialog.dismiss();
            }
        }
    };

    @Override
    public void showProgressDialog(@StringRes int message) {
        dismissProgressDialog();

        progress = new MaterialProgressDialog(this);
        progress.setMessage(message);
        progress.show();
    }

    @Override
    public void dismissProgressDialog() {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }
    }

    @Override
    public void updateAppShortcuts() {
        if (AndroidUtils.is25orHigher() && viewModel.areLauncherShortcutsEnabled()) {
            LauncherShortcutHelper launcherShortcutHelper = new LauncherShortcutHelper(
                    this,
                    viewModel.getPayloadDataManager(),
                    getSystemService(ShortcutManager.class));

            launcherShortcutHelper.generateReceiveShortcuts();
        }
    }
}
