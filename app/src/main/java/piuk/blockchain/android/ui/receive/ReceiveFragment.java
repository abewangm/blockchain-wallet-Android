package piuk.blockchain.android.ui.receive;

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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.fasterxml.jackson.databind.ObjectMapper;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;

import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.contacts.PaymentRequestType;
import piuk.blockchain.android.databinding.AlertWatchOnlySpendBinding;
import piuk.blockchain.android.databinding.FragmentReceiveBinding;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.chooser.AccountChooserActivity;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.home.MainActivity;
import piuk.blockchain.android.util.PermissionUtil;
import piuk.blockchain.android.util.annotations.Thunk;
import timber.log.Timber;

import static piuk.blockchain.android.ui.chooser.AccountChooserActivity.EXTRA_SELECTED_ITEM;
import static piuk.blockchain.android.ui.chooser.AccountChooserActivity.EXTRA_SELECTED_OBJECT_TYPE;

public class ReceiveFragment extends Fragment implements ReceiveViewModel.DataListener {

    private static final String TAG = ReceiveFragment.class.getSimpleName();
    private static final String ARG_SELECTED_ACCOUNT_POSITION = "selected_account_position";
    private static final int COOL_DOWN_MILLIS = 2 * 1000;

    @Thunk ReceiveViewModel viewModel;
    @Thunk FragmentReceiveBinding binding;
    private BottomSheetDialog bottomSheetDialog;
    private OnReceiveFragmentInteractionListener listener;

    @Thunk boolean textChangeAllowed = true;
    private String uri;
    private long backPressed;
    @Thunk int selectedAccountPosition = -1;

    private IntentFilter intentFilter = new IntentFilter(BalanceFragment.ACTION_INTENT);
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction().equals(BalanceFragment.ACTION_INTENT)) {
                if (viewModel != null) {
                    // Update UI with new Address + QR
                    viewModel.updateAccountList();
                }
            }
        }
    };

    public ReceiveFragment() {
        // Required empty public constructor
    }

    public static ReceiveFragment newInstance(int selectedAccountPosition) {
        ReceiveFragment fragment = new ReceiveFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SELECTED_ACCOUNT_POSITION, selectedAccountPosition);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            selectedAccountPosition = getArguments().getInt(ARG_SELECTED_ACCOUNT_POSITION);
        }
    }

    @Override
    public void startContactSelectionActivity() {
        AccountChooserActivity.startForResult(this,
                AccountChooserActivity.REQUEST_CODE_CHOOSE_CONTACT,
                PaymentRequestType.CONTACT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_receive, container, false);
        viewModel = new ReceiveViewModel(this, Locale.getDefault());
        viewModel.onViewReady();

        setupLayout();

        selectAccount(selectedAccountPosition != -1
                ? selectedAccountPosition : viewModel.getDefaultAccountPosition());

        binding.scrollView.post(() -> binding.scrollView.scrollTo(0, 0));

        return binding.getRoot();
    }

    private void setupToolbar() {
        if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((BaseAuthActivity) getActivity()).setupToolbar(
                    ((MainActivity) getActivity()).getSupportActionBar(), R.string.receive_bitcoin);
        } else {
            finishPage();
        }
    }

    private void setupLayout() {
        if (viewModel.getReceiveToList().size() == 1) {
            binding.fromRow.setVisibility(View.GONE);
            binding.dividerFromRow.setVisibility(View.GONE);
        }

        // BTC Field
        binding.amountContainer.amountBtc.setKeyListener(
                DigitsKeyListener.getInstance(false, true));
        binding.amountContainer.amountBtc.setHint("0" + getDefaultDecimalSeparator() + "00");
        binding.amountContainer.amountBtc.addTextChangedListener(btcTextWatcher);

        // Fiat Field
        binding.amountContainer.amountFiat.setKeyListener(
                DigitsKeyListener.getInstance(false, true));
        binding.amountContainer.amountFiat.setHint("0" + getDefaultDecimalSeparator() + "00");
        binding.amountContainer.amountFiat.addTextChangedListener(fiatTextWatcher);

        // Units
        binding.amountContainer.currencyBtc.setText(viewModel.getCurrencyHelper().getBtcUnit());
        binding.amountContainer.currencyFiat.setText(viewModel.getCurrencyHelper().getFiatUnit());

        // QR Code
        binding.qr.setOnClickListener(v -> showClipboardWarning());
        binding.qr.setOnLongClickListener(view -> {
            onShareClicked();
            return true;
        });

        binding.destination.setOnClickListener(v ->
                AccountChooserActivity.startForResult(this,
                        AccountChooserActivity.REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT_FROM_RECEIVE,
                        PaymentRequestType.REQUEST));

        binding.imageviewDropdownReceive.setOnClickListener(v ->
                AccountChooserActivity.startForResult(this,
                        AccountChooserActivity.REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT_FROM_RECEIVE,
                        PaymentRequestType.REQUEST));

        if (BuildConfig.CONTACTS_ENABLED) {
            binding.buttonSendToContact.setOnClickListener(v ->
                    viewModel.onSendToContactClicked(binding.amountContainer.amountBtc.getText().toString()));
        } else {
            binding.buttonSendToContact.setVisibility(View.GONE);
        }
    }

    private TextWatcher btcTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            String input = s.toString();

            binding.amountContainer.amountBtc.removeTextChangedListener(this);
            NumberFormat btcFormat = NumberFormat.getInstance(Locale.getDefault());
            btcFormat.setMaximumFractionDigits(viewModel.getCurrencyHelper().getMaxBtcDecimalLength() + 1);
            btcFormat.setMinimumFractionDigits(0);

            s = formatEditable(s, input, viewModel.getCurrencyHelper().getMaxBtcDecimalLength(), binding.amountContainer.amountBtc);

            binding.amountContainer.amountBtc.addTextChangedListener(this);

            if (textChangeAllowed) {
                textChangeAllowed = false;
                viewModel.updateFiatTextField(s.toString().replace(".", getDefaultDecimalSeparator()));

                displayQRCode(selectedAccountPosition);
                textChangeAllowed = true;
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // No-op
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // No-op
        }
    };

    private TextWatcher fiatTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            String input = s.toString();

            binding.amountContainer.amountFiat.removeTextChangedListener(this);
            int maxLength = 2;
            NumberFormat fiatFormat = NumberFormat.getInstance(Locale.getDefault());
            fiatFormat.setMaximumFractionDigits(maxLength + 1);
            fiatFormat.setMinimumFractionDigits(0);

            s = formatEditable(s, input, maxLength, binding.amountContainer.amountFiat);

            binding.amountContainer.amountFiat.addTextChangedListener(this);

            if (textChangeAllowed) {
                textChangeAllowed = false;
                viewModel.updateBtcTextField(s.toString().replace(".", getDefaultDecimalSeparator()));

                displayQRCode(selectedAccountPosition);
                textChangeAllowed = true;
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // No-op
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // No-op
        }
    };

    @Thunk
    Editable formatEditable(Editable s, String input, int maxLength, EditText editText) {
        try {
            if (input.contains(String.valueOf(getDefaultDecimalSeparator()))) {
                String dec = input.substring(input.indexOf(getDefaultDecimalSeparator()));
                if (!dec.isEmpty()) {
                    dec = dec.substring(1);
                    if (dec.length() > maxLength) {
                        editText.setText(input.substring(0, input.length() - 1));
                        editText.setSelection(editText.getText().length());
                        s = editText.getEditableText();
                    }
                }
            }
        } catch (NumberFormatException e) {
            Timber.e(e);
        }
        return s;
    }

    @Thunk
    String getDefaultDecimalSeparator() {
        return String.valueOf(DecimalFormatSymbols.getInstance().getDecimalSeparator());
    }

    public int getSelectedAccountPosition() {
        return selectedAccountPosition;
    }

    private void selectAccount(int position) {
        selectedAccountPosition = position;
        displayQRCode(position);
    }

    @Thunk
    void displayQRCode(int position) {
        Object object = viewModel.getAccountItemForPosition(position);
        // Disable send to contact button if not using HD account
        binding.buttonSendToContact.setEnabled(object instanceof Account);

        String label;
        String receiveAddress = null;
        if (object instanceof LegacyAddress) {
            LegacyAddress legacyAddress = (LegacyAddress) object;
            receiveAddress = legacyAddress.getAddress();
            label = legacyAddress.getLabel();
            if (label == null || label.isEmpty()) {
                label = receiveAddress;
            }
        } else {
            Account account = (Account) object;
            viewModel.getV3ReceiveAddress(account);
            label = account.getLabel();
            if (label == null || label.isEmpty()) {
                label = account.getXpub();
            }
        }

        binding.destination.setText(StringUtils.abbreviate(label, 32));
        updateReceiveAddress(receiveAddress);
    }

    @Override
    public void updateReceiveAddress(String receiveAddress) {
        binding.receivingAddress.setText(receiveAddress);

        long amountLong = viewModel.getCurrencyHelper().getLongAmount(
                binding.amountContainer.amountBtc.getText().toString());

        BigInteger amountBigInt = viewModel.getCurrencyHelper().getUndenominatedAmount(amountLong);

        if (viewModel.getCurrencyHelper().getIfAmountInvalid(amountBigInt)) {
            ToastCustom.makeText(getContext(), getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return;
        }

        if (receiveAddress != null) {
            if (!amountBigInt.equals(BigInteger.ZERO)) {
                uri = BitcoinURI.convertToBitcoinURI(
                        Address.fromBase58(new EnvironmentSettings().getNetworkParameters(), receiveAddress),
                        Coin.valueOf(amountBigInt.longValue()), "", "");
            } else {
                uri = "bitcoin:" + receiveAddress;
            }
            viewModel.generateQrCode(uri);
        }
    }

    @Override
    public void updateFiatTextField(String text) {
        binding.amountContainer.amountFiat.setText(text);
    }

    @Override
    public void updateBtcTextField(String text) {
        binding.amountContainer.amountBtc.setText(text);
    }

    @Override
    public void onResume() {
        super.onResume();
        setupToolbar();
        viewModel.updateAccountList();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void showQrLoading() {
        binding.qr.setVisibility(View.INVISIBLE);
        binding.receivingAddress.setVisibility(View.INVISIBLE);
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void showQrCode(@Nullable Bitmap bitmap) {
        binding.progressBar.setVisibility(View.INVISIBLE);
        binding.qr.setVisibility(View.VISIBLE);
        binding.receivingAddress.setVisibility(View.VISIBLE);
        binding.qr.setImageBitmap(bitmap);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Set receiving account
        if (resultCode == Activity.RESULT_OK
                && requestCode == AccountChooserActivity.REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT_FROM_RECEIVE
                && data != null) {

            try {
                Class type = Class.forName(data.getStringExtra(EXTRA_SELECTED_OBJECT_TYPE));
                Object object = new ObjectMapper().readValue(data.getStringExtra(EXTRA_SELECTED_ITEM), type);

                if (viewModel.warnWatchOnlySpend()) {
                    promptWatchOnlySpendWarning(object);
                }

                selectAccount(viewModel.getObjectPosition(object));

            } catch (ClassNotFoundException | IOException e) {
                Log.e(TAG, "onActivityResult: ", e);
                selectAccount(viewModel.getDefaultAccountPosition());
            }
            // Choose contact for request
        } else if (resultCode == Activity.RESULT_OK
                && requestCode == AccountChooserActivity.REQUEST_CODE_CHOOSE_CONTACT
                && data != null) {

            try {
                Contact contact = new ObjectMapper().readValue(
                        data.getStringExtra(EXTRA_SELECTED_ITEM),
                        Contact.class);

                if (listener != null) {
                    listener.onTransactionNotesRequested(
                            contact.getId(),
                            viewModel.getCorrectedAccountIndex(selectedAccountPosition),
                            PaymentRequestType.REQUEST,
                            viewModel.getCurrencyHelper().getLongAmount(
                                    binding.amountContainer.amountBtc.getText().toString()));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void setupBottomSheet(String uri) {
        List<ReceiveViewModel.SendPaymentCodeData> list = viewModel.getIntentDataList(uri);
        if (list != null) {
            ShareReceiveIntentAdapter adapter = new ShareReceiveIntentAdapter(list);
            adapter.setItemClickedListener(() -> bottomSheetDialog.dismiss());

            View sheetView = View.inflate(getActivity(), R.layout.bottom_sheet_receive, null);
            RecyclerView recyclerView = (RecyclerView) sheetView.findViewById(R.id.recycler_view);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialog);
            bottomSheetDialog.setContentView(sheetView);

            adapter.notifyDataSetChanged();
        }
    }

    private void onShareClicked() {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_share)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        PermissionUtil.requestWriteStoragePermissionFromFragment(binding.getRoot(), this);
                    } else {
                        showBottomSheet();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showBottomSheet() {
        setupBottomSheet(uri);
        if (bottomSheetDialog != null) {
            bottomSheetDialog.show();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.PERMISSION_REQUEST_WRITE_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showBottomSheet();
            } else {
                // Permission request was denied.
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showClipboardWarning() {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Send address", binding.receivingAddress.getText().toString());
                    ToastCustom.makeText(getActivity(), getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_GENERAL);
                    clipboard.setPrimaryClip(clip);
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Thunk
    void promptWatchOnlySpendWarning(Object object) {
        if (object instanceof LegacyAddress && ((LegacyAddress) object).isWatchOnly()) {

            AlertWatchOnlySpendBinding dialogBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(getActivity()), R.layout.alert_watch_only_spend, null, false);

            AlertDialog alertDialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                    .setView(dialogBinding.getRoot())
                    .setCancelable(false)
                    .create();

            dialogBinding.confirmCancel.setOnClickListener(v -> {
                selectAccount(viewModel.getDefaultAccountPosition());
                viewModel.setWarnWatchOnlySpend(!dialogBinding.confirmDontAskAgain.isChecked());
                alertDialog.dismiss();
            });

            dialogBinding.confirmContinue.setOnClickListener(v -> {
                viewModel.setWarnWatchOnlySpend(!dialogBinding.confirmDontAskAgain.isChecked());
                alertDialog.dismiss();
            });

            alertDialog.show();
        }
    }

    @Override
    public Bitmap getQrBitmap() {
        return ((BitmapDrawable) binding.qr.getDrawable()).getBitmap();
    }

    @Override
    public void showToast(String message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(getActivity(), message, ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void onAccountDataChanged() {
        selectAccount(selectedAccountPosition != -1
                ? selectedAccountPosition : viewModel.getDefaultAccountPosition());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (menu != null) menu.clear();
        inflater.inflate(R.menu.menu_receive, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                onShareClicked();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onBackPressed() {
        handleBackPressed();
    }

    private void handleBackPressed() {
        if (backPressed + COOL_DOWN_MILLIS > System.currentTimeMillis()) {
            AccessState.getInstance().logout(getContext());
            return;
        } else {
            onExitConfirmToast();
        }

        backPressed = System.currentTimeMillis();
    }

    public void onExitConfirmToast() {
        ToastCustom.makeText(getActivity(), getString(R.string.exit_confirm), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
    }

    public void finishPage() {
        if (listener != null) {
            listener.onReceiveFragmentClose();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnReceiveFragmentInteractionListener) {
            listener = (OnReceiveFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context + " must implement OnReceiveFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface OnReceiveFragmentInteractionListener {

        void onReceiveFragmentClose();

        void onTransactionNotesRequested(String contactId, @Nullable Integer accountPosition, PaymentRequestType paymentRequestType, long satoshis);

    }
}
