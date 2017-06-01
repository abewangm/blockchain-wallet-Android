package piuk.blockchain.android.ui.web_pair;

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
import piuk.blockchain.android.databinding.ActivityWebPairBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.ToastCustom;

public class WebPairActivity extends BaseAuthActivity implements WebPairViewModel.DataListener {

    private final String TAG = getClass().getName();

    WebPairViewModel viewModel;
    ActivityWebPairBinding binding;

    public static void start(Context context) {
        Intent intent = new Intent(context, WebPairActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_web_pair);
        viewModel = new WebPairViewModel(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        setupToolbar(toolbar, R.string.web_pair_log_in);

        binding.pairingFirstStep.setText(viewModel.getFirstStep());

        viewModel.onViewReady();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void onClickQRToggle(View view) {

        if(binding.mainLayout.getVisibility() == View.VISIBLE) {

            //Show pairing QR
            binding.mainLayout.setVisibility(View.GONE);
            binding.btnQrToggle.setText(R.string.web_pair_hide_qr);

            binding.qrLayout.setVisibility(View.VISIBLE);
            binding.ivQr.setVisibility(View.GONE);

            viewModel.generatePairingQr();
        } else {

            //Hide pairing QR
            binding.tvWarning.setText(R.string.web_pair_warning_1);
            binding.mainLayout.setVisibility(View.VISIBLE);
            binding.btnQrToggle.setText(R.string.web_pair_show_qr);

            binding.qrLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onQrLoaded(Bitmap bitmap) {
        binding.tvWarning.setText(R.string.web_pair_warning_2);
        binding.progressBar.setVisibility(View.GONE);
        binding.ivQr.setVisibility(View.VISIBLE);

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = (width*bitmap.getHeight())/bitmap.getWidth();
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
}
