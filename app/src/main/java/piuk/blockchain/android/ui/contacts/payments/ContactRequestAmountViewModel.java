package piuk.blockchain.android.ui.contacts.payments;

import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.PayloadManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.receive.ReceiveCurrencyHelper;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;


@SuppressWarnings("WeakerAccess")
public class ContactRequestAmountViewModel extends BaseViewModel {

    private DataListener dataListener;
    private ReceiveCurrencyHelper currencyHelper;
    private long satoshis;
    private int accountPosition;
    @Inject PrefsUtil prefsUtil;
    @Inject PayloadManager payloadManager;

    interface DataListener {

        void updateFiatTextField(String formattedFiatString);

        void updateBtcTextField(String formattedBtcString);

    }

    ContactRequestAmountViewModel(DataListener dataListener, Locale locale) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;

        int btcUnitType = prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        MonetaryUtil monetaryUtil = new MonetaryUtil(btcUnitType);
        currencyHelper = new ReceiveCurrencyHelper(monetaryUtil, locale);
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    ReceiveCurrencyHelper getCurrencyHelper() {
        return currencyHelper;
    }

    int getAccountPosition() {
        return accountPosition;
    }

    public void setAccountPosition(int accountPosition) {
        this.accountPosition = getCorrectedAccountIndex(accountPosition);
    }

    List<String> getAccountsList() {
        List<String> accountNames = new ArrayList<>();
        //noinspection Convert2streamapi
        for (Account account : payloadManager.getPayload().getHdWallet().getAccounts()) {
            if (!account.isArchived()) {
                accountNames.add(account.getLabel());
            }
        }

        return accountNames;
    }

    void updateFiatTextField(String bitcoin) {
        if (bitcoin.isEmpty()) bitcoin = "0";
        double btcAmount = currencyHelper.getUndenominatedAmount(currencyHelper.getDoubleAmount(bitcoin));
        double fiatAmount = currencyHelper.getLastPrice() * btcAmount;

        satoshis = currencyHelper.getLongAmount(bitcoin);
        dataListener.updateFiatTextField(currencyHelper.getFormattedFiatString(fiatAmount));
    }

    void updateBtcTextField(String fiat) {
        if (fiat.isEmpty()) fiat = "0";
        double fiatAmount = currencyHelper.getDoubleAmount(fiat);
        double btcAmount = fiatAmount / currencyHelper.getLastPrice();

        String amountString = currencyHelper.getFormattedBtcString(btcAmount);
        satoshis = currencyHelper.getLongAmount(amountString);
        dataListener.updateBtcTextField(amountString);
    }

    long getAmountInSatoshis() {
        return satoshis;
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
        return payloadManager.getPayload().getHdWallet().getAccounts().indexOf(activeAccounts.get(accountIndex));
    }
}
