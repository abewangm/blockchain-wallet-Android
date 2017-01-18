package piuk.blockchain.android.ui.contacts.payments;

import java.util.Locale;

import javax.inject.Inject;

import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.receive.ReceiveCurrencyHelper;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;


public class ContactRequestAmountViewModel extends BaseViewModel {

    private DataListener dataListener;
    private ReceiveCurrencyHelper currencyHelper;
    private long satoshis;
    @Inject PrefsUtil prefsUtil;

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
}
