package piuk.blockchain.android.ui.balance;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ShortcutManager;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentBalanceBinding;
import piuk.blockchain.android.ui.backup.BackupWalletActivity;
import piuk.blockchain.android.ui.customviews.BottomSpacerDecoration;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.home.MainActivity;
import piuk.blockchain.android.ui.home.SecurityPromptDialog;
import piuk.blockchain.android.ui.home.TransactionSelectedListener;
import piuk.blockchain.android.ui.onboarding.OnboardingPagerAdapter;
import piuk.blockchain.android.ui.receive.ReceiveFragment;
import piuk.blockchain.android.ui.send.SendFragment;
import piuk.blockchain.android.ui.settings.SettingsActivity;
import piuk.blockchain.android.ui.settings.SettingsFragment;
import piuk.blockchain.android.ui.shortcuts.LauncherShortcutHelper;
import piuk.blockchain.android.ui.transactions.TransactionDetailActivity;
import piuk.blockchain.android.util.AndroidUtils;
import piuk.blockchain.android.util.DateUtil;
import piuk.blockchain.android.util.ListUtil;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.ViewUtils;
import piuk.blockchain.android.util.annotations.Thunk;


public class BalanceFragment extends Fragment implements BalanceViewModel.DataListener,
        TransactionSelectedListener {

    public static final String ACTION_INTENT = "info.blockchain.wallet.ui.BalanceFragment.REFRESH";
    public static final String KEY_TRANSACTION_LIST_POSITION = "transaction_list_position";
    public static final String KEY_TRANSACTION_HASH = "transaction_hash";
    public static final String ARGUMENT_BROADCASTING_PAYMENT = "broadcasting_payment";
    public static final int SHOW_BTC = 1;
    private static final int SHOW_FIAT = 2;
    private int balanceDisplayState = SHOW_BTC;
    private BalanceHeaderAdapter accountsAdapter;
    private MaterialProgressDialog progressDialog;
    private BottomSpacerDecoration spacerDecoration;
    private OnboardingPagerAdapter onboardingPagerAdapter;
    @Thunk OnFragmentInteractionListener interactionListener;
    @Thunk boolean isBTC = true;
    // Accounts list
    @Thunk AppCompatSpinner accountSpinner;
    // Tx list
    @Thunk BalanceListAdapter transactionAdapter;
    private DateUtil dateUtil;

    @Thunk FragmentBalanceBinding binding;
    @Thunk BalanceViewModel viewModel;

    public BalanceFragment() {
        // Required empty constructor
    }

    public static BalanceFragment newInstance(boolean broadcastingPayment) {
        Bundle args = new Bundle();
        args.putBoolean(ARGUMENT_BROADCASTING_PAYMENT, broadcastingPayment);
        BalanceFragment fragment = new BalanceFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction().equals(ACTION_INTENT) && getActivity() != null) {
                updateAccountList();
                updateBalanceAndTransactionList(true);
                refreshFacilitatedTransactions();
                // Check backup status on receiving funds
                viewModel.onViewReady();
                binding.rvTransactions.scrollToPosition(0);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_balance, container, false);
        viewModel = new BalanceViewModel(this);
        dateUtil = new DateUtil(getContext());

        balanceDisplayState = viewModel.getPrefsUtil().getValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, SHOW_BTC);
        isBTC = balanceDisplayState != SHOW_FIAT;

        setupViews();

        if (getArguments() == null || !getArguments().getBoolean(ARGUMENT_BROADCASTING_PAYMENT, false)) {
            refreshFacilitatedTransactions();
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.onViewReady();
        updateBalanceAndTransactionList(true);
    }

    private void setAccountSpinner() {
        ((AppCompatActivity) getContext()).setSupportActionBar(
                (Toolbar) getActivity().findViewById(R.id.toolbar_general));

        if (viewModel.getActiveAccountAndAddressList().size() > 1) {
            accountSpinner.setVisibility(View.VISIBLE);
        } else if (!viewModel.getActiveAccountAndAddressList().isEmpty()) {
            accountSpinner.setSelection(0);
            accountSpinner.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).getBottomNavigationView().restoreBottomNavigation();
        }

        interactionListener.resetNavigationDrawer();

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(receiver, filter);

        updateAccountList();
        viewModel.getFacilitatedTransactions();
        updateBalanceAndTransactionList(false);

        binding.rvTransactions.clearOnScrollListeners();

        updateAdapters();
    }

    private void updateAdapters() {
        String fiat = viewModel.getPrefsUtil().getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        double lastPrice = viewModel.getLastPrice(fiat);

        if (transactionAdapter != null) {
            transactionAdapter.notifyAdapterDataSetChanged(lastPrice);
        }

        if (accountsAdapter != null) {
            accountsAdapter.notifyFiatUnitsChanged(fiat, lastPrice);
        }
    }

    public void checkCachedTransactions() {
        if (accountSpinner != null) {
            updateBalanceAndTransactionList(false);
        }
    }

    @Override
    public void onExchangeRateUpdated() {
        updateAdapters();
    }

    @Override
    public void updateBalance(String balance) {
        binding.balance.setText(balance);
    }

    @Override
    public void setShowRefreshing(boolean showRefreshing) {
        binding.swipeContainer.setRefreshing(showRefreshing);
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(receiver);

        if (binding.swipeContainer != null) {
            binding.swipeContainer.setRefreshing(false);
            binding.swipeContainer.destroyDrawingCache();
            binding.swipeContainer.clearAnimation();
        }
    }

    /**
     * Deprecated, but necessary to prevent casting issues on <API21
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        interactionListener = (OnFragmentInteractionListener) activity;
    }

    @Override
    public void show2FaDialog() {
        SecurityPromptDialog securityPromptDialog = SecurityPromptDialog.newInstance(
                R.string.two_fa,
                getString(R.string.security_centre_two_fa_message),
                R.drawable.vector_mobile,
                R.string.enable,
                true,
                true);

        securityPromptDialog.setPositiveButtonListener(v -> {
            securityPromptDialog.dismiss();
            if (securityPromptDialog.isChecked()) {
                viewModel.neverPrompt2Fa();
            }
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            intent.putExtra(SettingsFragment.EXTRA_SHOW_TWO_FA_DIALOG, true);
            startActivity(intent);
        });

        securityPromptDialog.setNegativeButtonListener(v -> {
            securityPromptDialog.dismiss();
            if (securityPromptDialog.isChecked()) {
                viewModel.neverPrompt2Fa();
            }
        });

        if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
            securityPromptDialog.showDialog(getActivity().getSupportFragmentManager());
        }
    }

    @Override
    public void showBackupPromptDialog(boolean showNeverAgain) {
        SecurityPromptDialog securityPromptDialog = SecurityPromptDialog.newInstance(
                R.string.security_centre_backup_title,
                getString(R.string.security_centre_backup_message),
                R.drawable.vector_lock,
                R.string.security_centre_backup_positive_button,
                true,
                showNeverAgain);

        securityPromptDialog.setPositiveButtonListener(v -> {
            securityPromptDialog.dismiss();
            if (securityPromptDialog.isChecked()) {
                viewModel.neverPromptBackup();
            }
            Intent intent = new Intent(getActivity(), BackupWalletActivity.class);
            startActivity(intent);
        });

        securityPromptDialog.setNegativeButtonListener(v -> {
            securityPromptDialog.dismiss();
            if (securityPromptDialog.isChecked()) {
                viewModel.neverPromptBackup();
            }
        });

        if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
            securityPromptDialog.showDialog(getActivity().getSupportFragmentManager());
        }
    }

    /**
     * Position is offset to account for first item being "All Wallets". If returned result is -1,
     * {@link SendFragment} and {@link ReceiveFragment} can safely ignore and choose the defaults
     * instead.
     */
    public int getSelectedAccountPosition() {
        int position = accountSpinner.getSelectedItemPosition();
        if (position >= accountSpinner.getCount() - 1) {
            // End of list is imported addresses, ignore
            position = 0;
        }

        return position - 1;
    }

    public void refreshFacilitatedTransactions() {
        viewModel.refreshFacilitatedTransactions();
    }

    public void updateAccountList() {
        viewModel.updateAccountList();
    }

    public void updateBalanceAndTransactionList(boolean fetchTransactions) {
        viewModel.updateBalanceAndTransactionList(accountSpinner.getSelectedItemPosition(), isBTC, fetchTransactions);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupViews() {
        setShowRefreshing(true);
        binding.noTransactionInclude.noTxMessageLayout.setVisibility(View.GONE);

        binding.balance.setOnTouchListener((v, event) -> {
            if (balanceDisplayState == SHOW_BTC) {
                balanceDisplayState = SHOW_FIAT;
                isBTC = false;
                updateBalanceAndTransactionList(false);
            } else {
                balanceDisplayState = SHOW_BTC;
                isBTC = true;
                updateBalanceAndTransactionList(false);
            }

            transactionAdapter.onViewFormatUpdated(isBTC);
            viewModel.getPrefsUtil().setValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, balanceDisplayState);
            return false;
        });

        accountSpinner = binding.accountsSpinner;
        updateAccountList();

        String fiat = viewModel.getPrefsUtil().getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        accountsAdapter = new BalanceHeaderAdapter(
                getContext(),
                R.layout.spinner_balance_header,
                viewModel.getActiveAccountAndAddressList(),
                isBTC,
                new MonetaryUtil(viewModel.getPrefsUtil().getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)),
                fiat,
                viewModel.getLastPrice(fiat));

        accountsAdapter.setDropDownViewResource(R.layout.item_balance_account_dropdown);
        accountSpinner.setAdapter(accountsAdapter);
        accountSpinner.setOnTouchListener((v, event) ->
                event.getAction() == MotionEvent.ACTION_UP
                        && ((MainActivity) getActivity()).getDrawerOpen());
        accountSpinner.post(() -> accountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                //Refresh balance header and tx list
                updateBalanceAndTransactionList(true);
                binding.rvTransactions.scrollToPosition(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // No-op
            }
        }));

        String fiatString = viewModel.getPrefsUtil().getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        double lastPrice = viewModel.getLastPrice(fiatString);

        transactionAdapter = new BalanceListAdapter(
                viewModel.getContactsTransactionMap(),
                viewModel.getNotesTransactionMap(),
                viewModel.getPrefsUtil(),
                viewModel.getMonetaryUtil(),
                viewModel.stringUtils,
                dateUtil,
                lastPrice,
                isBTC);
        transactionAdapter.setTxListClickListener(new BalanceListAdapter.BalanceListClickListener() {
            @Override
            public void onTransactionClicked(int correctedPosition, int absolutePosition) {
                goToTransactionDetail(correctedPosition);
            }

            @Override
            public void onValueClicked(boolean isBtc) {
                isBTC = isBtc;
                viewModel.getPrefsUtil().setValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, isBtc ? SHOW_BTC : SHOW_FIAT);
                transactionAdapter.onViewFormatUpdated(isBtc);
                updateBalanceAndTransactionList(false);
            }

            @Override
            public void onFctxClicked(String fctxId) {
                viewModel.onPendingTransactionClicked(fctxId);
            }

            @Override
            public void onFctxLongClicked(String fctxId) {
                viewModel.onPendingTransactionLongClicked(fctxId);
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.rvTransactions.setHasFixedSize(true);
        binding.rvTransactions.setLayoutManager(layoutManager);
        binding.rvTransactions.setAdapter(transactionAdapter);
        // Disable blinking animations in RecyclerView
        RecyclerView.ItemAnimator animator = binding.rvTransactions.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        // drawerTitle account now that wallet has been created
        if (!viewModel.getPrefsUtil().getValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME, "").isEmpty()) {
            viewModel.getPayloadManager().getPayload().getHdWallets().get(0).getAccounts().get(0).setLabel(viewModel.getPrefsUtil().getValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME, ""));
            viewModel.getPrefsUtil().removeValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME);
            viewModel.getPayloadDataManager().syncPayloadWithServer()
                    .subscribe(() -> {
                        // No-op
                    }, throwable -> showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR));
            accountsAdapter.notifyDataSetChanged();
        }

        binding.swipeContainer.setProgressViewEndTarget(false, (int) ViewUtils.convertDpToPixel(72 + 20, getActivity()));
        binding.swipeContainer.setOnRefreshListener(() -> viewModel.onTransactionListRefreshed());
        binding.swipeContainer.setColorSchemeResources(
                R.color.product_green_medium,
                R.color.primary_blue_medium,
                R.color.product_red_medium);

        binding.noTransactionInclude.buttonGetBitcoin.setOnClickListener(v -> viewModel.getBitcoinClicked());

        setAnnouncement();
    }

    @Override
    public boolean isBtc() {
        return isBTC;
    }

    @Override
    public boolean getIfContactsEnabled() {
        return BuildConfig.CONTACTS_ENABLED;
    }

    @Override
    public int getSelectedItemPosition() {
        return accountSpinner.getSelectedItemPosition();
    }

    @Thunk
    void goToTransactionDetail(int position) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_TRANSACTION_LIST_POSITION, position);
        TransactionDetailActivity.start(getActivity(), bundle);
    }

    @Override
    public void onRefreshAccounts() {
        if (accountSpinner != null) setAccountSpinner();
        getActivity().runOnUiThread(() -> {
            if (accountsAdapter != null) accountsAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onAccountSizeChange() {
        if (accountSpinner != null) accountSpinner.setSelection(0);
    }

    @Override
    public void onRefreshBalanceAndTransactions() {
        // Notify adapter of change, let DiffUtil work out what needs changing
        List<Object> newTransactions = new ArrayList<>();
        ListUtil.addAllIfNotNull(newTransactions, viewModel.getTransactionList());
        transactionAdapter.onTransactionsUpdated(newTransactions);

        //Display help text to user if no transactionList on selected account/address
        handleTransactionsVisibility();

        accountsAdapter.notifyBtcChanged(isBTC);
        binding.rvTransactions.scrollToPosition(0);

        viewModel.storeSwipeReceiveAddresses();

        if (AndroidUtils.is25orHigher() && viewModel.areLauncherShortcutsEnabled()) {
            LauncherShortcutHelper launcherShortcutHelper = new LauncherShortcutHelper(
                    getActivity(),
                    viewModel.getPayloadDataManager(),
                    getActivity().getSystemService(ShortcutManager.class));

            launcherShortcutHelper.generateReceiveShortcuts();
        }

        if (spacerDecoration == null) {
            spacerDecoration = new BottomSpacerDecoration(
                    getContext(),
                    (int) ViewUtils.convertDpToPixel(56f, getContext()));
        }
        binding.rvTransactions.removeItemDecoration(spacerDecoration);
        binding.rvTransactions.addItemDecoration(spacerDecoration);
    }

    private void handleTransactionsVisibility() {
        if (!viewModel.getTransactionList().isEmpty()) {
            binding.rvTransactions.setVisibility(View.VISIBLE);
            binding.noTransactionInclude.noTxMessageLayout.setVisibility(View.GONE);
        } else {
            binding.rvTransactions.setVisibility(View.GONE);
            binding.noTransactionInclude.noTxMessageLayout.setVisibility(View.VISIBLE);

            if (!viewModel.isOnboardingComplete()) {
                initOnboardingPager();
                binding.noTransactionInclude.framelayoutOnboarding.setVisibility(View.VISIBLE);
            } else {
                binding.noTransactionInclude.framelayoutOnboarding.setVisibility(View.GONE);

                if(binding.announcementView.getVisibility() != View.VISIBLE) {
                    binding.noTransactionInclude.buttonGetBitcoin.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onRefreshContactList() {
        transactionAdapter.onContactsMapChanged(
                viewModel.getContactsTransactionMap(),
                viewModel.getNotesTransactionMap());
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(getContext(), getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void showAccountChoiceDialog(List<String> accounts, String fctxId) {
        AppCompatSpinner spinner = new AppCompatSpinner(getActivity());
        spinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, accounts));
        final int[] selection = {0};
        //noinspection AnonymousInnerClassMayBeStatic
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selection[0] = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No-op
            }
        });

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.contacts_choose_account_message)
                .setView(ViewUtils.getAlertDialogPaddedView(getContext(), spinner))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> viewModel.onAccountChosen(selection[0], fctxId))
                .create()
                .show();
    }

    @Override
    public void showSendAddressDialog(String fctxId) {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.contacts_send_address_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> viewModel.onAccountChosen(0, fctxId))
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void showWaitingForPaymentDialog() {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.contacts_waiting_for_payment_message)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    @Override
    public void showWaitingForAddressDialog() {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.contacts_waiting_for_address_message)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    @Override
    public void showTransactionDeclineDialog(String fctxId) {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.contacts_decline_pending_transaction)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        viewModel.confirmDeclineTransaction(fctxId))
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void showTransactionCancelDialog(String fctxId) {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.contacts_cancel_pending_transaction)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        viewModel.confirmCancelTransaction(fctxId))
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
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
    public void initiatePayment(String uri, String recipientId, String mdid, String fctxId) {
        if (interactionListener != null) {
            interactionListener.onPaymentInitiated(uri, recipientId, mdid, fctxId);
        }
    }

    @Override
    public void showFctxRequiringAttention(int number) {
        ((MainActivity) getActivity()).setMessagesCount(number);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
    }

    @Override
    public void onScrollToTop() {
        if (getActivity() != null && !getActivity().isFinishing()) {
            binding.rvTransactions.smoothScrollToPosition(0);
        }
    }

    public interface OnFragmentInteractionListener {

        void resetNavigationDrawer();

        void onPaymentInitiated(String uri, String recipientId, String mdid, String fctxId);

    }

    private void initOnboardingPager() {
        if (onboardingPagerAdapter == null) {
            onboardingPagerAdapter = new OnboardingPagerAdapter(getContext());
            binding.noTransactionInclude.onboardingViewpagerLayout.pagerOnboarding.setAdapter(onboardingPagerAdapter);
            notifyLayoutLoaded();
            binding.noTransactionInclude.onboardingViewpagerLayout.pagerOnboarding.addOnPageChangeListener(
                    new ViewPager.OnPageChangeListener() {
                        @Override
                        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                            if (position == viewModel.getOnboardingPages().size()) {
                                binding.noTransactionInclude.onboardingViewpagerLayout.pagerOnboarding.setPagingEnabled(false);
                                viewModel.setOnboardingComplete(true);
                            } else if (position == viewModel.getOnboardingPages().size() - 1) {
                                binding.noTransactionInclude.onboardingCompleteLayout.onboardingLayout.setVisibility(View.VISIBLE);
                                binding.noTransactionInclude.onboardingViewpagerLayout.viewPagerIndicator.setAlpha(1 - positionOffset);
                                binding.noTransactionInclude.onboardingCompleteLayout.onboardingLayout.setAlpha(positionOffset);
                            } else {
                                binding.noTransactionInclude.onboardingViewpagerLayout.viewPagerIndicator.setVisibility(View.VISIBLE);
                                binding.noTransactionInclude.onboardingCompleteLayout.onboardingLayout.setVisibility(View.INVISIBLE);
                                binding.noTransactionInclude.onboardingViewpagerLayout.viewPagerIndicator.setAlpha(1.0f);
                            }
                        }

                        @Override
                        public void onPageSelected(int position) {
                            // No-op
                        }

                        @Override
                        public void onPageScrollStateChanged(int state) {
                            // No-op
                        }
                    });
        } else {
            notifyLayoutLoaded();
        }

        binding.noTransactionInclude.onboardingViewpagerLayout.btnSkipAll.setOnClickListener(v -> {
            binding.noTransactionInclude.framelayoutOnboarding.setVisibility(View.GONE);

            if(binding.announcementView.getVisibility() != View.VISIBLE) {
                binding.noTransactionInclude.buttonGetBitcoin.setVisibility(View.VISIBLE);
            }
            viewModel.setOnboardingComplete(true);
        });

        binding.noTransactionInclude.onboardingCompleteLayout.onboardingClose.setOnClickListener(v -> {
            binding.noTransactionInclude.framelayoutOnboarding.setVisibility(View.GONE);

            if(binding.announcementView.getVisibility() != View.VISIBLE) {
                binding.noTransactionInclude.buttonGetBitcoin.setVisibility(View.VISIBLE);
            }
            viewModel.setOnboardingComplete(true);
        });

        binding.noTransactionInclude.onboardingCompleteLayout.buttonStartOver.setOnClickListener(v -> {
            binding.noTransactionInclude.onboardingViewpagerLayout.onboardingLayout.setVisibility(View.VISIBLE);
            binding.noTransactionInclude.onboardingViewpagerLayout.viewPagerIndicator.setVisibility(View.VISIBLE);
            binding.noTransactionInclude.onboardingCompleteLayout.onboardingLayout.setVisibility(View.INVISIBLE);
            binding.noTransactionInclude.onboardingViewpagerLayout.pagerOnboarding.setCurrentItem(0);
            binding.noTransactionInclude.onboardingViewpagerLayout.pagerOnboarding.setPagingEnabled(true);
            binding.noTransactionInclude.onboardingViewpagerLayout.viewPagerIndicator.setAlpha(1.0f);
            viewModel.setOnboardingComplete(false);
        });
    }

    private void notifyLayoutLoaded() {
        onboardingPagerAdapter.notifyPagesChanged(viewModel.getOnboardingPages());
        binding.noTransactionInclude.onboardingViewpagerLayout.pagerOnboarding.post(() ->
                binding.noTransactionInclude.progressBar.setVisibility(View.GONE));
        binding.noTransactionInclude.onboardingViewpagerLayout.indicator.setViewPager(
                binding.noTransactionInclude.onboardingViewpagerLayout.pagerOnboarding);
    }

    @Override
    public void startBuyActivity() {
        Intent intent = new Intent(MainActivity.ACTION_BUY);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    @Override
    public void startReceiveFragment() {
        Intent intent = new Intent(MainActivity.ACTION_RECEIVE);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    private void setAnnouncement() {

        binding.announcementView.setTitle(R.string.onboarding_available_now);
        binding.announcementView.setContent(R.string.onboarding_buy_details);
        binding.announcementView.setLink(R.string.onboarding_buy_bitcoin);
        binding.announcementView.setImage(R.drawable.vector_wallet_offset);
        binding.announcementView.setEmoji(R.drawable.celebration_emoji);
        binding.announcementView.setLinkOnclickListener(v -> {
            startBuyActivity();
            viewModel.disableAnnouncement();
        });
        binding.announcementView.setCloseOnclickListener(v -> {
            binding.announcementView.setVisibility(View.GONE);
            binding.noTransactionInclude.buttonGetBitcoin.setVisibility(View.VISIBLE);
            viewModel.disableAnnouncement();
        });
    }

    @Override
    public void onShowAnnouncement() {
        binding.announcementView.setVisibility(View.VISIBLE);
        binding.noTransactionInclude.buttonGetBitcoin.setVisibility(View.GONE);
    }

    @Override
    public void onHideAnnouncement() {
        binding.announcementView.setVisibility(View.GONE);
        binding.noTransactionInclude.buttonGetBitcoin.setVisibility(View.VISIBLE);
    }
}