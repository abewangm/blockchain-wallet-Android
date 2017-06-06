package piuk.blockchain.android.ui.pairing_code;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityPairingCodeBinding;
import piuk.blockchain.android.ui.base.BaseMvpActivity;
import piuk.blockchain.android.ui.customviews.ToastCustom;

public class PairingCodeActivity extends BaseMvpActivity<PairingCodeView, PairingCodePresenter>
        implements PairingCodeView {

    private ActivityPairingCodeBinding binding;

    public static void start(Context context) {
        Intent intent = new Intent(context, PairingCodeActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_pairing_code);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        setupToolbar(toolbar, R.string.pairing_code_log_in);

        binding.pairingFirstStep.setText(getPresenter().getFirstStep());

        onViewReady();
    }

    @Override
    protected PairingCodePresenter createPresenter() {
        return new PairingCodePresenter();
    }

    @Override
    protected PairingCodeView getView() {
        return this;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void onClickQRToggle(View view) {

        if (binding.mainLayout.getVisibility() == View.VISIBLE) {

            //Show pairing QR
            binding.mainLayout.setVisibility(View.GONE);
            binding.btnQrToggle.setText(R.string.pairing_code_hide_qr);

            binding.qrLayout.setVisibility(View.VISIBLE);
            binding.ivQr.setVisibility(View.GONE);

            getPresenter().generatePairingQr();
        } else {

            //Hide pairing QR
            binding.tvWarning.setText(R.string.pairing_code_warning_1);
            binding.mainLayout.setVisibility(View.VISIBLE);
            binding.btnQrToggle.setText(R.string.pairing_code_show_qr);

            binding.qrLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onQrLoaded(Bitmap bitmap) {
        binding.tvWarning.setText(R.string.pairing_code_warning_2);
        binding.ivQr.setVisibility(View.VISIBLE);

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = (width * bitmap.getHeight()) / bitmap.getWidth();
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);

        binding.ivQr.setImageBitmap(bitmap);
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void showProgressSpinner() {
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgressSpinner() {
        binding.progressBar.setVisibility(View.GONE);
    }
}
