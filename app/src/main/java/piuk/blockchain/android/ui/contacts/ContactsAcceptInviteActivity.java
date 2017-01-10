package piuk.blockchain.android.ui.contacts;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityContactsAcceptInviteBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.zxing.CaptureActivity;
import piuk.blockchain.android.util.PermissionUtil;


public class ContactsAcceptInviteActivity extends BaseAuthActivity {

    public static final int SCAN_URI = 2007;

    private ActivityContactsAcceptInviteBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contacts_accept_invite);

        binding.toolbar.toolbarGeneral.setTitle(R.string.contacts_accept_invite_title);
        setSupportActionBar(binding.toolbar.toolbarGeneral);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.buttonScanQr.setOnClickListener(v -> requestScanActivity());

        binding.buttonSentLink.setOnClickListener(v -> {
            // TODO: 10/01/2017 Dialog explaining what to do
        });
    }

    public static void start(Context context) {
        Intent starter = new Intent(context, ContactsAcceptInviteActivity.class);
        context.startActivity(starter);
    }

//  @Override
    public void onShareIntentGenerated(Intent intent) {
        startActivity(Intent.createChooser(intent, getString(R.string.contacts_share_invitation)));
    }

//    @Override
    public void onShowToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK
                && requestCode == SCAN_URI
                && data != null
                && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {

            String extra = data.getStringExtra(CaptureActivity.SCAN_RESULT);
//            if (extra != null) viewModel.handleScanInput(extra);

        } else if (resultCode != RESULT_CANCELED && requestCode == SCAN_URI) {
            onShowToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
        }
    }

    private void requestScanActivity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            PermissionUtil.requestCameraPermissionFromActivity(binding.getRoot(), this);
        } else {
            startScanActivity();
        }
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

//    @Override
    public void finishActivityWithResult(int resultCode) {
        setResult(resultCode);
        finish();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

//    @Override
    public Intent getPageIntent() {
        return getIntent();
    }

    private void startScanActivity() {
//        if (!viewModel.isCameraOpen()) {
//            Intent intent = new Intent(this, CaptureActivity.class);
//            startActivityForResult(intent, SCAN_URI);
//        } else {
//            ToastCustom.makeText(this, getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
//        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
