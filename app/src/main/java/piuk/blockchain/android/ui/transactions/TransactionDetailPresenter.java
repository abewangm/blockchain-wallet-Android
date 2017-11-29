package piuk.blockchain.android.ui.transactions;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import info.blockchain.wallet.contacts.data.FacilitatedTransaction;
import info.blockchain.wallet.multiaddress.MultiAddressFactory;
import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.multiaddress.TransactionSummary.Direction;

import org.apache.commons.lang3.tuple.Pair;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.ContactsDataManager;
import piuk.blockchain.android.data.contacts.models.ContactTransactionDisplayModel;
import piuk.blockchain.android.data.currency.CryptoCurrencies;
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager;
import piuk.blockchain.android.data.ethereum.EthDataManager;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.transactions.BtcDisplayable;
import piuk.blockchain.android.data.transactions.Displayable;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

import static piuk.blockchain.android.ui.balance.BalanceFragment.KEY_TRANSACTION_HASH;
import static piuk.blockchain.android.ui.balance.BalanceFragment.KEY_TRANSACTION_LIST_POSITION;

@SuppressWarnings("WeakerAccess")
public class TransactionDetailPresenter extends BasePresenter<TransactionDetailView> {

    private static final int CONFIRMATIONS_BTC = 3;
    private static final int CONFIRMATIONS_ETH = 12;

    private MonetaryUtil monetaryUtil;
    private TransactionHelper transactionHelper;
    private PrefsUtil prefsUtil;
    private PayloadDataManager payloadDataManager;
    private StringUtils stringUtils;
    private TransactionListDataManager transactionListDataManager;
    private ExchangeRateFactory exchangeRateFactory;
    private ContactsDataManager contactsDataManager;
    private EthDataManager ethDataManager;

    private String fiatType;

    @VisibleForTesting Displayable displayable;

    @Inject
    public TransactionDetailPresenter(TransactionHelper transactionHelper,
                                      PrefsUtil prefsUtil,
                                      PayloadDataManager payloadDataManager,
                                      StringUtils stringUtils,
                                      TransactionListDataManager transactionListDataManager,
                                      ExchangeRateFactory exchangeRateFactory,
                                      ContactsDataManager contactsDataManager,
                                      EthDataManager ethDataManager) {

        this.transactionHelper = transactionHelper;
        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        fiatType = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        this.prefsUtil = prefsUtil;
        this.payloadDataManager = payloadDataManager;
        this.stringUtils = stringUtils;
        this.transactionListDataManager = transactionListDataManager;
        this.exchangeRateFactory = exchangeRateFactory;
        this.contactsDataManager = contactsDataManager;
        this.ethDataManager = ethDataManager;
    }

    @Override
    public void onViewReady() {
        Intent pageIntent = getView().getPageIntent();
        if (pageIntent != null && pageIntent.hasExtra(KEY_TRANSACTION_LIST_POSITION)) {
            int transactionPosition = pageIntent.getIntExtra(KEY_TRANSACTION_LIST_POSITION, -1);
            if (transactionPosition != -1) {
                displayable = transactionListDataManager.getTransactionList().get(transactionPosition);
                updateUiFromTransaction(displayable);
            } else {
                getView().pageFinish();
            }
        } else if (pageIntent != null && pageIntent.hasExtra(KEY_TRANSACTION_HASH)) {
            getCompositeDisposable().add(
                    transactionListDataManager.getTxFromHash(pageIntent.getStringExtra(KEY_TRANSACTION_HASH))
                            .doOnSuccess(displayable -> this.displayable = displayable)
                            .subscribe(
                                    this::updateUiFromTransaction,
                                    throwable -> getView().pageFinish()));
        } else {
            getView().pageFinish();
        }
    }

    void updateTransactionNote(String description) {
        Completable completable;
        if (displayable.getCryptoCurrency() == CryptoCurrencies.BTC) {
            completable = payloadDataManager.updateTransactionNotes(displayable.getHash(), description);
        } else {
            completable = ethDataManager.updateTransactionNotes(displayable.getHash(), description);
        }

        completable.compose(RxUtil.addCompletableToCompositeDisposable(this))
                .subscribe(() -> {
                    getView().showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK);
                    getView().setDescription(description);
                }, throwable -> getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR));
    }

    private void updateUiFromTransaction(Displayable displayable) {
        getView().setTransactionType(displayable.getDirection());
        setTransactionColor(displayable);
        setTransactionAmountInBtcOrEth(displayable.getCryptoCurrency(), displayable.getTotal());
        setConfirmationStatus(displayable.getCryptoCurrency(), displayable.getHash(), displayable.getConfirmations());
        setTransactionNote(displayable.getHash());
        setDate(displayable.getTimeStamp());
        setFee(displayable.getCryptoCurrency(), displayable.getFee());

        if (displayable.getCryptoCurrency() == CryptoCurrencies.BTC) {
            handleBtcToAndFrom(displayable);
        } else {
            handleEthToAndFrom(displayable);
        }

        getCompositeDisposable().add(
                getTransactionValueString(fiatType, displayable)
                        .subscribe(
                                value -> getView().setTransactionValueFiat(value),
                                throwable -> getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));

        getView().onDataLoaded();
        getView().setIsDoubleSpend(displayable.getDoubleSpend());
    }

    @SuppressWarnings("ConstantConditions")
    private void handleEthToAndFrom(Displayable displayable) {
        String fromAddress = displayable.getInputsMap().keySet().iterator().next();
        String toAddress = displayable.getOutputsMap().keySet().iterator().next();

        String ethAddress = ethDataManager.getEthResponseModel().getAddressResponse().getAccount();
        if (fromAddress.equals(ethAddress)) {
            fromAddress = stringUtils.getString(R.string.eth_default_account_label);
        }
        if (toAddress.equals(ethAddress)) {
            toAddress = stringUtils.getString(R.string.eth_default_account_label);
        }

        getView().setFromAddress(Collections.singletonList(new TransactionDetailModel(
                fromAddress, "", "")));
        getView().setToAddresses(Collections.singletonList(new TransactionDetailModel(
                toAddress, "", "")));
    }

    private void handleBtcToAndFrom(Displayable displayable) {
        Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> pair =
                transactionHelper.filterNonChangeAddresses(displayable);

        // From Addresses
        ArrayList<TransactionDetailModel> fromList = getFromList(pair.getLeft());
        getView().setFromAddress(fromList);

        // From Contacts
        ContactTransactionDisplayModel displayModel = null;
        if (contactsDataManager.getTransactionDisplayMap().containsKey(displayable.getHash())) {
            displayModel = contactsDataManager.getTransactionDisplayMap().get(displayable.getHash());

            // Check if should be "Paid" state via contacts
            if (displayModel != null) {
                if (displayModel.getState().equals(FacilitatedTransaction.STATE_PAYMENT_BROADCASTED)
                        && displayModel.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER)) {
                    getView().showTransactionAsPaid();
                }
            }
        }

        // To Addresses
        ArrayList<TransactionDetailModel> recipients = getToList(displayModel, pair.getRight());
        getView().setToAddresses(recipients);

        if (displayModel != null) {
            getView().setTransactionNote(displayModel.getNote());
        }
    }

    private ArrayList<TransactionDetailModel> getFromList(HashMap<String, BigInteger> inputMap) {
        ArrayList<TransactionDetailModel> inputs = new ArrayList<>();

        for (Map.Entry<String, BigInteger> item : inputMap.entrySet()) {

            long value = (item.getValue() != null) ? item.getValue().longValue() : 0;

            TransactionDetailModel transactionDetailModel = new TransactionDetailModel(
                    payloadDataManager.addressToLabel(item.getKey()),
                    monetaryUtil.getDisplayAmountWithFormatting(value),
                    getDisplayUnits());

            if (transactionDetailModel.getAddress().equals(MultiAddressFactory.ADDRESS_DECODE_ERROR)) {
                transactionDetailModel.setAddress(stringUtils.getString(R.string.tx_decode_error));
                transactionDetailModel.setAddressDecodeError(true);
            }

            inputs.add(transactionDetailModel);
        }

        //No inputs = coinbase
        if (inputs.isEmpty()) {
            TransactionDetailModel coinbase = new TransactionDetailModel(
                    stringUtils.getString(R.string.transaction_detail_coinbase),
                    "",
                    getDisplayUnits());

            inputs.add(coinbase);
        }

        return inputs;
    }

    private ArrayList<TransactionDetailModel> getToList(ContactTransactionDisplayModel displayModel, HashMap<String, BigInteger> outputMap) {
        ArrayList<TransactionDetailModel> recipients = new ArrayList<>();

        for (Map.Entry<String, BigInteger> item : outputMap.entrySet()) {

            long value = (item.getValue() != null) ? item.getValue().longValue() : 0;

            TransactionDetailModel transactionDetailModel = new TransactionDetailModel(
                    payloadDataManager.addressToLabel(item.getKey()),
                    monetaryUtil.getDisplayAmountWithFormatting(value),
                    getDisplayUnits());

            if (displayModel != null && displayable.getDirection().equals(Direction.SENT)) {
                transactionDetailModel.setAddress(displayModel.getContactName());
            }

            if(transactionDetailModel.getAddress().equals(MultiAddressFactory.ADDRESS_DECODE_ERROR)) {
                transactionDetailModel.setAddress(stringUtils.getString(R.string.tx_decode_error));
                transactionDetailModel.setAddressDecodeError(true);
            }

            recipients.add(transactionDetailModel);
        }

        return recipients;
    }

    private void setFee(CryptoCurrencies currency, BigInteger fee) {
        if (currency == CryptoCurrencies.BTC) {
            String formattedFee = (monetaryUtil.getDisplayAmountWithFormatting(fee.longValue()) + " " + getDisplayUnits());
            getView().setFee(formattedFee);
        } else {
            BigDecimal value = new BigDecimal(fee)
                    .divide(BigDecimal.valueOf(1e18), 8, RoundingMode.HALF_UP);
            NumberFormat format = NumberFormat.getInstance();
            format.setMaximumFractionDigits(8);
            getView().setFee(format.format(value.doubleValue()) + " ETH");
        }
    }

    private void setTransactionAmountInBtcOrEth(CryptoCurrencies currency, BigInteger total) {
        if (currency == CryptoCurrencies.ETHER) {
            BigDecimal value = new BigDecimal(total)
                    .divide(BigDecimal.valueOf(1e18), 8, RoundingMode.HALF_UP);
            NumberFormat format = NumberFormat.getInstance();
            format.setMaximumFractionDigits(8);
            String amountEth = (format.format(value.doubleValue()) + " ETH");
            getView().setTransactionValueBtc(amountEth);
        } else {
            String amountBtc = (
                    monetaryUtil.getDisplayAmountWithFormatting(
                            total.abs().longValue())
                            + " "
                            + getDisplayUnits());

            getView().setTransactionValueBtc(amountBtc);
        }
    }

    private void setTransactionNote(String txHash) {
        String notes;
        if (displayable.getCryptoCurrency() == CryptoCurrencies.BTC) {
            notes = payloadDataManager.getTransactionNotes(txHash);
        } else {
            notes = ethDataManager.getTransactionNotes(displayable.getHash());
        }
        getView().setDescription(notes);
    }

    public String getTransactionNote() {
        if (displayable.getCryptoCurrency() == CryptoCurrencies.BTC) {
            return payloadDataManager.getTransactionNotes(displayable.getHash());
        } else {
            return ethDataManager.getTransactionNotes(displayable.getHash());
        }
    }

    public String getTransactionHash() {
        return displayable.getHash();
    }

    public CryptoCurrencies getTransactionType() {
        return displayable.getCryptoCurrency();
    }

    @VisibleForTesting
    void setConfirmationStatus(CryptoCurrencies cryptoCurrency, String txHash, long confirmations) {
        if (confirmations >= getRequiredConfirmations(cryptoCurrency)) {
            getView().setStatus(cryptoCurrency, stringUtils.getString(R.string.transaction_detail_confirmed), txHash);
        } else {
            String pending = stringUtils.getString(R.string.transaction_detail_pending);
            pending = String.format(Locale.getDefault(), pending, confirmations, getRequiredConfirmations(cryptoCurrency));
            getView().setStatus(cryptoCurrency, pending, txHash);
        }
    }

    private int getRequiredConfirmations(CryptoCurrencies cryptoCurrency) {
        return cryptoCurrency == CryptoCurrencies.BTC ? CONFIRMATIONS_BTC : CONFIRMATIONS_ETH;
    }

    private void setDate(long time) {
        long epochTime = time * 1000;

        Date date = new Date(epochTime);
        DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.LONG);
        DateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String dateText = dateFormat.format(date);
        String timeText = timeFormat.format(date);

        getView().setDate(dateText + " @ " + timeText);
    }

    @VisibleForTesting
    void setTransactionColor(Displayable transaction) {
        if (transaction.getDirection() == Direction.TRANSFERRED) {
            getView().setTransactionColour(transaction.getConfirmations() < getRequiredConfirmations(transaction.getCryptoCurrency())
                    ? R.color.product_gray_transferred_50 : R.color.product_gray_transferred);
        } else if (transaction.getDirection() == Direction.SENT) {
            getView().setTransactionColour(transaction.getConfirmations() < getRequiredConfirmations(transaction.getCryptoCurrency())
                    ? R.color.product_red_sent_50 : R.color.product_red_sent);
        } else {
            getView().setTransactionColour(transaction.getConfirmations() < getRequiredConfirmations(transaction.getCryptoCurrency())
                    ? R.color.product_green_received_50 : R.color.product_green_received);
        }
    }

    @VisibleForTesting
    Observable<String> getTransactionValueString(String currency, Displayable transaction) {
        if (transaction.getCryptoCurrency() == CryptoCurrencies.BTC) {
            return exchangeRateFactory.getBtcHistoricPrice(
                    transaction.getTotal().longValue(),
                    currency,
                    transaction.getTimeStamp())
                    .map(aDouble -> getTransactionString(transaction, aDouble));
        } else {
            return exchangeRateFactory.getEthHistoricPrice(
                    transaction.getTotal(),
                    currency,
                    transaction.getTimeStamp())
                    .map(aDouble -> getTransactionString(transaction, aDouble));
        }
    }

    @NonNull
    private String getTransactionString(Displayable transaction, Double aDouble) {
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
        return stringUtils.getString(stringId)
                + exchangeRateFactory.getSymbol(fiatType)
                + monetaryUtil.getFiatFormat(fiatType).format(aDouble);
    }

    private String getDisplayUnits() {
        return monetaryUtil.getBtcUnits()[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];
    }

}
