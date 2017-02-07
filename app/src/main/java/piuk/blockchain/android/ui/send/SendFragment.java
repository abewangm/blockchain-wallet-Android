package piuk.blockchain.android.ui.send;

import com.google.gson.Gson;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RelativeLayout;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.LegacyAddress;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.connectivity.ConnectivityStatus;
import piuk.blockchain.android.data.contacts.PaymentRequestType;
import piuk.blockchain.android.databinding.AlertWatchOnlySpendBinding;
import piuk.blockchain.android.databinding.FragmentSendBinding;
import piuk.blockchain.android.databinding.FragmentSendConfirmBinding;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails;
import piuk.blockchain.android.ui.account.SecondPasswordHandler;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.chooser.AccountChooserActivity;
import piuk.blockchain.android.ui.customviews.CustomKeypad;
import piuk.blockchain.android.ui.customviews.CustomKeypadCallback;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.home.MainActivity;
import piuk.blockchain.android.ui.zxing.CaptureActivity;
import piuk.blockchain.android.util.AppRate;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.EventLogHandler;
import piuk.blockchain.android.util.PermissionUtil;
import piuk.blockchain.android.util.ViewUtils;
import piuk.blockchain.android.util.annotations.Thunk;

import static android.databinding.DataBindingUtil.inflate;
import static piuk.blockchain.android.ui.chooser.AccountChooserActivity.EXTRA_SELECTED_ITEM;
import static piuk.blockchain.android.ui.chooser.AccountChooserActivity.EXTRA_SELECTED_OBJECT_TYPE;


public class SendFragment extends Fragment implements SendContract.DataListener, CustomKeypadCallback {

    private static final String TAG = SendFragment.class.getSimpleName();

    public static final String ARGUMENT_SCAN_DATA = "scan_data";
    public static final String ARGUMENT_SELECTED_ACCOUNT_POSITION = "selected_account_position";
    public static final String ARGUMENT_CONTACT_ID = "contact_id";
    public static final String ARGUMENT_CONTACT_MDID = "contact_mdid";
    public static final String ARGUMENT_FCTX_ID = "fctx_id";
    public static final String ARGUMENT_SCAN_DATA_ADDRESS_INPUT_ROUTE = "address_input_route";

    private static final int SCAN_URI = 2007;
    private static final int SCAN_PRIVX = 2008;
    private static final int COOL_DOWN_MILLIS = 2 * 1000;

    @Thunk FragmentSendBinding binding;
    @Thunk SendViewModel viewModel;
    @Thunk AlertDialog transactionSuccessDialog;
    @Thunk boolean textChangeAllowed = true;
    private OnSendFragmentInteractionListener listener;
    private CustomKeypad customKeypad;
    private MaterialProgressDialog progressDialog;

    private int selectedAccountPosition = -1;
    private long backPressed;
    private final Handler dialogHandler = new Handler();
    private final Runnable dialogRunnable = new Runnable() {
        @Override
        public void run() {
            if (transactionSuccessDialog != null && transactionSuccessDialog.isShowing()) {
                transactionSuccessDialog.dismiss();
            }
        }
    };

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction().equals(BalanceFragment.ACTION_INTENT) && binding != null) {
                viewModel.onViewReady();
                viewModel.updateUI();
            }
        }
    };

    public SendFragment() {
        // Required empty public constructor
    }

    public static SendFragment newInstance(@Nullable String scanData,
                                           @Nullable String contactId,
                                           @Nullable String mdid,
                                           @Nullable String fctxId,
                                           String scanRoute,
                                           int selectedAccountPosition) {
        SendFragment fragment = new SendFragment();
        Bundle args = new Bundle();
        args.putString(ARGUMENT_SCAN_DATA, scanData);
        args.putString(ARGUMENT_CONTACT_ID, contactId);
        args.putString(ARGUMENT_CONTACT_MDID, mdid);
        args.putString(ARGUMENT_FCTX_ID, fctxId);
        args.putString(ARGUMENT_SCAN_DATA_ADDRESS_INPUT_ROUTE, scanRoute);
        args.putInt(ARGUMENT_SELECTED_ACCOUNT_POSITION, selectedAccountPosition);
        fragment.setArguments(args);
        return fragment;
    }

    public static SendFragment newInstance(@Nullable String scanData,
                                           String scanRoute,
                                           int selectedAccountPosition) {
        SendFragment fragment = new SendFragment();
        Bundle args = new Bundle();
        args.putString(ARGUMENT_SCAN_DATA, scanData);
        args.putString(ARGUMENT_SCAN_DATA_ADDRESS_INPUT_ROUTE, scanRoute);
        args.putInt(ARGUMENT_SELECTED_ACCOUNT_POSITION, selectedAccountPosition);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getArguments() != null) {
            selectedAccountPosition = getArguments().getInt(ARGUMENT_SELECTED_ACCOUNT_POSITION);
        }

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_send, container, false);
        viewModel = new SendViewModel(this, Locale.getDefault());

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setCustomKeypad();
        setupViews();
        setHasOptionsMenu(true);
        viewModel.onViewReady();

        return binding.getRoot();
    }

    @Override
    public Bundle getFragmentBundle() {
        return getArguments();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupToolbar();
        IntentFilter filter = new IntentFilter(BalanceFragment.ACTION_INTENT);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);
        viewModel.updateUI();

        if (listener != null) listener.onSendFragmentStart();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        super.onPause();
    }

    private void setupToolbar() {
        ActionBar supportActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setTitle(R.string.send_bitcoin);

            AppBarLayout appBarLayout = (AppBarLayout) getActivity().findViewById(R.id.appbar_layout);
            if (appBarLayout != null) {
                ViewUtils.setElevation(
                        getActivity().findViewById(R.id.appbar_layout),
                        ViewUtils.convertDpToPixel(5F, getContext()));
            }
        } else {
            finishPage();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.send_activity_actions, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Main activity QR button
        final MenuItem menuItem = menu.findItem(R.id.action_qr_main);
        if (menuItem != null) {
            menuItem.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_qr:
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    PermissionUtil.requestCameraPermissionFromFragment(binding.getRoot(), this);
                } else {
                    startScanActivity(SCAN_URI);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startScanActivity(int code) {
        if (!new AppUtil(getActivity()).isCameraOpen()) {
            Intent intent = new Intent(getActivity(), CaptureActivity.class);
            startActivityForResult(intent, code);
        } else {
            showToast(R.string.camera_unavailable, ToastCustom.TYPE_ERROR);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_URI
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {

            viewModel.handleIncomingQRScan(data.getStringExtra(CaptureActivity.SCAN_RESULT),
                    EventLogHandler.URL_EVENT_TX_INPUT_FROM_QR);

        } else if (requestCode == SCAN_PRIVX && resultCode == Activity.RESULT_OK) {
            final String scanData = data.getStringExtra(CaptureActivity.SCAN_RESULT);
            viewModel.handleScannedDataForWatchOnlySpend(scanData);

            // Set Receiving account
        } else if (resultCode == Activity.RESULT_OK
                && requestCode == AccountChooserActivity.REQUEST_CODE_CHOOSE_ACCOUNT_RECEIVE
                && data != null) {

            try {
                Class type = Class.forName(data.getStringExtra(EXTRA_SELECTED_OBJECT_TYPE));
                Object object = new Gson().fromJson(data.getStringExtra(EXTRA_SELECTED_ITEM), type);

                if (object instanceof Contact) {
                    viewModel.setContact(((Contact) object));
                } else if (object instanceof Account) {
                    Account account = ((Account) object);
                    viewModel.setReceivingAddress(new ItemAccount(account.getLabel(), null, null, null, account));
                    binding.destination.setText(account.getLabel());
                } else if (object instanceof LegacyAddress) {
                    LegacyAddress legacyAddress = ((LegacyAddress) object);
                    viewModel.setReceivingAddress(new ItemAccount(legacyAddress.getLabel(), null, null, null, legacyAddress));
                    binding.destination.setText(legacyAddress.getLabel());
                }

            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            // Set Sending account
        } else if (resultCode == Activity.RESULT_OK
                && requestCode == AccountChooserActivity.REQUEST_CODE_CHOOSE_ACCOUNT_SEND
                && data != null) {

            try {
                Class type = Class.forName(data.getStringExtra(EXTRA_SELECTED_OBJECT_TYPE));
                Object object = new Gson().fromJson(data.getStringExtra(EXTRA_SELECTED_ITEM), type);

                ItemAccount chosenItem = null;
                if (object instanceof Account) {
                    Account account = ((Account) object);
                    chosenItem = new ItemAccount(account.getLabel(), null, null, null, account);
                } else if (object instanceof LegacyAddress) {
                    LegacyAddress legacyAddress = ((LegacyAddress) object);
                    chosenItem = new ItemAccount(legacyAddress.getLabel(), null, null, null, legacyAddress);
                }

                viewModel.setSendingAddress(chosenItem);
                binding.from.setText(chosenItem.label);

                viewModel.calculateTransactionAmounts(chosenItem,
                        binding.amountRow.amountBtc.getText().toString(),
                        binding.customFee.getText().toString(), null);

            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean isKeyboardVisible() {
        return customKeypad.isVisible();
    }

    public void onBackPressed() {
        if (isKeyboardVisible()) {
            closeKeypad();
        } else {
            handleBackPressed();
        }
    }

    public void handleBackPressed() {
        if (backPressed + COOL_DOWN_MILLIS > System.currentTimeMillis()) {
            AccessState.getInstance().logout(getContext());
            return;
        } else {
            onExitConfirmToast();
        }

        backPressed = System.currentTimeMillis();
    }

    public void onExitConfirmToast() {
        showToast(R.string.exit_confirm, ToastCustom.TYPE_GENERAL);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanActivity(SCAN_URI);
            } else {
                // Permission request was denied.
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void closeKeypad() {
        customKeypad.setNumpadVisibility(View.GONE);
    }

    @Override
    public void onKeypadClose() {
        // Show bottom nav if applicable
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).getBottomNavigationView().restoreBottomNavigation();
        }
        // Resize activity back to initial state
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        layoutParams.addRule(RelativeLayout.ABOVE, 0);
        binding.scrollView.setLayoutParams(layoutParams);
    }

    @Override
    public void onKeypadOpen() {
        // Hide bottom nav if applicable
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).getBottomNavigationView().hideBottomNavigation();
        }
    }

    @Override
    public void onKeypadOpenCompleted() {
        // Resize activity around keyboard view
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        layoutParams.addRule(RelativeLayout.ABOVE, R.id.keyboard);

        binding.scrollView.setLayoutParams(layoutParams);
    }

    private void setCustomKeypad() {
        customKeypad = binding.keyboard;
        customKeypad.setCallback(this);
        customKeypad.setDecimalSeparator(getDefaultDecimalSeparator());

        //Enable custom keypad and disables default keyboard from popping up
        customKeypad.enableOnView(binding.amountRow.amountBtc);
        customKeypad.enableOnView(binding.amountRow.amountFiat);
        customKeypad.enableOnView(binding.customFee);

        binding.amountRow.amountBtc.setText("");
        binding.amountRow.amountBtc.requestFocus();
    }

    private void setupViews() {
        setupDestinationView();
        setupSendFromView();
        setupReceiveToView();

        setupBtcTextField();
        setupFiatTextField();

        binding.customFee.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable customizedFee) {
                ItemAccount selectedItem = viewModel.getSendingItemAccount();
                viewModel.calculateTransactionAmounts(selectedItem,
                        binding.amountRow.amountBtc.getText().toString(),
                        customizedFee.toString(),
                        null);
            }
        });

        binding.max.setOnClickListener(view ->
                viewModel.spendAllClicked(viewModel.getSendingItemAccount(), binding.customFee.getText().toString()));

        binding.buttonSend.setOnClickListener(v -> {
            customKeypad.setNumpadVisibility(View.GONE);
            if (ConnectivityStatus.hasConnectivity(getActivity())) {
                requestSendPayment(binding.amountRow.amountBtc.getText().toString());
            } else {
                showToast(R.string.check_connectivity_exit, ToastCustom.TYPE_ERROR);
            }
        });
    }

    private void requestSendPayment(String amount) {
        viewModel.onSendClicked(binding.customFee.getText().toString(),
                amount,
                false,
                binding.destination.getText().toString());
    }

    private void setupDestinationView() {
        binding.destination.setHorizontallyScrolling(false);
        binding.destination.setLines(3);
        binding.destination.setOnClickListener(view -> {
            binding.destination.setText("");
            viewModel.setReceivingAddress(null);
        });
        binding.destination.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && customKeypad != null) {
                customKeypad.setNumpadVisibility(View.GONE);
            }
        });
    }

    private void setupSendFromView() {
        ItemAccount itemAccount;
        if (selectedAccountPosition != -1) {
            itemAccount = viewModel.getAddressList(false).get(selectedAccountPosition);
        } else {
            itemAccount = viewModel.getAddressList(false).get(viewModel.getDefaultAccount());
        }

        viewModel.setSendingAddress(itemAccount);
        viewModel.calculateTransactionAmounts(itemAccount,
                binding.amountRow.amountBtc.getText().toString(),
                binding.customFee.getText().toString(), null);
        binding.from.setText(itemAccount.label);

        binding.from.setOnClickListener(v -> startFromFragment());
        binding.imageviewDropdownSend.setOnClickListener(v -> startFromFragment());
    }

    private void startFromFragment() {
        AccountChooserActivity.startForResult(this,
                AccountChooserActivity.REQUEST_CODE_CHOOSE_ACCOUNT_SEND,
                PaymentRequestType.REQUEST);
    }

    private void setupReceiveToView() {
        binding.imageviewDropdownReceive.setOnClickListener(v ->
                AccountChooserActivity.startForResult(this,
                        AccountChooserActivity.REQUEST_CODE_CHOOSE_ACCOUNT_RECEIVE,
                        PaymentRequestType.SEND));
    }

    @Override
    public void hideSendingAddressField() {
        binding.fromRow.setVisibility(View.GONE);
    }

    @Override
    public void hideReceivingAddressField() {
        binding.destination.setHint(R.string.to_field_helper_no_dropdown);
    }

    @Override
    public void showInvalidAmount() {
        showToast(R.string.invalid_amount, ToastCustom.TYPE_ERROR);
    }

    @Override
    public void updateBtcAmount(String amount) {
        binding.amountRow.amountBtc.setText(amount);
        binding.amountRow.amountBtc.setSelection(binding.amountRow.amountBtc.getText().length());
    }

    @Override
    public void setDestinationAddress(String btcAddress) {
        binding.destination.setText(btcAddress);
    }

    @Override
    public void setMaxAvailable(String max) {
        binding.max.setText(max);
    }

    @Override
    public void setEstimate(String estimate) {
        binding.tvEstimate.setText(estimate);
    }

    @Override
    public void setEstimateColor(@ColorRes int color) {
        binding.tvEstimate.setTextColor(ContextCompat.getColor(getContext(), color));
    }

    @Override
    public void setCustomFeeColor(@ColorRes int color) {
        binding.customFee.setTextColor(ContextCompat.getColor(getContext(), color));
    }

    @Override
    public void setUnconfirmedFunds(String warning) {
        binding.unconfirmedFundsWarning.setText(warning);
    }

    @Override
    public void setContactName(String name) {
        binding.destination.setText(name);
    }

    @Override
    public void setMaxAvailableVisible(boolean visible) {
        if (visible) {
            binding.max.setVisibility(View.VISIBLE);
            binding.progressBarMaxAvailable.setVisibility(View.INVISIBLE);
        } else {
            binding.max.setVisibility(View.INVISIBLE);
            binding.progressBarMaxAvailable.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setMaxAvailableColor(@ColorRes int color) {
        binding.max.setTextColor(ContextCompat.getColor(getContext(), color));
    }

    @Override
    public void updateBtcUnit(String unit) {
        binding.amountRow.currencyBtc.setText(unit);
    }

    @Override
    public void updateFiatUnit(String unit) {
        binding.amountRow.currencyFiat.setText(unit);
    }

    @Override
    public void onSetSpendAllAmount(String textFromSatoshis) {
        binding.amountRow.amountBtc.setText(textFromSatoshis);
    }

    @Override
    public void lockDestination() {
        // Disable changing destination and disable all input
        binding.imageviewDropdownReceive.setVisibility(View.GONE);
        binding.destination.setOnClickListener(null);
        binding.destination.setKeyListener(null);
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(getActivity(), getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void showSecondPasswordDialog() {
        new SecondPasswordHandler(getContext()).validate(new SecondPasswordHandler.ResultListener() {
            @Override
            public void onNoSecondPassword() {
                viewModel.onNoSecondPassword();
            }

            @Override
            public void onSecondPasswordValidated(String validateSecondPassword) {
                viewModel.onSecondPasswordValidated(validateSecondPassword);
            }
        });
    }

    @Override
    public void onShowTransactionSuccess(@Nullable String mdid, String hash, @Nullable String fctxId, long transactionValue) {
        playAudio();
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcastSync(new Intent(BalanceFragment.ACTION_INTENT));

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.modal_transaction_success, null);
        transactionSuccessDialog = dialogBuilder.setView(dialogView)
                .setPositiveButton(getString(R.string.done), null)
                .create();
        transactionSuccessDialog.setTitle(R.string.transaction_submitted);

        AppRate appRate = new AppRate(getActivity())
                .setMinTransactionsUntilPrompt(3)
                .incrementTransactionCount();

        // If should show app rate, success dialog shows first and launches
        // rate dialog on dismiss. Dismissing rate dialog then closes the page. This will
        // happen if the user chooses to rate the app - they'll return to the main page.
        if (appRate.shouldShowDialog()) {
            AlertDialog ratingDialog = appRate.getRateDialog();
            ratingDialog.setOnDismissListener(d -> finishAndNotifySuccess(mdid, hash, fctxId, transactionValue));
            transactionSuccessDialog.show();
            transactionSuccessDialog.setOnDismissListener(d -> ratingDialog.show());
        } else {
            transactionSuccessDialog.show();
            transactionSuccessDialog.setOnDismissListener(dialogInterface -> finishAndNotifySuccess(mdid, hash, fctxId, transactionValue));
        }

        dialogHandler.postDelayed(dialogRunnable, 5 * 1000);
    }

    private void finishAndNotifySuccess(@Nullable String mdid, String hash, @Nullable String fctxId, long transactionValue) {
        if (listener != null) {
            listener.onSendPaymentSuccessful(mdid, hash, fctxId, transactionValue);
        }
        finishPage();
    }

    @Override
    public void onShowBIP38PassphrasePrompt(String scanData) {
        final EditText password = new EditText(getActivity());
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setHint(R.string.password);

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.bip38_password_entry)
                .setView(password)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) ->
                        viewModel.spendFromWatchOnlyBIP38(password.getText().toString(), scanData))
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void onShowLargeTransactionWarning(AlertDialog alertDialog) {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setCancelable(false)
                .setTitle(R.string.warning)
                .setMessage(R.string.large_tx_warning)
                .setNegativeButton(R.string.go_back, (dialog, which) -> alertDialog.dismiss())
                .setPositiveButton(R.string.accept_higher_fee, null)
                .create()
                .show();
    }

    @Override
    public void onShowSpendFromWatchOnly(String address) {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.privx_required)
                .setMessage(String.format(getString(R.string.watch_only_spend_instructionss), address))
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_continue, (dialog, whichButton) -> {
                    if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        PermissionUtil.requestCameraPermissionFromActivity(binding.getRoot(), getActivity());
                    } else {
                        startScanActivity(SCAN_PRIVX);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    @Override
    public void navigateToAddNote(String contactId, PaymentRequestType paymentRequestType, long satoshis) {
        if (listener != null) {
            listener.onTransactionNotesRequested(contactId, null, paymentRequestType, satoshis);
        }
    }

    // BTC Field
    private void setupBtcTextField() {
        binding.amountRow.amountBtc.setSelectAllOnFocus(true);
        binding.amountRow.amountBtc.setHint("0" + getDefaultDecimalSeparator() + "00");

        binding.amountRow.amountBtc.setKeyListener(
                DigitsKeyListener.getInstance("0123456789" + getDefaultDecimalSeparator()));
        binding.amountRow.amountBtc.addTextChangedListener(btcTextWatcher);
    }

    // Fiat Field
    private void setupFiatTextField() {
        binding.amountRow.amountFiat.setHint("0" + getDefaultDecimalSeparator() + "00");
        binding.amountRow.amountFiat.setSelectAllOnFocus(true);

        binding.amountRow.amountFiat.setKeyListener(
                DigitsKeyListener.getInstance("0123456789" + getDefaultDecimalSeparator()));
        binding.amountRow.amountFiat.addTextChangedListener(fiatTextWatcher);
    }

    @Override
    public void updateBtcTextField(String text) {
        binding.amountRow.amountBtc.setText(text);
    }

    @Override
    public void updateFiatTextField(String text) {
        binding.amountRow.amountFiat.setText(text);
    }

    @Thunk
    void setKeyListener(Editable s, EditText editText) {
        if (s.toString().contains(getDefaultDecimalSeparator())) {
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
        } else {
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789" + getDefaultDecimalSeparator()));
        }
    }

    @Thunk
    Editable formatEditable(Editable s, String input, int maxLength, EditText editText) {
        try {
            if (input.contains(getDefaultDecimalSeparator())) {
                String dec = input.substring(input.indexOf(getDefaultDecimalSeparator()));
                if (dec.length() > 0) {
                    dec = dec.substring(1);
                    if (dec.length() > maxLength) {
                        editText.setText(input.substring(0, input.length() - 1));
                        editText.setSelection(editText.getText().length());
                        s = editText.getEditableText();
                    }
                }
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "afterTextChanged: ", e);
        }
        return s;
    }

    private TextWatcher btcTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // No-op
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // No-op
        }

        @Override
        public void afterTextChanged(Editable s) {
            String input = s.toString();

            binding.amountRow.amountBtc.removeTextChangedListener(this);
            NumberFormat btcFormat = NumberFormat.getInstance(Locale.getDefault());
            btcFormat.setMaximumFractionDigits(viewModel.getCurrencyHelper().getMaxBtcDecimalLength() + 1);
            btcFormat.setMinimumFractionDigits(0);

            s = formatEditable(s, input, viewModel.getCurrencyHelper().getMaxBtcDecimalLength(), binding.amountRow.amountBtc);

            binding.amountRow.amountBtc.addTextChangedListener(this);

            if (textChangeAllowed) {
                textChangeAllowed = false;
                viewModel.updateFiatTextField(s.toString());

                textChangeAllowed = true;
            }
            setKeyListener(s, binding.amountRow.amountBtc);
        }
    };

    private TextWatcher fiatTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // No-op
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // No-op
        }

        @Override
        public void afterTextChanged(Editable s) {
            String input = s.toString();

            binding.amountRow.amountFiat.removeTextChangedListener(this);
            int maxLength = 2;
            NumberFormat fiatFormat = NumberFormat.getInstance(Locale.getDefault());
            fiatFormat.setMaximumFractionDigits(maxLength + 1);
            fiatFormat.setMinimumFractionDigits(0);

            s = formatEditable(s, input, maxLength, binding.amountRow.amountFiat);

            binding.amountRow.amountFiat.addTextChangedListener(this);

            if (textChangeAllowed) {
                textChangeAllowed = false;
                viewModel.updateBtcTextField(s.toString());
                textChangeAllowed = true;
            }
            setKeyListener(s, binding.amountRow.amountFiat);
        }
    };

    private String getDefaultDecimalSeparator() {
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        return Character.toString(symbols.getDecimalSeparator());
    }

    @Override
    public void onShowPaymentDetails(PaymentConfirmationDetails details) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        FragmentSendConfirmBinding dialogBinding = inflate(LayoutInflater.from(getActivity()),
                R.layout.fragment_send_confirm, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        dialogBinding.confirmFromLabel.setText(details.fromLabel);
        dialogBinding.confirmToLabel.setText(details.toLabel);
        dialogBinding.confirmAmountBtcUnit.setText(details.btcUnit);
        dialogBinding.confirmAmountFiatUnit.setText(details.fiatUnit);
        dialogBinding.confirmAmountBtc.setText(details.btcAmount);
        dialogBinding.confirmAmountFiat.setText(details.fiatAmount);
        dialogBinding.confirmFeeBtc.setText(details.btcFee);
        dialogBinding.confirmFeeFiat.setText(details.fiatFee);
        dialogBinding.confirmTotalBtc.setText(details.btcTotal);
        dialogBinding.confirmTotalFiat.setText(details.fiatTotal);

        String feeMessage = "";
        if (details.isSurge) {
            dialogBinding.ivFeeInfo.setVisibility(View.VISIBLE);
            feeMessage += getString(R.string.transaction_surge);

        }

        if (details.hasConsumedAmounts) {
            dialogBinding.ivFeeInfo.setVisibility(View.VISIBLE);
            if (details.hasConsumedAmounts) {
                if (details.isSurge) feeMessage += "\n\n";
                feeMessage += getString(R.string.large_tx_high_fee_warning);
            }

        }

        final String finalFeeMessage = feeMessage;
        dialogBinding.ivFeeInfo.setOnClickListener(view -> new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.transaction_fee)
                .setMessage(finalFeeMessage)
                .setPositiveButton(android.R.string.ok, null).show());

        if (details.isSurge) {
            dialogBinding.confirmFeeBtc.setTextColor(ContextCompat.getColor(getContext(), R.color.blockchain_send_red));
            dialogBinding.confirmFeeFiat.setTextColor(ContextCompat.getColor(getContext(), R.color.blockchain_send_red));
            dialogBinding.ivFeeInfo.setVisibility(View.VISIBLE);
        }

        dialogBinding.tvCustomizeFee.setOnClickListener(v -> {
            if (alertDialog.isShowing()) {
                alertDialog.cancel();
            }

            getActivity().runOnUiThread(() -> {
                binding.customFeeContainer.setVisibility(View.VISIBLE);

                binding.customFee.setText(details.btcFee);
                binding.customFee.setHint(details.btcSuggestedFee);
                binding.customFee.requestFocus();
                binding.customFee.setSelection(binding.customFee.getText().length());
                customKeypad.setNumpadVisibility(View.GONE);
            });

            alertCustomSpend(details.btcSuggestedFee, details.btcUnit);

        });

        dialogBinding.confirmCancel.setOnClickListener(v -> {
            if (alertDialog.isShowing()) {
                alertDialog.cancel();
            }
        });

        dialogBinding.confirmSend.setOnClickListener(v -> {
            if (ConnectivityStatus.hasConnectivity(getActivity())) {
                dialogBinding.confirmSend.setClickable(false);
                viewModel.submitPayment(alertDialog);
            } else {
                showToast(R.string.check_connectivity_exit, ToastCustom.TYPE_ERROR);
                // Queue tx here
            }
        });

        if (getActivity() != null && !getActivity().isFinishing()) {
            alertDialog.show();
        }

        // To prevent the dialog from appearing too large on Android N
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        if (viewModel.isLargeTransaction()) {
            onShowLargeTransactionWarning(alertDialog);
        }
    }

    @Override
    public void onShowReceiveToWatchOnlyWarning(String address) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle);
        AlertWatchOnlySpendBinding dialogBinding = inflate(LayoutInflater.from(getActivity()),
                R.layout.alert_watch_only_spend, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());
        dialogBuilder.setCancelable(false);

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        dialogBinding.confirmCancel.setOnClickListener(v -> {
            binding.destination.setText("");
            if (dialogBinding.confirmDontAskAgain.isChecked())
                viewModel.setWatchOnlySpendWarning(false);
            alertDialog.dismiss();
        });

        dialogBinding.confirmContinue.setOnClickListener(v -> {
            binding.destination.setText(address);
            if (dialogBinding.confirmDontAskAgain.isChecked())
                viewModel.setWatchOnlySpendWarning(false);
            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    @Override
    public void onShowAlterFee(String absoluteFeeSuggested,
                               String body,
                               @StringRes int positiveAction,
                               @StringRes int negativeAction) {

        new AlertDialog.Builder(getContext(), R.style.AlertDialogStyle)
                .setTitle(R.string.warning)
                .setMessage(body)
                .setCancelable(false)
                .setPositiveButton(positiveAction, (dialog, which) ->
                        requestSendPayment(absoluteFeeSuggested))
                .setNegativeButton(negativeAction, (dialog, which) ->
                        requestSendPayment(binding.amountRow.amountBtc.getText().toString()))
                .create()
                .show();
    }

    private void alertCustomSpend(String btcFee, String btcFeeUnit) {
        String message = getResources().getString(R.string.recommended_fee)
                + "\n\n"
                + btcFee
                + " " + btcFeeUnit;

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.transaction_fee)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    private void playAudio() {
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null && audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            MediaPlayer mp;
            mp = MediaPlayer.create(getActivity().getApplicationContext(), R.raw.beep);
            mp.setOnCompletionListener(mp1 -> {
                mp1.reset();
                mp1.release();
            });
            mp.start();
        }
    }

    @Nullable
    @Override
    public String getClipboardContents() {
        ClipboardManager clipMan = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipMan.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
            return clip.getItemAt(0).coerceToText(getActivity()).toString();
        }
        return null;
    }

    @Override
    public void showProgressDialog() {
        progressDialog = new MaterialProgressDialog(getActivity());
        progressDialog.setCancelable(false);
        progressDialog.setMessage(R.string.please_wait);
        progressDialog.show();
    }

    @Override
    public void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public void finishPage() {
        if (listener != null) listener.onSendFragmentClose();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSendFragmentInteractionListener) {
            listener = (OnSendFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context + " must implement OnSendFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface OnSendFragmentInteractionListener {

        void onSendFragmentClose();

        void onSendFragmentStart();

        void onSendPaymentSuccessful(@Nullable String mdid, String transactionHash, @Nullable String fctxId, long transactionValue);

        void onTransactionNotesRequested(String contactId, @Nullable Integer accountPosition, PaymentRequestType paymentRequestType, long satoshis);
    }
}
