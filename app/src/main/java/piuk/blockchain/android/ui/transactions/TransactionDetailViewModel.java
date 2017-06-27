package piuk.blockchain.android.ui.transactions;

import android.content.Intent;
import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;

import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.multiaddress.TransactionSummary.Direction;

import org.apache.commons.lang3.tuple.Pair;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

import static piuk.blockchain.android.ui.balance.BalanceFragment.KEY_TRANSACTION_HASH;
import static piuk.blockchain.android.ui.balance.BalanceFragment.KEY_TRANSACTION_LIST_POSITION;

@SuppressWarnings("WeakerAccess")
public class TransactionDetailViewModel extends BaseViewModel {

    private static final int REQUIRED_CONFIRMATIONS = 3;

    private DataListener mDataListener;
    private MonetaryUtil mMonetaryUtil;
    @Inject TransactionHelper transactionHelper;
    @Inject PrefsUtil mPrefsUtil;
    @Inject PayloadDataManager mPayloadDataManager;
    @Inject StringUtils mStringUtils;
    @Inject TransactionListDataManager mTransactionListDataManager;
    @Inject ExchangeRateFactory mExchangeRateFactory;
    @Inject ContactsDataManager mContactsDataManager;

    private String mFiatType;

    @VisibleForTesting TransactionSummary mTransaction;

    public interface DataListener {

        Intent getPageIntent();

        void pageFinish();

        void setTransactionType(Direction type);

        void setTransactionValueBtc(String value);

        void setTransactionValueFiat(String fiat);

        void setToAddresses(List<RecipientModel> addresses);

        void setFromAddress(String address);

        void setStatus(String status, String hash);

        void setFee(String fee);

        void setDate(String date);

        void setDescription(String description);

        void setIsDoubleSpend(boolean isDoubleSpend);

        void setTransactionNote(String note);

        void setTransactionColour(@ColorRes int colour);

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void onDataLoaded();

    }

    public TransactionDetailViewModel(DataListener listener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        mDataListener = listener;
        mMonetaryUtil = new MonetaryUtil(mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));

        mFiatType = mPrefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
    }

    @Override
    public void onViewReady() {
        Intent pageIntent = mDataListener.getPageIntent();
        if (pageIntent != null && pageIntent.hasExtra(KEY_TRANSACTION_LIST_POSITION)) {
            int transactionPosition = pageIntent.getIntExtra(KEY_TRANSACTION_LIST_POSITION, -1);
            if (transactionPosition != -1) {
                mTransaction = mTransactionListDataManager.getTransactionList().get(transactionPosition);
                updateUiFromTransaction(mTransaction);
            } else {
                mDataListener.pageFinish();
            }
        } else if (pageIntent != null && pageIntent.hasExtra(KEY_TRANSACTION_HASH)) {
            compositeDisposable.add(
                    mTransactionListDataManager.getTxFromHash(pageIntent.getStringExtra(KEY_TRANSACTION_HASH))
                            .subscribe(
                                    this::updateUiFromTransaction,
                                    throwable -> mDataListener.pageFinish()));
        } else {
            mDataListener.pageFinish();
        }
    }

    public void updateTransactionNote(String description) {
        compositeDisposable.add(
                mPayloadDataManager.updateTransactionNotes(mTransaction.getHash(), description)
                        .subscribe(() -> {
                            mDataListener.showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK);
                            mDataListener.setDescription(description);
                        }, throwable -> mDataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));
    }

    private void updateUiFromTransaction(TransactionSummary transactionSummary) {
        mDataListener.setTransactionType(transactionSummary.getDirection());
        setTransactionColor(transactionSummary);
        setTransactionAmountInBtc(transactionSummary.getTotal());
        setConfirmationStatus(transactionSummary.getHash(), transactionSummary.getConfirmations());
        setTransactionNote(transactionSummary.getHash());
        setDate(transactionSummary.getTime());
        setFee(transactionSummary.getFee());

        Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> pair =
                transactionHelper.filterNonChangeAddresses(transactionSummary);

        // From Address
        HashMap<String, BigInteger> inputMap = pair.getLeft();
        ArrayList<String> labelList = new ArrayList<>();
        Set<Entry<String, BigInteger>> entrySet = inputMap.entrySet();
        for (Entry<String, BigInteger> set : entrySet) {
            String label = mPayloadDataManager.addressToLabel(set.getKey());
            if (!labelList.contains(label)) labelList.add(label);
        }

        String inputMapString = org.apache.commons.lang3.StringUtils.join(labelList.toArray(), "\n\n");
        if (inputMapString.isEmpty()) {
            inputMapString = mStringUtils.getString(R.string.transaction_detail_coinbase);
        }

        if (mContactsDataManager.getContactsTransactionMap().containsKey(transactionSummary.getHash())
                && transactionSummary.getDirection().equals(Direction.RECEIVED)) {
            inputMapString = mContactsDataManager.getContactsTransactionMap().get(transactionSummary.getHash());
        }

        // TODO: 14/03/2017 Change this to dropdown like outputs, as a list of addresses looks terrible
        mDataListener.setFromAddress(inputMapString);

        // To Address
        HashMap<String, BigInteger> outputMap = pair.getRight();
        ArrayList<RecipientModel> recipients = new ArrayList<>();

        for (Entry<String, BigInteger> item : outputMap.entrySet()) {
            RecipientModel recipientModel = new RecipientModel(
                    mPayloadDataManager.addressToLabel(item.getKey()),
                    mMonetaryUtil.getDisplayAmountWithFormatting(item.getValue().longValue()),
                    getDisplayUnits());

            if (mContactsDataManager.getContactsTransactionMap().containsKey(transactionSummary.getHash())
                    && transactionSummary.getDirection().equals(Direction.SENT)) {
                String contactName = mContactsDataManager.getContactsTransactionMap().get(transactionSummary.getHash());
                recipientModel.setAddress(contactName);
            }

            recipients.add(recipientModel);
        }

        mDataListener.setToAddresses(recipients);

        if (mContactsDataManager.getNotesTransactionMap().containsKey(transactionSummary.getHash())) {
            String note = mContactsDataManager.getNotesTransactionMap().get(transactionSummary.getHash());
            mDataListener.setTransactionNote(note);
        }

        compositeDisposable.add(
                getTransactionValueString(mFiatType, transactionSummary)
                        .subscribe(
                                value -> mDataListener.setTransactionValueFiat(value),
                                throwable -> mDataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));

        mDataListener.onDataLoaded();
        mDataListener.setIsDoubleSpend(transactionSummary.isDoubleSpend());
    }

    private void setFee(BigInteger fee) {
        String formattedFee = (mMonetaryUtil.getDisplayAmountWithFormatting(fee.longValue()) + " " + getDisplayUnits());
        mDataListener.setFee(formattedFee);
    }

    private void setTransactionAmountInBtc(BigInteger total) {
        String amountBtc = (
                mMonetaryUtil.getDisplayAmountWithFormatting(
                        total.abs().longValue())
                        + " "
                        + getDisplayUnits());

        mDataListener.setTransactionValueBtc(amountBtc);
    }

    private void setTransactionNote(String txHash) {
        String notes = mPayloadDataManager.getTransactionNotes(txHash);
        mDataListener.setDescription(notes);
    }

    public String getTransactionNote() {
        return mPayloadDataManager.getTransactionNotes(mTransaction.getHash());
    }

    public String getTransactionHash() {
        return mTransaction.getHash();
    }

    @VisibleForTesting
    void setConfirmationStatus(String txHash, long confirmations) {
        if (confirmations >= REQUIRED_CONFIRMATIONS) {
            mDataListener.setStatus(mStringUtils.getString(R.string.transaction_detail_confirmed), txHash);
        } else {
            String pending = mStringUtils.getString(R.string.transaction_detail_pending);
            pending = String.format(Locale.getDefault(), pending, confirmations, REQUIRED_CONFIRMATIONS);
            mDataListener.setStatus(pending, txHash);
        }
    }

    private void setDate(long time) {
        long epochTime = time * 1000;

        Date date = new Date(epochTime);
        DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.LONG);
        DateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String dateText = dateFormat.format(date);
        String timeText = timeFormat.format(date);

        mDataListener.setDate(dateText + " @ " + timeText);
    }

    @VisibleForTesting
    void setTransactionColor(TransactionSummary transaction) {
        if (transaction.getDirection() == Direction.TRANSFERRED) {
            mDataListener.setTransactionColour(transaction.getConfirmations() < REQUIRED_CONFIRMATIONS
                    ? R.color.product_gray_transferred_50 : R.color.product_gray_transferred);
        } else if (transaction.getDirection() == Direction.SENT) {
            mDataListener.setTransactionColour(transaction.getConfirmations() < REQUIRED_CONFIRMATIONS
                    ? R.color.product_red_sent_50 : R.color.product_red_sent);
        } else {
            mDataListener.setTransactionColour(transaction.getConfirmations() < REQUIRED_CONFIRMATIONS
                    ? R.color.product_green_received_50 : R.color.product_green_received);
        }
    }

    @VisibleForTesting
    Observable<String> getTransactionValueString(String currency, TransactionSummary transaction) {
        return mExchangeRateFactory.getHistoricPrice(
                transaction.getTotal().abs().longValue(),
                currency,
                transaction.getTime() * 1000)
                .map(aDouble -> {
                    int stringId = -1;
                    switch (transaction.getDirection()) {
                        case TRANSFERRED:
                            stringId = R.string.transaction_detail_value_at_time_transferred;
                            break;
                        case SENT:
                            stringId = R.string.transaction_detail_value_at_time_sent;
                            break;
                        case RECEIVED:
                            stringId = R.string.transaction_detail_value_at_time_received;
                            break;
                    }
                    return mStringUtils.getString(stringId)
                            + mExchangeRateFactory.getSymbol(mFiatType)
                            + mMonetaryUtil.getFiatFormat(mFiatType).format(aDouble);
                });
    }

    private String getDisplayUnits() {
        return (String) mMonetaryUtil.getBTCUnits()[mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];
    }

}
