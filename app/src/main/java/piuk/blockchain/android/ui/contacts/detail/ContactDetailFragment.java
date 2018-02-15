package piuk.blockchain.android.ui.contacts.detail;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.LinearLayoutManager;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;

import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentContactDetailBinding;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.balance.adapter.TxFeedAdapter;
import piuk.blockchain.android.ui.balance.adapter.TxFeedClickListener;
import piuk.blockchain.android.ui.base.BaseFragment;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.ViewUtils;
import piuk.blockchain.android.util.annotations.Thunk;


public class ContactDetailFragment extends BaseFragment<ContactDetailView, ContactDetailPresenter>
        implements ContactDetailView {

    private static final String ARGUMENT_CONTACT_ID = "contact_id";

    @Inject ContactDetailPresenter contactDetailPresenter;
    @Thunk TxFeedAdapter balanceAdapter;
    private FragmentContactDetailBinding binding;
    private MaterialProgressDialog progressDialog;
    private OnFragmentInteractionListener listener;

    {
        Injector.getInstance().getPresenterComponent().inject(this);
    }

    public static ContactDetailFragment newInstance(String contactId) {
        ContactDetailFragment fragment = new ContactDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARGUMENT_CONTACT_ID, contactId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_contact_detail, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((ContactDetailActivity) getActivity()).setupToolbar(
                ((ContactDetailActivity) getActivity()).getToolbar(), getString(R.string.transactions));
        onViewReady();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (menu != null) menu.clear();
        inflater.inflate(R.menu.menu_contact_details, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                getPresenter().onDeleteContactClicked();
                return true;
            case R.id.action_rename:
                getPresenter().onRenameContactClicked();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void finishPage() {
        if (listener != null) listener.onFinishPageCalled();
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(getActivity(), getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void showPayOrDeclineDialog(String fctxId, String amount, String name, @Nullable String note) {
        String message;
        if (note != null && !note.isEmpty()) {
            message = getString(R.string.contacts_balance_dialog_description_pr_note, name, amount, note);
        } else {
            message = getString(R.string.contacts_balance_dialog_description_pr_no_note, name, amount);
        }

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.contacts_balance_dialog_payment_requested)
                .setMessage(message)
                .setPositiveButton(
                        R.string.contacts_balance_dialog_accept, (dialogInterface, i) ->
                                getPresenter().onPaymentRequestAccepted(fctxId)
                )
                .setNegativeButton(
                        R.string.contacts_balance_dialog_decline, (dialogInterface, i) ->
                                getPresenter().confirmDeclineTransaction(fctxId)
                )
                .setNeutralButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void showRenameDialog(String name) {
        AppCompatEditText editText = new AppCompatEditText(getActivity());
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editText.setHint(R.string.name);
        int maxLength = 128;
        InputFilter[] fArray = new InputFilter[1];
        fArray[0] = new InputFilter.LengthFilter(maxLength);
        editText.setFilters(fArray);
        editText.setText(name);
        editText.setSelection(name.length());

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.contacts_rename)
                .setView(ViewUtils.getAlertDialogPaddedView(getActivity(), editText))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> getPresenter().onContactRenamed(editText.getText().toString()))
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void showDeleteUserDialog() {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(getString(R.string.contacts_delete) + "?")
                .setMessage(R.string.contacts_delete_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> getPresenter().onDeleteContactConfirmed())
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void showTransactionDeclineDialog(String fctxId) {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.contacts_decline_pending_transaction)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        getPresenter().confirmDeclineTransaction(fctxId))
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
                        getPresenter().confirmCancelTransaction(fctxId))
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void onTransactionsUpdated(List<Object> transactions, boolean isBtc) {
        if (balanceAdapter == null) {
            setUpAdapter(isBtc);
        }

//        balanceAdapter.onContactsMapChanged(getPresenter().getTransactionDisplayMap());
//        balanceAdapter.setItems(transactions);
//        if (!transactions.isEmpty()) {
//            binding.recyclerView.setVisibility(View.VISIBLE);
//            binding.layoutNoTransactions.setVisibility(View.GONE);
//        } else {
//            binding.recyclerView.setVisibility(View.GONE);
//            binding.layoutNoTransactions.setVisibility(View.VISIBLE);
//        }
    }

    private void setUpAdapter(boolean isBtc) {
        String fiatString = getPresenter().getPrefsUtil().getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        int btcFormat = getPresenter().getPrefsUtil().getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        double btcExchangeRate = ExchangeRateFactory.getInstance().getLastBtcPrice(fiatString);

//        balanceAdapter = new TxFeedAdapter(
//                getActivity(),
//                btcExchangeRate,
//                getPresenter().getEthExchangeRate(),
//                isBtc,
//                null,//TODO Check this when dev on Contacts resume
//                new TxFeedClickListener() {
//                    @Override
//                    public void onTransactionClicked(int correctedPosition, int absolutePosition) {
//                        getPresenter().onCompletedTransactionClicked(absolutePosition);
//                    }
//
//                    @Override
//                    public void onValueClicked(boolean isBtc) {
//                        getPresenter().onBtcFormatChanged(isBtc);
//                        balanceAdapter.onViewFormatUpdated(isBtc, btcFormat);
//                    }
//                });

        binding.recyclerView.setAdapter(balanceAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
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

        FrameLayout frameLayout = new FrameLayout(getActivity());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int marginInPixels = (int) ViewUtils.convertDpToPixel(20, getActivity());
        params.setMargins(marginInPixels, 0, marginInPixels, 0);
        frameLayout.addView(spinner, params);

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.contacts_balance_dialog_choose_account_message)
                .setView(frameLayout)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> getPresenter().onAccountChosen(selection[0], fctxId))
                .setNegativeButton(R.string.contacts_balance_dialog_decline,
                        (dialogInterface, i) -> getPresenter().declineTransaction(fctxId))
                .setNeutralButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void showSendAddressDialog(String fctxId) {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.contacts_send_address_message)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> getPresenter().onAccountChosen(0, fctxId))
                .setNegativeButton(android.R.string.cancel,
                        (dialogInterface, i) -> getPresenter().declineTransaction(fctxId))
                .setNeutralButton(android.R.string.cancel, null)
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
    public void showTransactionDetail(String txHash) {
        if (listener != null) listener.onShowTransactionDetailCalled(txHash);
    }

    // FIXME: 03/08/2017 This is currently broken because of onResume issues on MainActivity, presumably
    // ¯\_(ツ)_/¯
    // TODO: 03/08/2017 Fix me before an actual release
    @Override
    public void initiatePayment(String uri, String recipientId, String mdid, String fctxId) {
        if (listener != null) {
            listener.onPaymentInitiated(uri, recipientId, mdid, fctxId);
        }
    }

    @Override
    public void updateContactName(String name) {
        binding.textNoTransactionsHelper.setText(getString(R.string.contacts_transaction_helper_text_1, name));
    }

    @Override
    public Bundle getPageBundle() {
        return getArguments();
    }

    @Override
    protected ContactDetailPresenter createPresenter() {
        return contactDetailPresenter;
    }

    @Override
    protected ContactDetailView getMvpView() {
        return this;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    interface OnFragmentInteractionListener {

        void onFinishPageCalled();

        void onPaymentInitiated(String uri, String recipientId, String mdid, String fctxId);

        void onShowTransactionDetailCalled(String hash);

    }
}
