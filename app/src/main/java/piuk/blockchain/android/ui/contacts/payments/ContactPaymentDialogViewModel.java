package piuk.blockchain.android.ui.contacts.payments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import info.blockchain.api.data.UnspentOutputs;
import info.blockchain.wallet.api.data.FeeList;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.util.FormatsUtil;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.ECKey;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.contacts.ContactsPredicates;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.datamanagers.SendDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.receive.ReceiveCurrencyHelper;
import piuk.blockchain.android.ui.receive.WalletAccountHelper;
import piuk.blockchain.android.ui.send.PendingTransaction;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;

import static piuk.blockchain.android.ui.contacts.payments.ContactPaymentDialog.ARGUMENT_CONTACT_ID;
import static piuk.blockchain.android.ui.contacts.payments.ContactPaymentDialog.ARGUMENT_CONTACT_MDID;
import static piuk.blockchain.android.ui.contacts.payments.ContactPaymentDialog.ARGUMENT_FCTX_ID;
import static piuk.blockchain.android.ui.contacts.payments.ContactPaymentDialog.ARGUMENT_URI;

@SuppressWarnings("WeakerAccess")
public class ContactPaymentDialogViewModel extends BaseViewModel {

    private static final String TAG = ContactPaymentDialogViewModel.class.getSimpleName();

    private DataListener dataListener;
    private MonetaryUtil monetaryUtil;
    private ReceiveCurrencyHelper currencyHelper;
    private Disposable unspentApiDisposable;
    @VisibleForTesting BigInteger maxAvailable;
    @VisibleForTesting FeeList dynamicFeeList;
    @VisibleForTesting PendingTransaction pendingTransaction = new PendingTransaction();
    @Nullable private String contactMdid;
    @Nullable private String fctxId;
    @Inject protected ContactsDataManager contactsDataManager;
    @Inject protected PrefsUtil prefsUtil;
    @Inject protected ExchangeRateFactory exchangeRateFactory;
    @Inject protected WalletAccountHelper walletAccountHelper;
    @Inject protected PayloadDataManager payloadDataManager;
    @Inject protected SendDataManager sendDataManager;
    @Inject protected DynamicFeeCache dynamicFeeCache;

    interface DataListener {

        Bundle getFragmentBundle();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void showProgressDialog();

        void hideProgressDialog();

        void setContactName(String name);

        void finishPage(boolean paymentSent);

        void updatePaymentAmountBtc(String amount);

        void updateFeeAmountFiat(String amount);

        void updatePaymentAmountFiat(String amount);

        void updateFeeAmountBtc(String amount);

        void setPaymentButtonEnabled(boolean enabled);

        void onUiUpdated();

        void onShowTransactionSuccess(String contactMdid, String hash, String fctxId, long amount);
    }

    ContactPaymentDialogViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        currencyHelper = new ReceiveCurrencyHelper(monetaryUtil, Locale.getDefault());
        getSuggestedFee();
    }

    @Override
    public void onViewReady() {
        Bundle bundle = dataListener.getFragmentBundle();
        if (bundle != null) {
            final String uri = bundle.getString(ARGUMENT_URI);
            final String contactId = bundle.getString(ARGUMENT_CONTACT_ID);
            contactMdid = bundle.getString(ARGUMENT_CONTACT_MDID);
            fctxId = bundle.getString(ARGUMENT_FCTX_ID);

            if (contactId != null) {
                compositeDisposable.add(
                        contactsDataManager.getContactList()
                                .filter(ContactsPredicates.filterById(contactId))
                                .firstOrError()
                                .subscribe(
                                        contact -> {
                                            dataListener.setContactName(contact.getName());
                                            handleIncomingUri(uri);
                                        },
                                        throwable -> showContactNotFoundAndQuit()));
            } else {
                showContactNotFoundAndQuit();
            }
        } else {
            dataListener.finishPage(false);
        }
    }

    /**
     * Returns a list of {@link ItemAccount} objects generated from the user's HD {@link Account}
     * list.
     */
    List<ItemAccount> getSendFromList() {
        return walletAccountHelper.getHdAccounts(true);
    }

    /**
     * Get corrected default account position. Returns 0 if you attempt to find the position of an
     * archived Account.x
     *
     * @return int account position in list of non-archived accounts
     * @see PayloadDataManager#getPositionOfAccountInActiveList(int)
     */
    int getDefaultAccountPosition() {
        return Math.max(payloadDataManager.getPositionOfAccountInActiveList(
                payloadDataManager.getDefaultAccountIndex()), 0);
    }

    void accountSelected(int position) {
        Account account =
                payloadDataManager.getAccount(payloadDataManager.getPositionOfAccountFromActiveList(position));

        pendingTransaction.sendingObject = new ItemAccount(account.getLabel(), "", null, null, account);
        calculateTransactionAmounts();
    }

    void onSendClicked(@Nullable String verifiedSecondPassword) {
        Account account = (Account) pendingTransaction.sendingObject.accountObject;
        compositeDisposable.add(
                payloadDataManager.getNextChangeAddress(account)
                        .doOnSubscribe(disposable -> dataListener.showProgressDialog())
                        .flatMap(changeAddress -> {
                            if (payloadDataManager.isDoubleEncrypted()) {
                                payloadDataManager.getWallet()
                                        .decryptHDWallet(0, verifiedSecondPassword);
                            }

                            List<ECKey> keys = payloadDataManager.getHDKeysForSigning(
                                    account, pendingTransaction.unspentOutputBundle);

                            return sendDataManager.submitPayment(
                                    pendingTransaction.unspentOutputBundle,
                                    keys,
                                    pendingTransaction.receivingAddress,
                                    changeAddress,
                                    pendingTransaction.bigIntFee,
                                    pendingTransaction.bigIntAmount);
                        })
                        .doOnTerminate(() -> dataListener.hideProgressDialog())
                        .subscribe(
                                this::handleSuccessfulPayment,
                                throwable -> dataListener.showToast(R.string.transaction_failed, ToastCustom.TYPE_ERROR)));
    }

    @SuppressWarnings("ConstantConditions")
    private void handleSuccessfulPayment(String hash) {
        Account account = (Account) pendingTransaction.sendingObject.accountObject;
        payloadDataManager.incrementChangeAddress(account);
        payloadDataManager.incrementReceiveAddress(account);
        try {
            payloadDataManager.subtractAmountFromAddressBalance(
                    account.getXpub(),
                    pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee).longValue());
        } catch (Exception e) {
            Log.e(TAG, "subtractAmountFromAddressBalance: ", e);
        }

        if (dataListener != null) {
            dataListener.onShowTransactionSuccess(contactMdid, hash, fctxId, pendingTransaction.bigIntAmount.longValue());
        }
    }

    private void getSuggestedFee() {
        dynamicFeeList = dynamicFeeCache.getCachedDynamicFee();
        // Refresh fee cache
        compositeDisposable.add(
                sendDataManager.getSuggestedFee()
                        .doAfterTerminate(() -> dynamicFeeList = dynamicFeeCache.getCachedDynamicFee())
                        .subscribe(suggestedFee -> dynamicFeeCache.setCachedDynamicFee(suggestedFee)
                                , throwable -> {
                                    // No-op
                                }));
    }

    private void handleIncomingUri(String uri) {
        uri = uri.trim();
        String btcAddress;
        String btcAmount;

        btcAddress = FormatsUtil.getBitcoinAddress(uri);
        btcAmount = FormatsUtil.getBitcoinAmount(uri);

        // Convert to correct units
        try {
            btcAmount = monetaryUtil.getDisplayAmount(Long.parseLong(btcAmount));
        } catch (Exception e) {
            dataListener.showToast(R.string.invalid_bitcoin_address, ToastCustom.TYPE_ERROR);
            dataListener.finishPage(false);
            return;
        }

        pendingTransaction.receivingAddress = btcAddress;
        pendingTransaction.bigIntAmount = BigInteger.valueOf(currencyHelper.getLongAmount(btcAmount));
    }

    @SuppressWarnings("ConstantConditions")
    private void calculateTransactionAmounts() {
        String address = ((Account) pendingTransaction.sendingObject.accountObject).getXpub();
        if (unspentApiDisposable != null) unspentApiDisposable.dispose();
        unspentApiDisposable = sendDataManager.getUnspentOutputs(address)
                .subscribe(
                        coins -> suggestedFeePayment(coins, pendingTransaction.bigIntAmount),
                        throwable -> dataListener.showToast(R.string.no_confirmed_funds, ToastCustom.TYPE_ERROR));
    }

    private void suggestedFeePayment(final UnspentOutputs coins, BigInteger amountToSend)
            throws UnsupportedEncodingException {
        if (dynamicFeeList != null) {
            BigInteger feePerKb = new BigDecimal(dynamicFeeList.getDefaultFee().getFee()).toBigInteger();
            pendingTransaction.unspentOutputBundle = sendDataManager.getSpendableCoins(coins,
                    amountToSend,
                    feePerKb);
            pendingTransaction.bigIntFee = pendingTransaction.unspentOutputBundle.getAbsoluteFee();

            // Calculate sweepable amount to compute max available
            Pair<BigInteger, BigInteger> sweepBundle = sendDataManager.getSweepableCoins(coins, feePerKb);
            maxAvailable = sweepBundle.getLeft();
            // Tell UI about update
            updateUi(pendingTransaction.bigIntAmount.longValue(),
                    pendingTransaction.bigIntFee.longValue());
        } else {
            // App is likely in low memory environment, leave page gracefully
            if (dataListener != null) dataListener.finishPage(false);
        }
    }

    /**
     * Checks whether or not the transaction is valid
     */
    @VisibleForTesting
    boolean isValidSpend(PendingTransaction pendingTransaction) {
        // Validate sufficient funds
        if (pendingTransaction.unspentOutputBundle == null
                || pendingTransaction.unspentOutputBundle.getSpendableOutputs() == null) {
            dataListener.showToast(R.string.no_confirmed_funds, ToastCustom.TYPE_ERROR);
            return false;
        }

        if (maxAvailable.compareTo(pendingTransaction.bigIntAmount) == -1) {
            dataListener.showToast(R.string.insufficient_funds, ToastCustom.TYPE_ERROR);
            // TODO: 27/03/2017 Prompt user to buy bitcoin here, probably want a flag to only do it once per session
            return false;
        }

        if (pendingTransaction.unspentOutputBundle.getSpendableOutputs().isEmpty()) {
            // TODO: 27/03/2017 Prompt user to buy bitcoin here, probably want a flag to only do it once per session
            dataListener.showToast(R.string.insufficient_funds, ToastCustom.TYPE_ERROR);
            return false;
        }

        return true;
    }

    private void updateUi(long totalToSend, long totalFee) {
        String fiatUnit = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        String btcUnit = monetaryUtil.getBTCUnit(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        double exchangeRate = exchangeRateFactory.getLastPrice(fiatUnit);

        String fiatAmount = monetaryUtil.getFiatFormat(fiatUnit).format(exchangeRate * ((double) totalToSend / 1e8));
        String fiatFee = monetaryUtil.getFiatFormat(fiatUnit).format(exchangeRate * ((double) totalFee / 1e8));

        dataListener.updatePaymentAmountBtc(
                monetaryUtil.getDisplayAmountWithFormatting(totalToSend)
                        + " "
                        + btcUnit);
        dataListener.updatePaymentAmountFiat(
                exchangeRateFactory.getSymbol(fiatUnit)
                        + fiatAmount);

        dataListener.updateFeeAmountBtc(
                monetaryUtil.getDisplayAmountWithFormatting(totalFee)
                        + " "
                        + btcUnit);
        dataListener.updateFeeAmountFiat(
                exchangeRateFactory.getSymbol(fiatUnit)
                        + fiatFee);

        dataListener.setPaymentButtonEnabled(isValidSpend(pendingTransaction));

        dataListener.onUiUpdated();
    }

    private void showContactNotFoundAndQuit() {
        dataListener.showToast(R.string.contacts_not_found_error, ToastCustom.TYPE_ERROR);
        dataListener.finishPage(false);
    }

}
