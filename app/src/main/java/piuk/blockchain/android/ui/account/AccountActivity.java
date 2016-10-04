package piuk.blockchain.android.ui.account;

import com.google.zxing.BarcodeFormat;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.PrivateKeyFactory;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.params.MainNetParams;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.connectivity.ConnectivityStatus;
import piuk.blockchain.android.databinding.ActivityAccountsBinding;
import piuk.blockchain.android.databinding.AlertPromptTransferFundsBinding;
import piuk.blockchain.android.ui.backup.ConfirmFundsTransferDialogFragment;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.zxing.CaptureActivity;
import piuk.blockchain.android.ui.zxing.Intents;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.DialogButtonCallback;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PermissionUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.ViewUtils;
import piuk.blockchain.android.util.annotations.Thunk;

import static piuk.blockchain.android.ui.account.AccountViewModel.KEY_WARN_TRANSFER_ALL;

public class AccountActivity extends BaseAuthActivity implements AccountViewModel.DataListener {

    private static final int IMPORT_PRIVATE_REQUEST_CODE = 2006;
    private static final int EDIT_ACTIVITY_REQUEST_CODE = 2007;
    private static final int ADDRESS_LABEL_MAX_LENGTH = 17;

    public static String IMPORTED_HEADER;
    public static final String IMPORT_ADDRESS = "import_account";
    public static final String CREATE_NEW = "create_wallet";

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            if (BalanceFragment.ACTION_INTENT.equals(intent.getAction())) {
                runOnUiThread(() -> onUpdateAccountsList());
            }
        }
    };
    private ArrayList<AccountItem> accountsAndImportedList;
    private AccountAdapter accountsAdapter;
    private List<LegacyAddress> legacy;
    private MenuItem transferFundsMenuItem;
    private PrefsUtil prefsUtil;
    private MonetaryUtil monetaryUtil;
    private int hdAccountsIdx;
    @Thunk MaterialProgressDialog progress;
    @Thunk AppUtil appUtil;
    @Thunk PayloadManager payloadManager;
    @Thunk AccountViewModel viewModel;
    @Thunk ActivityAccountsBinding binding;
    @Thunk String secondPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefsUtil = new PrefsUtil(this);
        appUtil = new AppUtil(this);
        payloadManager = PayloadManager.getInstance();
        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));

        binding = DataBindingUtil.setContentView(this, R.layout.activity_accounts);

        viewModel = new AccountViewModel(this);

        setSupportActionBar(binding.toolbarContainer.toolbarGeneral);

        setupViews();
    }

    private void setupViews() {

        IMPORTED_HEADER = getResources().getString(R.string.imported_addresses);

        binding.accountsList.setLayoutManager(new LinearLayoutManager(this));

        accountsAndImportedList = new ArrayList<>();
        onUpdateAccountsList();
        accountsAdapter = new AccountAdapter(accountsAndImportedList, !payloadManager.isNotUpgraded());
        accountsAdapter.setAccountHeaderListener(new AccountAdapter.AccountHeadersListener() {
            @Override
            public void onCreateNewClicked() {
                createNewAccount();
            }

            @Override
            public void onImportAddressClicked() {
                importAddress();
            }

            @Override
            public void onCardClicked(int correctedPosition) {
                onRowClick(correctedPosition);
            }
        });

        binding.accountsList.setAdapter(accountsAdapter);
    }

    @Thunk
    void onRowClick(int position) {
        Intent intent = new Intent(this, AccountEditActivity.class);
        if (position >= hdAccountsIdx) {
            intent.putExtra("address_index", position - hdAccountsIdx);
        } else {
            intent.putExtra("account_index", position);
        }
        startActivityForResult(intent, EDIT_ACTIVITY_REQUEST_CODE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.account_activity_actions, menu);

        transferFundsMenuItem = menu.findItem(R.id.action_transfer_funds);

        viewModel.checkTransferableLegacyFunds(true);//Auto popup

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_transfer_funds:
                showProgressDialog(R.string.please_wait);
                viewModel.checkTransferableLegacyFunds(false);//Not auto popup
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Thunk
    void startScanActivity() {
        if (!appUtil.isCameraOpen()) {
            Intent intent = new Intent(AccountActivity.this, CaptureActivity.class);
            intent.putExtra(Intents.Scan.FORMATS, EnumSet.allOf(BarcodeFormat.class));
            intent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE);
            startActivityForResult(intent, IMPORT_PRIVATE_REQUEST_CODE);
        } else {
            ToastCustom.makeText(AccountActivity.this, getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
    }

    @Thunk
    void importAddress() {
        if (ContextCompat.checkSelfPermission(AccountActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            PermissionUtil.requestCameraPermissionFromActivity(binding.mainLayout, AccountActivity.this);
        } else {
            new SecondPasswordHandler(this).validate(new SecondPasswordHandler.ResultListener() {
                @Override
                public void onNoSecondPassword() {
                    startScanActivity();
                }

                @Override
                public void onSecondPasswordValidated(String validateSecondPassword) {
                    secondPassword = validateSecondPassword;
                    startScanActivity();
                }
            });
        }
    }

    private void createNewAccount() {
        new SecondPasswordHandler(this).validate(new SecondPasswordHandler.ResultListener() {
            @Override
            public void onNoSecondPassword() {
                promptForAccountLabel(null);
            }

            @Override
            public void onSecondPasswordValidated(String validateSecondPassword) {
                secondPassword = validateSecondPassword;
                promptForAccountLabel(validateSecondPassword);
            }
        });
    }

    @Thunk
    void promptForAccountLabel(@Nullable final String validatedSecondPassword) {
        final AppCompatEditText etLabel = new AppCompatEditText(this);
        etLabel.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        etLabel.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH)});

        FrameLayout frameLayout = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int marginInPixels = (int) ViewUtils.convertDpToPixel(20, this);
        params.setMargins(marginInPixels, 0, marginInPixels, 0);
        frameLayout.addView(etLabel, params);

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.label)
                .setMessage(R.string.assign_display_name)
                .setView(frameLayout)
                .setCancelable(false)
                .setPositiveButton(R.string.save_name, (dialog, whichButton) -> {

                    if (etLabel.getText().toString().trim().length() > 0) {
                        addAccount(etLabel.getText().toString().trim(), validatedSecondPassword);
                    } else {
                        ToastCustom.makeText(AccountActivity.this, getResources().getString(R.string.label_cant_be_empty), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void addAccount(final String accountLabel, @Nullable final String secondPassword) {
        if (!ConnectivityStatus.hasConnectivity(AccountActivity.this)) {
            ToastCustom.makeText(AccountActivity.this, getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        } else {
            viewModel.createNewAccount(accountLabel, secondPassword != null ? new CharSequenceX(secondPassword) : null);
        }
    }

    @Override
    public void onUpdateAccountsList() {
        //accountsAndImportedList is linked to AccountAdapter - do not reconstruct or loose reference otherwise notifyDataSetChanged won't work
        accountsAndImportedList.clear();
        int correctedPosition = 0;

        List<Account> accounts = payloadManager.getPayload().getHdWallet().getAccounts();
        List<Account> accountClone = new ArrayList<>(accounts.size());
        accountClone.addAll(accounts);

        if (accountClone.get(accountClone.size() - 1) instanceof ImportedAccount) {
            accountClone.remove(accountClone.size() - 1);
        }

        int defaultIndex = payloadManager.getPayload().getHdWallet().getDefaultIndex();
        Account defaultAccount = payloadManager.getPayload().getHdWallet().getAccounts().get(defaultIndex);

        for (int i = 0; i < accountClone.size(); i++) {
            String label = accountClone.get(i).getLabel();
            String balance = getAccountBalance(i);

            if (label != null && label.length() > ADDRESS_LABEL_MAX_LENGTH)
                label = label.substring(0, ADDRESS_LABEL_MAX_LENGTH) + "...";
            if (label == null || label.length() == 0) label = "";

            accountsAndImportedList.add(new AccountItem(correctedPosition, label, null, balance, getResources().getDrawable(R.drawable.icon_accounthd), accountClone.get(i).isArchived(), false, defaultAccount.getXpub().equals(accountClone.get(i).getXpub())));
            correctedPosition++;
        }

        // Create New button position
        accountsAndImportedList.add(new AccountItem(null, CREATE_NEW, null, "", getResources().getDrawable(R.drawable.icon_accounthd), false, false, false));
        hdAccountsIdx = correctedPosition;

        ImportedAccount iAccount = null;
        if (payloadManager.getPayload().getLegacyAddresses().size() > 0) {
            iAccount = new ImportedAccount(getString(R.string.imported_addresses), payloadManager.getPayload().getLegacyAddresses(), new ArrayList<>(), MultiAddrFactory.getInstance().getLegacyBalance());
        }

        if (iAccount != null) {

            // Imported Header Position
            accountsAndImportedList.add(new AccountItem(null, getString(R.string.imported_addresses), null, "", getResources().getDrawable(R.drawable.icon_accounthd), false, false, false));

            legacy = iAccount.getLegacyAddresses();
            for (int j = 0; j < legacy.size(); j++) {

                String label = legacy.get(j).getLabel();
                String address = legacy.get(j).getAddress();
                String balance = getAddressBalance(j);

                if (label != null && label.length() > ADDRESS_LABEL_MAX_LENGTH)
                    label = label.substring(0, ADDRESS_LABEL_MAX_LENGTH) + "...";
                if (label == null || label.length() == 0) label = "";
                if (address == null || address.length() == 0) address = "";

                accountsAndImportedList.add(new AccountItem(correctedPosition, label, address, balance, getResources().getDrawable(R.drawable.icon_imported), legacy.get(j).getTag() == PayloadManager.ARCHIVED_ADDRESS, legacy.get(j).isWatchOnly(), false));
                correctedPosition++;
            }
        }

        // Import Address button at last position
        accountsAndImportedList.add(new AccountItem(null, IMPORT_ADDRESS, null, "", getResources().getDrawable(R.drawable.icon_accounthd), false, false, false));

        if (accountsAdapter != null) {
            accountsAdapter.notifyDataSetChanged();
            binding.accountsList.setAdapter(accountsAdapter);
        }
    }

    private String getAccountBalance(int index) {

        String address = payloadManager.getXpubFromAccountIndex(index);
        Long amount = MultiAddrFactory.getInstance().getXpubAmounts().get(address);
        if (amount == null) amount = 0L;

        String unit = (String) monetaryUtil.getBTCUnits()[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];

        return monetaryUtil.getDisplayAmount(amount) + " " + unit;
    }

    private String getAddressBalance(int index) {

        String address = legacy.get(index).getAddress();
        Long amount = MultiAddrFactory.getInstance().getLegacyBalance(address);
        String unit = (String) monetaryUtil.getBTCUnits()[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];

        return monetaryUtil.getDisplayAmount(amount) + " " + unit;
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(BalanceFragment.ACTION_INTENT);
        LocalBroadcastManager.getInstance(AccountActivity.this).registerReceiver(receiver, filter);
        onUpdateAccountsList();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(AccountActivity.this).unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == IMPORT_PRIVATE_REQUEST_CODE
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {

            try {
                final String strResult = data.getStringExtra(CaptureActivity.SCAN_RESULT);
                String format = PrivateKeyFactory.getInstance().getFormat(strResult);
                if (format != null) {
                    // Private key scanned
                    if (!format.equals(PrivateKeyFactory.BIP38)) {
                        importNonBIP38Address(format, strResult);
                    } else {
                        importBIP38Address(strResult);
                    }
                } else {
                    // Watch-only address scanned
                    importWatchOnly(strResult);
                }
            } catch (Exception e) {
                ToastCustom.makeText(AccountActivity.this, getString(R.string.privkey_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == EDIT_ACTIVITY_REQUEST_CODE) {
            onUpdateAccountsList();
        }
    }

    private void importBIP38Address(final String data) {

        final AppCompatEditText password = new AppCompatEditText(this);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.bip38_password_entry)
                .setView(ViewUtils.getAlertDialogEditTextLayout(this, password))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {

                    final String pw = password.getText().toString();

                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }
                    progress = new MaterialProgressDialog(AccountActivity.this);
                    progress.setMessage(AccountActivity.this.getResources().getString(R.string.please_wait));
                    progress.show();

                    new Thread(() -> {

                        Looper.prepare();

                        try {
                            BIP38PrivateKey bip38 = new BIP38PrivateKey(MainNetParams.get(), data);
                            final ECKey key = bip38.decrypt(pw);

                            if (key != null && key.hasPrivKey() && payloadManager.getPayload().getLegacyAddressStrings().contains(key.toAddress(MainNetParams.get()).toString())) {

                                //A private key to an existing address has been scanned
                                setPrivateKey(key);

                            } else if (key != null && key.hasPrivKey() && !payloadManager.getPayload().getLegacyAddressStrings().contains(key.toAddress(MainNetParams.get()).toString())) {
                                final LegacyAddress legacyAddress = new LegacyAddress(null, System.currentTimeMillis() / 1000L, key.toAddress(MainNetParams.get()).toString(), "", 0L, "android", BuildConfig.VERSION_NAME);
                                            /*
                                             * if double encrypted, save encrypted in payload
                                             */
                                if (!payloadManager.getPayload().isDoubleEncrypted()) {
                                    legacyAddress.setEncryptedKey(key.getPrivKeyBytes());
                                } else {
                                    String encryptedKey = Base58.encode(key.getPrivKeyBytes());
                                    String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey,
                                            payloadManager.getPayload().getSharedKey(),
                                            secondPassword,
                                            payloadManager.getPayload().getOptions().getIterations());
                                    legacyAddress.setEncryptedKey(encrypted2);
                                }

                                final AppCompatEditText address_label = new AppCompatEditText(AccountActivity.this);
                                address_label.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH)});
                                address_label.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);

                                FrameLayout frameLayout1 = new FrameLayout(AccountActivity.this);
                                FrameLayout.LayoutParams params1 = new FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                int marginInPixels1 = (int) ViewUtils.convertDpToPixel(20, AccountActivity.this);
                                params1.setMargins(marginInPixels1, 0, marginInPixels1, 0);
                                frameLayout1.addView(address_label, params1);

                                new AlertDialog.Builder(AccountActivity.this, R.style.AlertDialogStyle)
                                        .setTitle(R.string.app_name)
                                        .setMessage(R.string.label_address)
                                        .setView(frameLayout1)
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.save_name, (dialog1, whichButton1) -> {
                                            String label = address_label.getText().toString();
                                            if (label.trim().length() > 0) {
                                                legacyAddress.setLabel(label);
                                            } else {
                                                legacyAddress.setLabel(legacyAddress.getAddress());
                                            }

                                            remoteSaveNewAddress(legacyAddress);

                                        }).setNegativeButton(R.string.polite_no, (dialog1, whichButton1) -> {
                                    legacyAddress.setLabel(legacyAddress.getAddress());
                                    remoteSaveNewAddress(legacyAddress);

                                }).show();

                            } else {
                                ToastCustom.makeText(getApplicationContext(), getString(R.string.bip38_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                            }
                        } catch (Exception e) {
                            ToastCustom.makeText(AccountActivity.this, getString(R.string.bip38_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        } finally {
                            if (progress != null && progress.isShowing()) {
                                progress.dismiss();
                                progress = null;
                            }
                        }

                        Looper.loop();

                    }).start();

                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void importNonBIP38Address(final String format, final String data) {

        ECKey key;

        try {
            key = PrivateKeyFactory.getInstance().getKey(format, data);
        } catch (Exception e) {
            ToastCustom.makeText(AccountActivity.this, getString(R.string.no_private_key), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            e.printStackTrace();
            return;
        }

        if (key != null && key.hasPrivKey() && payloadManager.getPayload().getLegacyAddressStrings().contains(key.toAddress(MainNetParams.get()).toString())) {

            // A private key to an existing address has been scanned
            setPrivateKey(key);

        } else if (key != null && key.hasPrivKey() && !payloadManager.getPayload().getLegacyAddressStrings().contains(key.toAddress(MainNetParams.get()).toString())) {

            final LegacyAddress legacyAddress = new LegacyAddress(null, System.currentTimeMillis() / 1000L, key.toAddress(MainNetParams.get()).toString(), "", 0L, "android", BuildConfig.VERSION_NAME);

            // If double encrypted, save encrypted in payload
            if (!payloadManager.getPayload().isDoubleEncrypted()) {
                legacyAddress.setEncryptedKey(key.getPrivKeyBytes());
            } else {
                String encryptedKey = Base58.encode(key.getPrivKeyBytes());
                String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey,
                        payloadManager.getPayload().getSharedKey(),
                        secondPassword,
                        payloadManager.getPayload().getOptions().getIterations());
                legacyAddress.setEncryptedKey(encrypted2);
            }

            showRenameImportedAddressDialog(legacyAddress);

        } else {
            ToastCustom.makeText(AccountActivity.this, getString(R.string.no_private_key), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
    }

    @Thunk
    void setPrivateKey(ECKey key) {
        viewModel.setPrivateECKey(key, secondPassword != null ? new CharSequenceX(secondPassword) : null);
    }

    private void importWatchOnly(String address) {
        viewModel.importWatchOnlyAddress(address);
    }

    @Override
    public void showWatchOnlyWarning(DialogButtonCallback dialogButtonCallback) {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.warning)
                .setCancelable(false)
                .setMessage(getString(R.string.watch_only_import_warning))
                .setPositiveButton(R.string.dialog_continue, (dialog, whichButton) -> dialogButtonCallback.onPositiveClicked())
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialogButtonCallback.onNegativeClicked())
                .show();
    }

    @Override
    public void showRenameImportedAddressDialog(LegacyAddress address) {
        final AppCompatEditText editText = new AppCompatEditText(AccountActivity.this);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH)});
        editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        new AlertDialog.Builder(AccountActivity.this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.label_address)
                .setView(ViewUtils.getAlertDialogEditTextLayout(this, editText))
                .setCancelable(false)
                .setPositiveButton(R.string.save_name, (dialog, whichButton) -> {
                    String label = editText.getText().toString();
                    if (label.trim().length() > 0) {
                        address.setLabel(label);
                    } else {
                        address.setLabel(address.getAddress());
                    }

                    remoteSaveNewAddress(address);

                })
                .setNegativeButton(R.string.polite_no, (dialog, whichButton) -> {
                    address.setLabel(address.getAddress());
                    remoteSaveNewAddress(address);

                }).show();
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void broadcastIntent(Intent intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void remoteSaveNewAddress(final LegacyAddress legacy) {
        if (!ConnectivityStatus.hasConnectivity(AccountActivity.this)) {
            ToastCustom.makeText(AccountActivity.this, getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        } else {
            viewModel.updateLegacyAddress(legacy);
        }
    }

    @Override
    public void onShowTransferableLegacyFundsWarning(boolean isAutoPopup) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.AlertDialogStyle);
        AlertPromptTransferFundsBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.alert_prompt_transfer_funds, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        if (!isAutoPopup) {
            dialogBinding.confirmDontAskAgain.setVisibility(View.GONE);
        }

        dialogBinding.confirmCancel.setOnClickListener(v -> {
            if (dialogBinding.confirmDontAskAgain.isChecked())
                prefsUtil.setValue(KEY_WARN_TRANSFER_ALL, false);
            alertDialog.dismiss();
        });

        dialogBinding.confirmSend.setOnClickListener(v -> {
            if (dialogBinding.confirmDontAskAgain.isChecked())
                prefsUtil.setValue(KEY_WARN_TRANSFER_ALL, false);
            transferSpendableFunds();
            alertDialog.dismiss();
        });

        alertDialog.show();

        // This corrects the layout size after view drawn
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(alertDialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        alertDialog.getWindow().setAttributes(lp);
    }

    private void transferSpendableFunds() {
        ConfirmFundsTransferDialogFragment fragment = ConfirmFundsTransferDialogFragment.newInstance();
        fragment.setOnDismissListener(() -> viewModel.checkTransferableLegacyFunds(false));
        fragment.show(getSupportFragmentManager(), ConfirmFundsTransferDialogFragment.TAG);
    }

    @Override
    public void onSetTransferLegacyFundsMenuItemVisible(boolean visible) {
        runOnUiThread(() -> transferFundsMenuItem.setVisible(visible));
    }

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
    public void showProgressDialog(@StringRes int message) {
        dismissProgressDialog();
        if (!isFinishing()) {
            progress = new MaterialProgressDialog(this);
            progress.setMessage(message);
            progress.setCancelable(false);
            progress.show();
        }
    }

    @Override
    public void dismissProgressDialog() {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }
    }
}
