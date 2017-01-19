package piuk.blockchain.android.ui.contacts.payments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;

import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.util.CharSequenceX;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.ContactsPredicates;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.receive.WalletAccountHelper;
import piuk.blockchain.android.ui.send.PendingTransaction;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;
import piuk.blockchain.android.util.annotations.Thunk;


@SuppressWarnings("WeakerAccess")
public class SendDialogViewModel extends BaseViewModel {

    @Thunk DataListener dataListener;
    @Inject WalletAccountHelper walletAccountHelper;
    @Inject TransferFundsDataManager fundsDataManager;
    @Inject PayloadManager payloadManager;
    @Inject PrefsUtil prefsUtil;
    @Inject StringUtils stringUtils;
    @Inject ExchangeRateFactory exchangeRateFactory;
    @Inject ContactsDataManager contactsDataManager;
    @VisibleForTesting List<PendingTransaction> pendingTransactions;

    public interface DataListener {

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void updateToLabel(String label);

        void updateTransferAmountBtc(String amount);

        void updateTransferAmountFiat(String amount);

        void updateFeeAmountBtc(String amount);

        void updateFeeAmountFiat(String amount);

        void dismissDialog();

        void setPaymentButtonEnabled(boolean enabled);

        void showProgressDialog();

        void hideProgressDialog();

        void onUiUpdated();

        Bundle getBundle();
    }

    public SendDialogViewModel(DataListener listener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        dataListener = listener;
    }

    @Override
    public void onViewReady() {
        String uri = dataListener.getBundle().getString(SendDialogFragment.KEY_BUNDLE_URI);
        String recipientId = dataListener.getBundle().getString(SendDialogFragment.KEY_BUNDLE_CONTACT_ID);
        if (uri == null || recipientId == null) {
            dataListener.showToast(R.string.contacts_not_found_error, ToastCustom.TYPE_ERROR);
            dataListener.dismissDialog();
            return;
        }

        updateToAddress(payloadManager.getPayload().getHdWallet().getDefaultIndex());

        compositeDisposable.add(
                contactsDataManager.getContactList()
                        .filter(ContactsPredicates.filterById(recipientId))
                        .subscribe(
                                contact -> dataListener.updateToLabel(contact.getName()),
                                throwable -> {
                                    dataListener.showToast(R.string.contacts_not_found_error, ToastCustom.TYPE_ERROR);
                                    dataListener.dismissDialog();
                                }));
    }

    public void accountSelected(int position) {
        updateToAddress(getAdjustedAccountPosition(position));
    }

    private void updateToAddress(int indexOfReceiveAccount) {
        dataListener.setPaymentButtonEnabled(false);
        compositeDisposable.add(
                fundsDataManager.getTransferableFundTransactionList(indexOfReceiveAccount)
                        .subscribe(triple -> {
                            pendingTransactions = triple.getLeft();
                            updateUi(triple.getMiddle(), triple.getRight());

                        }, throwable -> {
                            dataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                            dataListener.dismissDialog();
                        }));
    }

    @VisibleForTesting
    void updateUi(long totalToSend, long totalFee) {

        MonetaryUtil monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        String fiatUnit = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        String btcUnit = monetaryUtil.getBTCUnit(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        double exchangeRate = exchangeRateFactory.getLastPrice(fiatUnit);

        String fiatAmount = monetaryUtil.getFiatFormat(fiatUnit).format(exchangeRate * ((double) totalToSend / 1e8));
        String fiatFee = monetaryUtil.getFiatFormat(fiatUnit).format(exchangeRate * ((double) totalFee / 1e8));

        dataListener.updateTransferAmountBtc(
                monetaryUtil.getDisplayAmountWithFormatting(totalToSend)
                        + " "
                        + btcUnit);
        dataListener.updateTransferAmountFiat(
                exchangeRateFactory.getSymbol(fiatUnit)
                        + fiatAmount);

        dataListener.updateFeeAmountBtc(
                monetaryUtil.getDisplayAmountWithFormatting(totalFee)
                        + " "
                        + btcUnit);
        dataListener.updateFeeAmountFiat(
                exchangeRateFactory.getSymbol(fiatUnit)
                        + fiatFee);

        dataListener.setPaymentButtonEnabled(true);

        dataListener.onUiUpdated();
    }

    /**
     * Transacts all {@link PendingTransaction} objects
     *
     * @param secondPassword The user's double encryption password if necessary
     */
    public void sendPayment(@Nullable CharSequenceX secondPassword) {
        dataListener.setPaymentButtonEnabled(false);
        dataListener.showProgressDialog();
        compositeDisposable.add(
                fundsDataManager.sendPayment(new Payment(), pendingTransactions, secondPassword)
                        .subscribe(s -> {
                            dataListener.hideProgressDialog();
                            dataListener.showToast(R.string.transfer_confirmed, ToastCustom.TYPE_OK);
                            dataListener.dismissDialog();
                        }, throwable -> {
                            dataListener.hideProgressDialog();
                            dataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                            dataListener.dismissDialog();
                        }));
    }

    /**
     * Returns only HD Accounts as we want to move funds to a backed up place
     *
     * @return {@link List<ItemAccount>}
     */
    public List<ItemAccount> getSendFromList() {
        return new ArrayList<ItemAccount>() {{
            addAll(walletAccountHelper.getHdAccounts(true));
        }};
    }

    /**
     * Get corrected default account position
     *
     * @return int account position in list of non-archived accounts
     */
    public int getDefaultAccount() {
        return Math.max(getCorrectedAccountIndex(payloadManager.getPayload().getHdWallet().getDefaultIndex()), 0);
    }

    private int getAdjustedAccountPosition(int position) {
        List<Account> accounts = payloadManager.getPayload().getHdWallet().getAccounts();
        int adjustedPosition = 0;
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            if (!account.isArchived()) {
                if (position == adjustedPosition) {
                    return i;
                }
                adjustedPosition++;
            }
        }

        return 0;
    }

    private int getCorrectedAccountIndex(int accountIndex) {
        // Filter accounts by active
        List<Account> activeAccounts = new ArrayList<>();
        List<Account> accounts = payloadManager.getPayload().getHdWallet().getAccounts();
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            if (!account.isArchived()) {
                activeAccounts.add(account);
            }
        }

        // Find corrected position
        return activeAccounts.indexOf(payloadManager.getPayload().getHdWallet().getAccounts().get(accountIndex));
    }
}

