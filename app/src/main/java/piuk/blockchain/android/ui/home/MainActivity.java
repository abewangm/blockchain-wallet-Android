package piuk.blockchain.android.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutManager;
import android.databinding.DataBindingUtil;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.aurelhubert.ahbottomnavigation.notification.AHNotification;

import info.blockchain.wallet.payload.PayloadManager;

import java.util.Arrays;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.contacts.PaymentRequestType;
import piuk.blockchain.android.databinding.ActivityMainBinding;
import piuk.blockchain.android.ui.account.AccountActivity;
import piuk.blockchain.android.ui.auth.LandingActivity;
import piuk.blockchain.android.ui.auth.PinEntryActivity;
import piuk.blockchain.android.ui.backup.BackupWalletActivity;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.contacts.list.ContactsListActivity;
import piuk.blockchain.android.ui.contacts.payments.ContactPaymentRequestNotesFragment;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.launcher.LauncherActivity;
import piuk.blockchain.android.ui.receive.ReceiveFragment;
import piuk.blockchain.android.ui.send.SendFragment;
import piuk.blockchain.android.ui.settings.SettingsActivity;
import piuk.blockchain.android.ui.shortcuts.LauncherShortcutHelper;
import piuk.blockchain.android.ui.upgrade.UpgradeWalletActivity;
import piuk.blockchain.android.ui.zxing.CaptureActivity;
import piuk.blockchain.android.util.AndroidUtils;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.EventLogHandler;
import piuk.blockchain.android.util.PermissionUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.ViewUtils;
import piuk.blockchain.android.util.annotations.Thunk;

import static piuk.blockchain.android.ui.contacts.list.ContactsListActivity.EXTRA_METADATA_URI;
import static piuk.blockchain.android.ui.settings.SettingsFragment.EXTRA_SHOW_ADD_EMAIL_DIALOG;

public class MainActivity extends BaseAuthActivity implements BalanceFragment.OnFragmentInteractionListener,
        MainViewModel.DataListener,
        SendFragment.OnSendFragmentInteractionListener,
        ReceiveFragment.OnReceiveFragmentInteractionListener,
        ContactPaymentRequestNotesFragment.FragmentInteractionListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String SUPPORT_URI = "http://support.blockchain.com/";
    private static final int REQUEST_BACKUP = 2225;
    private static final int MERCHANT_ACTIVITY = 1;
    public static final int SCAN_URI = 2007;
    private static final int COOL_DOWN_MILLIS = 2 * 1000;

    @Thunk boolean drawerIsOpen = false;

    private MainViewModel viewModel;
    private ActivityMainBinding binding;
    private MaterialProgressDialog fetchTransactionsProgress;
    private AlertDialog rootedDialog;
    private MaterialProgressDialog materialProgressDialog;
    private AppUtil appUtil;
    private long backPressed;
    private Toolbar toolbar;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        appUtil = new AppUtil(this);
        viewModel = new MainViewModel(this);
        viewModel.onViewReady();

        binding.drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                // No-op
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                drawerIsOpen = true;
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                drawerIsOpen = false;
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                // No-op
            }
        });

        // Set up toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        toolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.vector_menu));
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        ViewUtils.setElevation(toolbar, 0F);

        // Create items
        AHBottomNavigationItem item1 = new AHBottomNavigationItem(R.string.send_bitcoin, R.drawable.vector_send, R.color.blockchain_pearl_white);
        AHBottomNavigationItem item2 = new AHBottomNavigationItem(R.string.transactions, R.drawable.vector_transactions, R.color.blockchain_pearl_white);
        AHBottomNavigationItem item3 = new AHBottomNavigationItem(R.string.receive_bitcoin, R.drawable.vector_receive, R.color.blockchain_pearl_white);

        // Add items
        binding.bottomNavigation.addItems(Arrays.asList(item1, item2, item3));

        // Styling
        binding.bottomNavigation.setAccentColor(ContextCompat.getColor(this, R.color.blockchain_blue));
        binding.bottomNavigation.setInactiveColor(ContextCompat.getColor(this, R.color.blockchain_grey));
        binding.bottomNavigation.setForceTint(true);
        binding.bottomNavigation.setUseElevation(true);

        // Select transactions by default
        binding.bottomNavigation.setCurrentItem(1);
        binding.bottomNavigation.setOnTabSelectedListener((position, wasSelected) -> {
            if (!wasSelected) {
                switch (position) {
                    case 0:
                        if (!(getCurrentFragment() instanceof SendFragment)) {
                            // This is a bit of a hack to allow the selection of the correct button
                            // On the bottom nav bar, but without starting the fragment again
                            startSendFragment(null, null);
                        }
                        break;
                    case 1:
                        onStartBalanceFragment();
                        break;
                    case 2:
                        startReceiveFragment();
                        break;
                }
            } else {
                if (position == 1 && getCurrentFragment() instanceof BalanceFragment) {
                    ((BalanceFragment) getCurrentFragment()).onScrollToTop();
                }
            }

            return true;
        });
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        appUtil.deleteQR();
        viewModel.storeSwipeReceiveAddresses();
        resetNavigationDrawer();

        if (AndroidUtils.is25orHigher() && viewModel.areLauncherShortcutsEnabled()) {
            LauncherShortcutHelper launcherShortcutHelper = new LauncherShortcutHelper(
                    this,
                    PayloadManager.getInstance(),
                    getSystemService(ShortcutManager.class));

            launcherShortcutHelper.generateReceiveShortcuts();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                binding.drawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_qr_main:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    PermissionUtil.requestCameraPermissionFromActivity(binding.getRoot(), this);
                } else {
                    startScanActivity();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setMessagesCount(int messageCount) {
        binding.navigationView.getMenu()
                .findItem(R.id.nav_contacts)
                .getActionView()
                .findViewById(R.id.menu_icon_count)
                .setVisibility(messageCount);

        if (messageCount > 0) {
            AHNotification notification = new AHNotification.Builder()
                    .setText(String.valueOf(messageCount))
                    .setBackgroundColor(ContextCompat.getColor(this, R.color.alert_red))
                    .setTextColor(ContextCompat.getColor(this, R.color.white))
                    .build();
            binding.bottomNavigation.setNotification(notification, 1);
        } else {
            binding.bottomNavigation.setNotification(new AHNotification(), 1);
        }
    }

    @Thunk
    Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.content_frame);
    }

    public boolean getDrawerOpen() {
        return drawerIsOpen;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == SCAN_URI
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
            String strResult = data.getStringExtra(CaptureActivity.SCAN_RESULT);
            doScanInput(strResult, EventLogHandler.URL_EVENT_TX_INPUT_FROM_QR);

        } else if (resultCode == RESULT_OK && requestCode == REQUEST_BACKUP) {
            resetNavigationDrawer();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerIsOpen) {
            binding.drawerLayout.closeDrawers();
        } else if (getCurrentFragment() instanceof BalanceFragment) {
            handleBackPressed();
        } else if (getCurrentFragment() instanceof SendFragment) {
            ((SendFragment) getCurrentFragment()).onBackPressed();
        } else if (getCurrentFragment() instanceof ReceiveFragment) {
            ((ReceiveFragment) getCurrentFragment()).onBackPressed();
        } else if (getCurrentFragment() instanceof ContactPaymentRequestNotesFragment) {
            // Remove Notes fragment from stack
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().remove(getCurrentFragment()).commit();
        } else {
            // Switch to balance fragment
            BalanceFragment fragment = new BalanceFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        }
    }

    public void handleBackPressed() {
        if (backPressed + COOL_DOWN_MILLIS > System.currentTimeMillis()) {
            AccessState.getInstance().logout(this);
            return;
        } else {
            onExitConfirmToast();
        }

        backPressed = System.currentTimeMillis();
    }

    public void onExitConfirmToast() {
        ToastCustom.makeText(getActivity(), getString(R.string.exit_confirm), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
    }

    private void startScanActivity() {
        if (!appUtil.isCameraOpen()) {
            Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
            startActivityForResult(intent, SCAN_URI);
        } else {
            ToastCustom.makeText(MainActivity.this, getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
    }

    private void doScanInput(String strResult, String scanRoute) {
        startSendFragment(strResult, scanRoute);
    }

    public void selectDrawerItem(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_backup:
                startActivityForResult(new Intent(MainActivity.this, BackupWalletActivity.class), REQUEST_BACKUP);
                break;
            case R.id.nav_addresses:
                startActivity(new Intent(MainActivity.this, AccountActivity.class));
                break;
            case R.id.nav_contacts:
                ContactsListActivity.start(this, null);
                break;
            case R.id.nav_upgrade:
                startActivity(new Intent(MainActivity.this, UpgradeWalletActivity.class));
                break;
            case R.id.nav_map:
                startMerchantActivity();
                break;
            case R.id.nav_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
            case R.id.nav_support:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_URI)));
                break;
            case R.id.nav_logout:
                new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                        .setTitle(R.string.unpair_wallet)
                        .setMessage(R.string.ask_you_sure_unpair)
                        .setPositiveButton(R.string.unpair, (dialog, which) -> viewModel.unpair())
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                break;
        }
        binding.drawerLayout.closeDrawers();
    }

    @Override
    public void resetNavigationDrawer() {
        // Called onResume from BalanceFragment
        toolbar.setTitle("");
        MenuItem backUpMenuItem = binding.navigationView.getMenu().findItem(R.id.nav_backup);
        MenuItem upgradeMenuItem = binding.navigationView.getMenu().findItem(R.id.nav_upgrade);

        if (viewModel.getPayloadManager().isNotUpgraded()) {
            //Legacy
            upgradeMenuItem.setVisible(true);
            backUpMenuItem.setVisible(false);
        } else {
            //HD
            upgradeMenuItem.setVisible(false);
            backUpMenuItem.setVisible(true);
        }

        MenuItem backUpView = binding.navigationView.getMenu().findItem(R.id.nav_backup);
        Drawable drawable = backUpView.getIcon();
        drawable.mutate();
        if (viewModel.getPayloadManager().getPayload() != null &&
                viewModel.getPayloadManager().getPayload().getHdWallet() != null &&
                !viewModel.getPayloadManager().getPayload().getHdWallet().isMnemonicVerified()) {
            //Not backed up
            drawable.setColorFilter(ContextCompat.getColor(this, R.color.blockchain_send_red), PorterDuff.Mode.SRC_ATOP);
        } else {
            //Backed up
            drawable.setColorFilter(ContextCompat.getColor(this, R.color.alert_green), PorterDuff.Mode.SRC_ATOP);
        }

        binding.navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    selectDrawerItem(menuItem);
                    return true;
                });

        // Set selected appropriately.
        if (getCurrentFragment() instanceof BalanceFragment) {
            binding.bottomNavigation.setCurrentItem(1);
        } else if (getCurrentFragment() instanceof SendFragment) {
            binding.bottomNavigation.setCurrentItem(0);
        } else if (getCurrentFragment() instanceof ReceiveFragment) {
            binding.bottomNavigation.setCurrentItem(2);
        }
    }

    private void startMerchantActivity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            PermissionUtil.requestLocationPermissionFromActivity(binding.getRoot(), this);
        } else {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            boolean enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (!enabled) {
                EnableGeo.displayGPSPrompt(this);
            } else {
                Intent intent = new Intent(MainActivity.this, piuk.blockchain.android.ui.directory.MapActivity.class);
                startActivityForResult(intent, MERCHANT_ACTIVITY);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (rootedDialog != null && rootedDialog.isShowing()) {
            rootedDialog.dismiss();
        }
    }

    private void startSingleActivity(Class clazz) {
        Intent intent = new Intent(MainActivity.this, clazz);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
        }
        if (requestCode == PermissionUtil.PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                startMerchantActivity();
            } else {
                // Permission request was denied.
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onRooted() {
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            if (!isFinishing()) {
                rootedDialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                        .setMessage(getString(R.string.device_rooted))
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_continue, null)
                        .create();

                rootedDialog.show();
            }
        }, 500);
    }

    private Context getActivity() {
        return this;
    }

    @Override
    public void showEmailVerificationDialog(String email) {
        String message = String.format(getString(R.string.security_centre_email_message), email);
        SecurityPromptDialog securityPromptDialog = SecurityPromptDialog.newInstance(
                R.string.security_centre_email_title,
                message,
                R.drawable.vector_email,
                android.R.string.ok);
        securityPromptDialog.showDialog(getSupportFragmentManager());
        securityPromptDialog.setPositiveButtonListener(v -> securityPromptDialog.dismiss());
    }

    @Override
    public void showAddEmailDialog() {
        String message = getString(R.string.security_centre_add_email_message);
        SecurityPromptDialog securityPromptDialog = SecurityPromptDialog.newInstance(
                R.string.security_centre_add_email_title,
                message,
                R.drawable.vector_email,
                R.string.security_centre_add_email_positive_button,
                true,
                false);
        securityPromptDialog.showDialog(getSupportFragmentManager());
        securityPromptDialog.setPositiveButtonListener(v -> {
            securityPromptDialog.dismiss();
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra(EXTRA_SHOW_ADD_EMAIL_DIALOG, true);
            startActivity(intent);
        });
        securityPromptDialog.setNegativeButtonListener(view -> securityPromptDialog.dismiss());
    }

    @Override
    public void showSurveyPrompt() {
        if (!isFinishing()) {
            new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.survey_message)
                    .setPositiveButton(R.string.survey_positive_button, (dialog, which) -> {
                        String url = "https://blockchain.co1.qualtrics.com/SE/?SID=SV_bQ8rW6DErUEzMeV";
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                    })
                    .setNegativeButton(R.string.polite_no, null)
                    .create()
                    .show();
        }
    }

    @Override
    public void showContactsRegistrationFailure() {
        if (!isFinishing()) {
            new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.contacts_register_nodes_failure)
                    .setPositiveButton(R.string.retry, (dialog, which) -> viewModel.checkForMessages())
                    .create()
                    .show();
        }
    }

    @Override
    public void onConnectivityFail() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogStyle);
        final String message = getString(R.string.check_connectivity_exit);
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_continue,
                        (d, id) -> {
                            d.dismiss();
                            Class c = null;
                            if (new PrefsUtil(MainActivity.this).getValue(PrefsUtil.KEY_GUID, "").length() < 1) {
                                c = LandingActivity.class;
                            } else {
                                c = PinEntryActivity.class;
                            }
                            startSingleActivity(c);
                        });

        if (!isFinishing()) {
            builder.create().show();
        }
    }

    @Override
    public void onPaymentInitiated(String uri, String recipientId, String mdid, String fctxId, int defaultIndex) {
        SendFragment sendFragment = SendFragment.newInstance(uri, recipientId, mdid, fctxId, null, defaultIndex);
        addFragment(sendFragment);
    }

    @Override
    public void kickToLauncherPage() {
        startSingleActivity(LauncherActivity.class);
    }

    @Override
    public void onFetchTransactionsStart() {
        fetchTransactionsProgress = new MaterialProgressDialog(this);
        fetchTransactionsProgress.setCancelable(false);
        fetchTransactionsProgress.setMessage(getString(R.string.please_wait));
        fetchTransactionsProgress.show();
    }

    @Override
    public void onFetchTransactionCompleted() {
        if (fetchTransactionsProgress != null && fetchTransactionsProgress.isShowing()) {
            fetchTransactionsProgress.dismiss();
        }
    }

    @Override
    public void onStartContactsActivity(@Nullable String data) {
        if (data != null) {
            Bundle bundle = new Bundle();
            bundle.putString(EXTRA_METADATA_URI, data);
            ContactsListActivity.start(this, bundle);
        } else {
            ContactsListActivity.start(this, null);
        }
    }

    @Override
    public void showProgressDialog(@StringRes int message) {
        materialProgressDialog = new MaterialProgressDialog(this);
        materialProgressDialog.setCancelable(false);
        materialProgressDialog.setMessage(message);
        materialProgressDialog.show();
    }

    @Override
    public void hideProgressDialog() {
        if (materialProgressDialog != null) {
            materialProgressDialog.dismiss();
            materialProgressDialog = null;
        }
    }

    @Override
    public void onScanInput(String strUri) {
        doScanInput(strUri, EventLogHandler.URL_EVENT_TX_INPUT_FROM_URI);
    }

    @Override
    public void onStartBalanceFragment() {
        BalanceFragment fragment = new BalanceFragment();
        replaceFragmentWithAnimation(fragment);
        viewModel.checkIfShouldShowSurvey();
    }

    public void startSendFragment(@Nullable String scanData, String scanRoute) {
        int selectedAccountPosition;
        if (getCurrentFragment() instanceof BalanceFragment) {
            selectedAccountPosition = ((BalanceFragment) getCurrentFragment()).getSelectedAccountPosition();
        } else if (getCurrentFragment() instanceof ReceiveFragment) {
            selectedAccountPosition = ((ReceiveFragment) getCurrentFragment()).getSelectedAccountPosition();
        } else {
            selectedAccountPosition = -1;
        }

        SendFragment sendFragment = SendFragment.newInstance(scanData, scanRoute, selectedAccountPosition);
        replaceFragmentWithAnimation(sendFragment);
    }

    public void startReceiveFragment() {
        int selectedAccountPosition;
        try {
            selectedAccountPosition = ((BalanceFragment) getCurrentFragment()).getSelectedAccountPosition();
        } catch (ClassCastException e) {
            Log.e(TAG, "startReceiveFragment: ", e);
            selectedAccountPosition = -1;
        }

        ReceiveFragment receiveFragment = ReceiveFragment.newInstance(selectedAccountPosition);
        replaceFragmentWithAnimation(receiveFragment);
    }

    private void replaceFragmentWithAnimation(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        transaction.replace(R.id.content_frame, fragment).commitAllowingStateLoss();
    }

    private void addFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.content_frame, fragment).commitAllowingStateLoss();
    }

    public AHBottomNavigation getBottomNavigationView() {
        return binding.bottomNavigation;
    }

    @Override
    public void clearAllDynamicShortcuts() {
        if (AndroidUtils.is25orHigher()) {
            getSystemService(ShortcutManager.class).removeAllDynamicShortcuts();
        }
    }

    @Override
    public void showBroadcastFailedDialog(String mdid, String txHash, String facilitatedTxId, long transactionValue) {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.contacts_payment_sent_failed_message)
                .setPositiveButton(R.string.retry, (dialog, which) ->
                        viewModel.broadcastPaymentSuccess(mdid, txHash, facilitatedTxId, transactionValue))
                .setCancelable(false)
                .create()
                .show();
    }

    @Override
    public void showPaymentMismatchDialog(@StringRes int message) {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    @Override
    public void showBroadcastSuccessDialog() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.contacts_payment_sent_success)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    @Override
    public void onSendFragmentClose() {
        binding.bottomNavigation.setCurrentItem(1);
    }

    // Ensure bottom nav button selected after scanning for result
    @Override
    public void onSendFragmentStart() {
        binding.bottomNavigation.setCurrentItem(0);
    }

    @Override
    public void onReceiveFragmentClose() {
        binding.bottomNavigation.setCurrentItem(1);
    }

    @Override
    public void onSendPaymentSuccessful(@Nullable String mdid, String transactionHash, @Nullable String fctxId, long transactionValue) {
        viewModel.broadcastPaymentSuccess(mdid, transactionHash, fctxId, transactionValue);
    }

    @Override
    public void onTransactionNotesRequested(String contactId, @Nullable Integer accountPosition, PaymentRequestType paymentRequestType, long satoshis) {
        addFragment(ContactPaymentRequestNotesFragment.newInstance(paymentRequestType, accountPosition, contactId, satoshis));
    }

    @Override
    public void onPageFinished() {
        onStartBalanceFragment();
    }

}
