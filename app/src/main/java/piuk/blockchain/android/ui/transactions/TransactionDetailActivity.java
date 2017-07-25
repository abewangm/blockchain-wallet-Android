package piuk.blockchain.android.ui.transactions;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import info.blockchain.wallet.multiaddress.TransactionSummary.Direction;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityTransactionDetailsBinding;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseMvpActivity;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.receive.RecipientAdapter;
import piuk.blockchain.android.util.ViewUtils;

public class TransactionDetailActivity extends BaseMvpActivity<TransactionDetailView, TransactionDetailPresenter>
        implements TransactionDetailView {

    @Inject TransactionDetailPresenter transactionDetailPresenter;
    private ActivityTransactionDetailsBinding binding;

    {
        Injector.getInstance().getPresenterComponent().inject(this);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_transaction_details);

        Toolbar toolbar = findViewById(R.id.toolbar_general);
        setupToolbar(toolbar, R.string.transaction_detail_title);

        binding.editIcon.setOnClickListener(v -> binding.descriptionField.performClick());
        binding.descriptionField.setOnClickListener(v -> {
            AppCompatEditText editText = new AppCompatEditText(this);
            editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
                    | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE);
            editText.setHint(R.string.transaction_detail_description_hint);

            int maxLength = 256;
            InputFilter[] fArray = new InputFilter[1];
            fArray[0] = new InputFilter.LengthFilter(maxLength);
            editText.setFilters(fArray);
            editText.setText(getPresenter().getTransactionNote());
            editText.setSelection(editText.getText().length());

            new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setTitle(R.string.app_name)
                    .setView(ViewUtils.getAlertDialogPaddedView(this, editText))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        getPresenter().updateTransactionNote(editText.getText().toString());
                        setDescription(editText.getText().toString());
                    })
                    .show();
        });

        onViewReady();
    }

    public static void start(Context context, Bundle args) {
        Intent starter = new Intent(context, TransactionDetailActivity.class);
        starter.putExtras(args);
        context.startActivity(starter);
    }

    @Override
    public void setTransactionType(@NonNull Direction type) {
        switch (type) {
            case TRANSFERRED:
                binding.transactionType.setText(getResources().getString(R.string.MOVED));
                break;
            case RECEIVED:
                binding.transactionType.setText(getResources().getString(R.string.RECEIVED));
                binding.transactionFee.setVisibility(View.GONE);
                break;
            case SENT:
                binding.transactionType.setText(getResources().getString(R.string.SENT));
                break;
        }
    }

    @Override
    public void onDataLoaded() {
        binding.mainLayout.setVisibility(View.VISIBLE);
        binding.loadingLayout.setVisibility(View.GONE);
    }

    @Override
    public void setTransactionColour(@ColorRes int colour) {
        binding.transactionAmount.setTextColor(ResourcesCompat.getColor(getResources(), colour, getTheme()));
        binding.transactionType.setTextColor(ResourcesCompat.getColor(getResources(), colour, getTheme()));
    }

    @Override
    public void setTransactionNote(String note) {
        binding.transactionNote.setText(note);
        binding.transactionNoteLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void setTransactionValueBtc(String value) {
        binding.transactionAmount.setText(value);
    }

    @Override
    public void setTransactionValueFiat(String fiat) {
        binding.transactionValue.setText(fiat);
    }

    @Override
    public void setToAddresses(@NotNull List<? extends RecipientModel> addresses) {
        if (addresses.size() == 1) {
            binding.toAddress.setText(addresses.get(0).getAddress());
        } else {
            binding.spinner.setVisibility(View.VISIBLE);
            RecipientAdapter adapter = new RecipientAdapter(new ArrayList<>(addresses));
            binding.toAddress.setText(String.format(Locale.getDefault(), "%1s Recipients", addresses.size()));
            binding.toAddress.setOnClickListener(v -> binding.spinner.performClick());
            binding.spinner.setAdapter(adapter);
            binding.spinner.setOnItemSelectedListener(null);
        }
    }

    @Override
    public void setDate(String date) {
        binding.date.setText(date);
    }

    @Override
    public void setDescription(String description) {
        binding.descriptionField.setText(description);
    }

    @Override
    public void setFromAddress(String address) {
        binding.fromAddress.setText(address);
    }

    @Override
    public void setStatus(String status, String hash) {
        binding.status.setText(status);
        binding.buttonVerify.setOnClickListener(v -> {
            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setData(Uri.parse("https://blockchain.info/tx/" + getPresenter().getTransactionHash()));
            startActivity(viewIntent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_transaction_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, "https://blockchain.info/tx/" + getPresenter().getTransactionHash());
                shareIntent.setType("text/plain");
                startActivity(Intent.createChooser(shareIntent, getString(R.string.transaction_detail_share_chooser)));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void setIsDoubleSpend(boolean isDoubleSpend) {
        if (isDoubleSpend) {
            binding.doubleSpendWarning.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void showToast(@StringRes int message, @NonNull @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void setFee(String fee) {
        binding.transactionFee.setText(String.format(Locale.getDefault(), getString(R.string.transaction_detail_fee), fee));
    }

    @Override
    public void pageFinish() {
        finish();
    }

    @Override
    public Intent getPageIntent() {
        return getIntent();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected TransactionDetailPresenter createPresenter() {
        return transactionDetailPresenter;
    }

    @Override
    protected TransactionDetailView getView() {
        return this;
    }

}
