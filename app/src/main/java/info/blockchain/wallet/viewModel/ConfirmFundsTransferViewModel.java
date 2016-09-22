package info.blockchain.wallet.viewModel;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.Pair;

import info.blockchain.wallet.model.ItemAccount;
import info.blockchain.wallet.model.PendingTransaction;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.StringUtils;
import info.blockchain.wallet.view.helpers.ToastCustom;
import info.blockchain.wallet.view.helpers.TransferFundsDataManager;
import info.blockchain.wallet.view.helpers.WalletAccountHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.annotations.Thunk;
import piuk.blockchain.android.di.Injector;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.subscriptions.CompositeSubscription;

@SuppressWarnings("WeakerAccess")
public class ConfirmFundsTransferViewModel implements ViewModel {

    @Thunk DataListener mDataListener;
    @Inject WalletAccountHelper mWalletAccountHelper;
    @Inject TransferFundsDataManager mFundsDataManager;
    @Inject PayloadManager mPayloadManager;
    @Inject PrefsUtil mPrefsUtil;
    @Inject StringUtils mStringUtils;
    @Inject ExchangeRateFactory mExchangeRateFactory;
    @VisibleForTesting List<PendingTransaction> mPendingTransactions;
    @VisibleForTesting CompositeSubscription mCompositeSubscription;

    public interface DataListener {

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void updateFromLabel(String label);

        void updateTransferAmountBtc(String amount);

        void updateTransferAmountFiat(String amount);

        void updateFeeAmountBtc(String amount);

        void updateFeeAmountFiat(String amount);

        void dismissDialog();

        void setPaymentButtonEnabled(boolean enabled);

        boolean getIfArchiveChecked();

        void showProgressDialog();

        void hideProgressDialog();

        void onUiUpdated();
    }

    public ConfirmFundsTransferViewModel(DataListener listener) {
        Injector.getInstance().getAppComponent().inject(this);
        mDataListener = listener;
        mCompositeSubscription = new CompositeSubscription();
    }

    public void onViewReady() {
        updateToAddress(mPayloadManager.getPayload().getHdWallet().getDefaultIndex());
    }

    public void accountSelected(int position) {
        updateToAddress(getAdjustedAccountPosition(position));
    }

    private void updateToAddress(int indexOfReceiveAccount) {
        mDataListener.setPaymentButtonEnabled(false);
        mCompositeSubscription.add(
                mFundsDataManager.getTransferableFundTransactionList(indexOfReceiveAccount)
                        .subscribe(map -> {
                            Map.Entry<List<PendingTransaction>, Pair<Long, Long>> entry = map.entrySet().iterator().next();
                            mPendingTransactions = entry.getKey();
                            Pair<Long, Long> value = entry.getValue();
                            updateUi(value.first, value.second);

                        }, throwable -> {
                            mDataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                            mDataListener.dismissDialog();
                        }));
    }

    @VisibleForTesting
    void updateUi(long totalToSend, long totalFee) {
        mDataListener.updateFromLabel(mStringUtils.getQuantityString(
                R.plurals.transfer_label_plural,
                mPendingTransactions.size()));

        MonetaryUtil monetaryUtil = new MonetaryUtil(mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        String fiatUnit = mPrefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        String btcUnit = monetaryUtil.getBTCUnit(mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        double exchangeRate = mExchangeRateFactory.getLastPrice(fiatUnit);

        String fiatAmount = monetaryUtil.getFiatFormat(fiatUnit).format(exchangeRate * ((double) totalToSend / 1e8));
        String fiatFee = monetaryUtil.getFiatFormat(fiatUnit).format(exchangeRate * ((double) totalFee / 1e8));

        mDataListener.updateTransferAmountBtc(
                monetaryUtil.getDisplayAmountWithFormatting(totalToSend)
                        + " "
                        + btcUnit);
        mDataListener.updateTransferAmountFiat(
                mExchangeRateFactory.getSymbol(fiatUnit)
                        + fiatAmount);

        mDataListener.updateFeeAmountBtc(
                monetaryUtil.getDisplayAmountWithFormatting(totalFee)
                        + " "
                        + btcUnit);
        mDataListener.updateFeeAmountFiat(
                mExchangeRateFactory.getSymbol(fiatUnit)
                        + fiatFee);

        mDataListener.setPaymentButtonEnabled(true);

        mDataListener.onUiUpdated();
    }

    /**
     * Transacts all {@link PendingTransaction} objects
     *
     * @param secondPassword The user's double encryption password if necessary
     */
    public void sendPayment(@Nullable CharSequenceX secondPassword) {
        boolean archiveAll = mDataListener.getIfArchiveChecked();
        mDataListener.setPaymentButtonEnabled(false);
        mDataListener.showProgressDialog();
        mCompositeSubscription.add(
                mFundsDataManager.sendPayment(new Payment(), mPendingTransactions, secondPassword)
                        .subscribe(new Subscriber<String>() {
                            @Override
                            public void onCompleted() {
                                mDataListener.hideProgressDialog();
                                mDataListener.showToast(R.string.transfer_confirmed, ToastCustom.TYPE_OK);
                                if (archiveAll) {
                                    archiveAll();
                                } else {
                                    mDataListener.dismissDialog();
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                mDataListener.hideProgressDialog();
                                mDataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                                mDataListener.dismissDialog();
                            }

                            @Override
                            public void onNext(String s) {
                                // Emits Tx hash - don't need to do anything with this
                            }
                        }));
    }

    /**
     * Returns only HD Accounts as we want to move funds to a backed up place
     *
     * @return {@link List<ItemAccount>}
     */
    public List<ItemAccount> getReceiveToList() {
        return new ArrayList<ItemAccount>() {{
            addAll(mWalletAccountHelper.getHdAccounts(true));
        }};
    }

    /**
     * Get corrected default account position
     *
     * @return int account position in list of non-archived accounts
     */
    public int getDefaultAccount() {
        return Math.max(getCorrectedAccountIndex(mPayloadManager.getPayload().getHdWallet().getDefaultIndex()), 0);
    }

    @Thunk
    void archiveAll() {
        mDataListener.showProgressDialog();
        for (PendingTransaction spend : mPendingTransactions) {
            ((LegacyAddress) spend.sendingObject.accountObject).setTag(PayloadManager.ARCHIVED_ADDRESS);
        }

        mCompositeSubscription.add(
                mFundsDataManager.savePayloadToServer()
                        .subscribe(aBoolean -> {
                            if (aBoolean) {
                                mDataListener.hideProgressDialog();
                                mDataListener.showToast(R.string.transfer_archive, ToastCustom.TYPE_OK);
                                mDataListener.dismissDialog();
                            } else {
                                throw Exceptions.propagate(new Throwable("Syncing Payload failed"));
                            }

                        }, throwable -> {
                            mDataListener.hideProgressDialog();
                            mDataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                            mDataListener.dismissDialog();
                        })
        );
    }

    private int getAdjustedAccountPosition(int position) {
        List<Account> accounts = mPayloadManager.getPayload().getHdWallet().getAccounts();
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
        List<Account> accounts = mPayloadManager.getPayload().getHdWallet().getAccounts();
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            if (!account.isArchived()) {
                activeAccounts.add(account);
            }
        }

        // Find corrected position
        return activeAccounts.indexOf(mPayloadManager.getPayload().getHdWallet().getAccounts().get(accountIndex));
    }

    @Override
    public void destroy() {
        // Clear all subscriptions so that:
        // 1) all processes are cancelled
        // 2) processes don't try to update a null View
        // 3) background processes don't leak memory
        mCompositeSubscription.clear();
    }
}
