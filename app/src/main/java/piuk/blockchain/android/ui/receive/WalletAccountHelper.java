package piuk.blockchain.android.ui.receive;

import android.support.annotation.NonNull;

import android.util.Log;
import info.blockchain.api.data.MultiAddress;
import info.blockchain.wallet.payload.PayloadManager;

import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.AddressBook;
import info.blockchain.wallet.payload.data.LegacyAddress;
import java.util.ArrayList;
import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

public class WalletAccountHelper {

    private final PayloadManager payloadManager;
    private final StringUtils stringUtils;
    private AddressBalanceHelper addressBalanceHelper;
    private double btcExchangeRate;
    private String fiatUnit;
    private String btcUnit;

    public WalletAccountHelper(
            PayloadManager payloadManager,
            PrefsUtil prefsUtil,
            StringUtils stringUtils,
            ExchangeRateFactory exchangeRateFactory) {
        this.payloadManager = payloadManager;
        this.stringUtils = stringUtils;
        int btcUnit = prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        MonetaryUtil monetaryUtil = new MonetaryUtil(btcUnit);
        this.btcUnit = monetaryUtil.getBTCUnit(btcUnit);
        fiatUnit = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        btcExchangeRate = exchangeRateFactory.getLastPrice(fiatUnit);

        addressBalanceHelper = new AddressBalanceHelper(monetaryUtil, payloadManager);
    }

    /**
     * Returns a list of {@link ItemAccount} objects containing both HD accounts and {@link
     * LegacyAddress} objects, eg from importing accounts.
     *
     * @param isBtc Whether or not you wish to have the ItemAccount objects returned with {@link
     *              ItemAccount#displayBalance} showing BTC or fiat
     * @return Returns a list of {@link ItemAccount} objects
     */
    @NonNull
    public List<ItemAccount> getAccountItems(boolean isBtc) {
        List<ItemAccount> accountList = new ArrayList<>();
        // V3
        accountList.addAll(getHdAccounts(isBtc));
        // V2
        accountList.addAll(getLegacyAddresses(isBtc));

        return accountList;
    }

    /**
     * Returns a list of {@link ItemAccount} objects containing only HD accounts.
     *
     * @param isBtc Whether or not you wish to have the ItemAccount objects returned with {@link
     *              ItemAccount#displayBalance} showing BTC or fiat
     * @return Returns a list of {@link ItemAccount} objects
     */
    @NonNull
    public List<ItemAccount> getHdAccounts(boolean isBtc) {
        List<ItemAccount> accountArrayList = new ArrayList<>();
        if (payloadManager.getPayload().isUpgraded()) {

            List<Account> accounts = payloadManager.getPayload().getHdWallets().get(0).getAccounts();
            for (Account account : accounts) {

                if (account.isArchived()) {
                    // Skip archived account
                    continue;
                }

                accountArrayList.add(new ItemAccount(
                        account.getLabel(),
                        addressBalanceHelper.getAccountBalance(account, isBtc, btcExchangeRate, fiatUnit, btcUnit),
                        null,
                        addressBalanceHelper.getAccountAbsoluteBalance(account),
                        account));
            }
        }

        return accountArrayList;
    }

    /**
     * Returns a list of {@link ItemAccount} objects containing only {@link LegacyAddress} objects.
     *
     * @param isBtc Whether or not you wish to have the ItemAccount objects returned with {@link
     *              ItemAccount#displayBalance} showing BTC or fiat
     * @return Returns a list of {@link ItemAccount} objects
     */
    @NonNull
    public List<ItemAccount> getLegacyAddresses(boolean isBtc) {
        List<ItemAccount> accountList = new ArrayList<>();

        List<LegacyAddress> legacyAddresses = payloadManager.getPayload().getLegacyAddressList();
        for (LegacyAddress legacyAddress : legacyAddresses) {

            if (legacyAddress.getTag() == LegacyAddress.ARCHIVED_ADDRESS)
                // Skip archived account
                continue;

            // If address has no label, we'll display address
            String labelOrAddress = legacyAddress.getLabel();
            if (labelOrAddress == null || labelOrAddress.trim().isEmpty()) {
                labelOrAddress = legacyAddress.getAddress();
            }

            // Watch-only tag - we'll ask for xpriv scan when spending from
            String tag = null;
            if (legacyAddress.isWatchOnly()) {
                tag = stringUtils.getString(R.string.watch_only);
            }

            accountList.add(new ItemAccount(
                    labelOrAddress,
                    addressBalanceHelper.getAddressBalance(legacyAddress, isBtc, btcExchangeRate, fiatUnit, btcUnit),
                    tag,
                    addressBalanceHelper.getAddressAbsoluteBalance(legacyAddress),
                    legacyAddress));

        }

        return accountList;
    }

    /**
     * Returns a list of {@link ItemAccount} objects containing only {@link LegacyAddress} objects,
     * specifically from the list of address book entries.
     *
     * @return Returns a list of {@link ItemAccount} objects
     */
    @Deprecated
    @NonNull
    public List<ItemAccount> getAddressBookEntries() {
        List<ItemAccount> itemAccountList = new ArrayList<>();

        List<AddressBook> addressBookEntries = payloadManager.getPayload().getAddressBook();
        if(addressBookEntries != null) {
            for (AddressBook addressBookEntry : addressBookEntries) {

                // If address has no label, we'll display address
                String labelOrAddress =
                    addressBookEntry.getLabel() == null || addressBookEntry.getLabel().length() == 0
                        ? addressBookEntry.getAddress() : addressBookEntry.getLabel();

                itemAccountList.add(new ItemAccount(labelOrAddress, "",
                    stringUtils.getString(R.string.address_book_label), null, addressBookEntry));
            }
        }

        return itemAccountList;
    }
}
