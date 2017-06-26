package piuk.blockchain.android.data.datamanagers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import info.blockchain.api.data.Balance;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.metadata.MetadataNodeFactory;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payload.data.Wallet;
import info.blockchain.wallet.payment.SpendableUnspentOutputs;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.DeterministicKey;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxPinning;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.PayloadService;

@SuppressWarnings("WeakerAccess")
public class PayloadDataManager {

    private PayloadService payloadService;
    private PayloadManager payloadManager;
    private RxPinning rxPinning;

    public PayloadDataManager(PayloadService payloadService, PayloadManager payloadManager, RxBus rxBus) {
        this.payloadService = payloadService;
        this.payloadManager = payloadManager;
        rxPinning = new RxPinning(rxBus);
    }

    ///////////////////////////////////////////////////////////////////////////
    // AUTH METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Decrypts and initializes a wallet from a payload String. Handles both V3 and V1 wallets. Will
     * return a {@link DecryptionException} if the password is incorrect, otherwise can return a
     * {@link HDWalletException} which should be regarded as fatal.
     *
     * @param payload  The payload String to be decrypted
     * @param password The user's password
     * @return A {@link Completable} object
     */
    public Completable initializeFromPayload(String payload, String password) {
        return rxPinning.call(() -> payloadService.initializeFromPayload(payload, password))
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Restores a HD wallet from a 12 word mnemonic and initializes the {@link PayloadDataManager}.
     * Also creates a new Blockchain.info account in the process.
     *
     * @param mnemonic   The 12 word mnemonic supplied as a String of words separated by whitespace
     * @param walletName The name of the wallet, usually a default name localised by region
     * @param email      The user's email address, preferably not associated with another account
     * @param password   The user's choice of password
     * @return An {@link Observable<Wallet>}
     */
    public Observable<Wallet> restoreHdWallet(String mnemonic, String walletName, String email, String password) {
        return rxPinning.call(() -> payloadService.restoreHdWallet(mnemonic, walletName, email, password))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Creates a new HD wallet and Blockchain.info account.
     *
     * @param password   The user's choice of password
     * @param walletName The name of the wallet, usually a default name localised by region
     * @param email      The user's email address, preferably not associated with another account
     * @return An {@link Observable<Wallet>}
     */
    public Observable<Wallet> createHdWallet(String password, String walletName, String email) {
        return rxPinning.call(() -> payloadService.createHdWallet(password, walletName, email))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Fetches the user's wallet payload, and then initializes and decrypts a payload using the
     * user's  password.
     *
     * @param sharedKey The shared key as a String
     * @param guid      The user's GUID
     * @param password  The user's password
     * @return A {@link Completable} object
     */
    public Completable initializeAndDecrypt(String sharedKey, String guid, String password) {
        return rxPinning.call(() -> payloadService.initializeAndDecrypt(sharedKey, guid, password))
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Initializes and decrypts a user's payload given valid QR code scan data.
     *
     * @param data A QR's URI for pairing
     * @return A {@link Completable} object
     */
    public Completable handleQrCode(String data) {
        return rxPinning.call(() -> payloadService.handleQrCode(data))
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Upgrades a Wallet from V2 to V3 and saves it with the server. If saving is unsuccessful or
     * some other part fails, this will propagate an Exception.
     *
     * @param secondPassword     An optional second password if the user has one
     * @param defaultAccountName A required name for the default account
     * @return A {@link Completable} object
     */
    public Completable upgradeV2toV3(@Nullable String secondPassword, String defaultAccountName) {
        return rxPinning.call(() -> payloadService.upgradeV2toV3(secondPassword, defaultAccountName))
                .compose(RxUtil.applySchedulersToCompletable());
    }

    ///////////////////////////////////////////////////////////////////////////
    // SYNC METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns a {@link Completable} which saves the current payload to the server.
     *
     * @return A {@link Completable} object
     */
    public Completable syncPayloadWithServer() {
        return rxPinning.call(() -> payloadService.syncPayloadWithServer())
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Returns a {@link Completable} which saves the current payload to the server whilst also
     * forcing the sync of the user's public keys. This method generates 20 addresses per {@link
     * Account}, so it should be used only when strictly necessary (for instance, after enabling
     * notifications).
     *
     * @return A {@link Completable} object
     */
    public Completable syncPayloadAndPublicKeys() {
        return rxPinning.call(() -> payloadService.syncPayloadAndPublicKeys())
                .compose(RxUtil.applySchedulersToCompletable());
    }

    ///////////////////////////////////////////////////////////////////////////
    // TRANSACTION METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns {@link Completable} which updates transactions in the PayloadManager.
     * Completable returns no value, and is used to call functions that return void but have side
     * effects.
     *
     * @return A {@link Completable} object
     * @see IgnorableDefaultObserver
     */
    public Completable updateAllTransactions() {
        return rxPinning.call(() -> payloadService.updateAllTransactions())
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Returns a {@link Completable} which updates all balances in the PayloadManager. Completable
     * returns no value, and is used to call functions that return void but have side effects.
     *
     * @return A {@link Completable} object
     * @see IgnorableDefaultObserver
     */
    public Completable updateAllBalances() {
        return rxPinning.call(() -> payloadService.updateAllBalances())
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Update notes for a specific transaction hash and then sync the payload to the server
     *
     * @param transactionHash The hash of the transaction to be updated
     * @param notes           Transaction notes
     * @return A {@link Completable} object
     */
    public Completable updateTransactionNotes(String transactionHash, String notes) {
        return rxPinning.call(() -> payloadService.updateTransactionNotes(transactionHash, notes))
                .compose(RxUtil.applySchedulersToCompletable());
    }

    ///////////////////////////////////////////////////////////////////////////
    // ACCOUNTS AND ADDRESS METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns a {@link LinkedHashMap} of {@link Balance} objects keyed to their addresses.
     *
     * @param addresses A List of addresses as Strings
     * @return A {@link LinkedHashMap}
     */
    public Observable<LinkedHashMap<String, Balance>> getBalanceOfAddresses(List<String> addresses) {
        return rxPinning.call(() -> payloadService.getBalanceOfAddresses(addresses))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Converts any address to a label.
     *
     * @param address Accepts account receive or change chain address, as well as legacy address.
     * @return Either the label associated with the address, or the original address
     */
    @NonNull
    public String addressToLabel(String address) {
        return payloadManager.getLabelFromAddress(address);
    }

    /**
     * Returns the next Receive address for a given account index.
     *
     * @param accountIndex The index of the account for which you want an address to be generated
     * @return An {@link Observable} wrapping the receive address
     */
    public Observable<String> getNextReceiveAddress(int accountIndex) {
        Account account = getAccounts().get(accountIndex);
        return getNextReceiveAddress(account);
    }

    /**
     * Returns the next Receive address for a given {@link Account object}
     *
     * @param account The {@link Account} for which you want an address to be generated
     * @return An {@link Observable} wrapping the receive address
     */
    public Observable<String> getNextReceiveAddress(Account account) {
        return Observable.fromCallable(() -> payloadManager.getNextReceiveAddress(account))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Returns the next Receive address for a given {@link Account object}
     *
     * @param accountIndex The index of the account for which you want an address to be generated
     * @param label        Label used to reserve address
     * @return An {@link Observable} wrapping the receive address
     */
    public Observable<String> getNextReceiveAddressAndReserve(int accountIndex, String label) {
        Account account = getAccounts().get(accountIndex);
        return Observable.fromCallable(() -> payloadManager.getNextReceiveAddressAndReserve(account, label))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Returns the next Change address for a given account index.
     *
     * @param accountIndex The index of the account for which you want an address to be generated
     * @return An {@link Observable} wrapping the receive address
     */
    public Observable<String> getNextChangeAddress(int accountIndex) {
        Account account = getAccounts().get(accountIndex);
        return getNextChangeAddress(account);
    }

    /**
     * Returns the next Change address for a given {@link Account object}
     *
     * @param account The {@link Account} for which you want an address to be generated
     * @return An {@link Observable} wrapping the receive address
     */
    public Observable<String> getNextChangeAddress(Account account) {
        return Observable.fromCallable(() -> payloadManager.getNextChangeAddress(account))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Returns an {@link ECKey} for a given {@link LegacyAddress}, optionally with a second password
     * should the private key be encrypted.
     *
     * @param legacyAddress  The {@link  LegacyAddress} to generate an Elliptic Curve Key for
     * @param secondPassword An optional second password, necessary if the private key is ebcrypted
     * @return An Elliptic Curve Key object {@link ECKey}
     * @throws UnsupportedEncodingException Thrown if the private key is formatted incorrectly
     * @throws DecryptionException          Thrown if the supplied password is wrong
     * @throws InvalidCipherTextException   Thrown if there's an issue decrypting the private key
     * @see LegacyAddress#isPrivateKeyEncrypted()
     */
    @Nullable
    public ECKey getAddressECKey(LegacyAddress legacyAddress, @Nullable String secondPassword)
            throws UnsupportedEncodingException, DecryptionException, InvalidCipherTextException {
        return payloadManager.getAddressECKey(legacyAddress, secondPassword);
    }

    @NonNull
    public List<Account> getAccounts() {
        return getWallet() != null
                ? getWallet().getHdWallets().get(0).getAccounts()
                : Collections.emptyList();
    }

    @NonNull
    public List<LegacyAddress> getLegacyAddresses() {
        return getWallet() != null ? getWallet().getLegacyAddressList() : Collections.emptyList();
    }

    /**
     * Returns the balance of an address. If the address isn't found in the address map object, the
     * method will return {@link BigInteger#ZERO} instead of a null object.
     *
     * @param address The address whose balance you wish to query
     * @return A {@link BigInteger} representing the total funds in the address
     */
    @NonNull
    public BigInteger getAddressBalance(String address) {
        return payloadManager.getAddressBalance(address);
    }

    /**
     * Allows you to generate a receive address at an arbitrary number of positions on the chain
     * from the next valid unused address. For example, the passing 5 as the position will generate
     * an address which correlates with the next available address + 5 positions.
     *
     * @param account  The {@link Account} you wish to generate an address from
     * @param position Represents how many positions on the chain beyond what is already used that
     *                 you wish to generate
     * @return A bitcoin address
     */
    @Nullable
    public String getReceiveAddressAtPosition(Account account, int position) {
        return payloadManager.getReceiveAddressAtPosition(account, position);
    }

    /**
     * Allows you to get an address from any given point on the receive chain.
     *
     * @param account  The {@link Account} you wish to generate an address from
     * @param position What position on the chain the address you wish to create is
     * @return A bitcoin address
     */
    @Nullable
    public String getReceiveAddressAtArbitraryPosition(Account account, int position) {
        return payloadManager.getReceiveAddressAtArbitraryPosition(account, position);
    }

    /**
     * Updates the balance of the address as well as that of the entire wallet. To be called after a
     * successful sweep to ensure that balances are displayed correctly before syncing the wallet.
     *
     * @param address     An address from which you've just spent funds
     * @param spentAmount The spent amount as a long
     * @throws Exception Thrown if the address isn't found
     */
    public void subtractAmountFromAddressBalance(String address, long spentAmount) throws Exception {
        payloadManager.subtractAmountFromAddressBalance(address, BigInteger.valueOf(spentAmount));
    }

    /**
     * Increments the index on the receive chain for an {@link Account} object.
     *
     * @param account The {@link Account} you wish to increment
     */
    public void incrementReceiveAddress(Account account) {
        payloadManager.incrementNextReceiveAddress(account);
    }

    /**
     * Increments the index on the change chain for an {@link Account} object.
     *
     * @param account The {@link Account} you wish to increment
     */
    public void incrementChangeAddress(Account account) {
        payloadManager.incrementNextChangeAddress(account);
    }

    /**
     * Returns an xPub from an address if the address belongs to this wallet.
     *
     * @param address The address you want to query as a String
     * @return An xPub as a String
     */
    @Nullable
    public String getXpubFromAddress(String address) {
        return payloadManager.getXpubFromAddress(address);
    }

    /**
     * Returns an xPub from a given {@link Account} index. This call is not index-safe, ie will
     * throw an {@link IndexOutOfBoundsException} if you choose an index which is greater than the
     * size of the Accounts list.
     *
     * @param index The index of the Account
     * @return An xPub as a String
     */
    @NonNull
    public String getXpubFromIndex(int index) {
        return payloadManager.getXpubFromAccountIndex(index);
    }

    /**
     * Returns true if the supplied address belongs to the user's wallet.
     *
     * @param address The address you want to query as a String
     * @return true if the address belongs to the user
     */
    public boolean isOwnHDAddress(String address) {
        return payloadManager.isOwnHDAddress(address);
    }

    ///////////////////////////////////////////////////////////////////////////
    // CONTACTS/METADATA/IWCS/CRYPTO-MATRIX METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Loads previously saved nodes from the Metadata service. If none are found, the {@link
     * Observable} returns false.
     *
     * @return An {@link Observable} object wrapping a boolean value, representing successfully
     * loaded nodes
     */
    public Observable<Boolean> loadNodes() {
        return rxPinning.call(() -> payloadService.loadNodes())
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Generates the metadata and shared metadata nodes if necessary.
     *
     * @param secondPassword An optional second password.
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    public Completable generateNodes(@Nullable String secondPassword) {
        return rxPinning.call(() -> payloadService.generateNodes(secondPassword))
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Returns a {@link MetadataNodeFactory} object which allows you to access the {@link
     * DeterministicKey} objects needed to initialise the Contacts service.
     *
     * @return An {@link Observable} wrapping a {@link MetadataNodeFactory}
     */
    public Observable<MetadataNodeFactory> getMetadataNodeFactory() {
        return Observable.just(payloadManager.getMetadataNodeFactory());
    }

    /**
     * Registers the user's MDID with the metadata service.
     *
     * @return An {@link Observable} wrapping a {@link ResponseBody}
     */
    public Observable<ResponseBody> registerMdid() {
        return rxPinning.call(() -> payloadService.registerMdid())
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Unregisters the user's MDID from the metadata service.
     *
     * @return An {@link Observable} wrapping a {@link ResponseBody}
     */
    public Observable<ResponseBody> unregisterMdid() {
        return rxPinning.call(() -> payloadService.unregisterMdid())
                .compose(RxUtil.applySchedulersToObservable());
    }

    ///////////////////////////////////////////////////////////////////////////
    // CONVENIENCE METHODS
    ///////////////////////////////////////////////////////////////////////////

    public Wallet getWallet() {
        return payloadManager != null ? payloadManager.getPayload() : null;
    }

    public int getDefaultAccountIndex() {
        return getWallet().getHdWallets().get(0).getDefaultAccountIdx();
    }

    public Account getDefaultAccount() {
        return getWallet().getHdWallets().get(0).getAccount(getDefaultAccountIndex());
    }

    public Account getAccount(int accountPosition) {
        return getWallet().getHdWallets().get(0).getAccount(accountPosition);
    }

    /**
     * Returns a list of {@link ECKey} objects for signing transactions.
     *
     * @param account             The {@link Account} that you wish to send funds from
     * @param unspentOutputBundle A {@link SpendableUnspentOutputs} bundle for a given Account
     * @return A list of {@link ECKey} objects
     * @throws Exception Will be thrown if there are issues with the private keys
     */
    public List<ECKey> getHDKeysForSigning(Account account, SpendableUnspentOutputs unspentOutputBundle)
            throws Exception {
        return getWallet()
                .getHdWallets()
                .get(0)
                .getHDKeysForSigning(account, unspentOutputBundle);
    }

    @Nullable
    public String getPayloadChecksum() {
        return payloadManager.getPayloadChecksum();
    }

    @Nullable
    public String getTempPassword() {
        return payloadManager.getTempPassword();
    }

    public void setTempPassword(String password) {
        payloadManager.setTempPassword(password);
    }

    @NonNull
    public BigInteger getWalletBalance() {
        return payloadManager.getWalletBalance();
    }

    @NonNull
    public BigInteger getImportedAddressesBalance() {
        return payloadManager.getImportedAddressesBalance();
    }

    public boolean isDoubleEncrypted() {
        return getWallet().isDoubleEncryption();
    }

    public boolean isBackedUp() {
        return payloadManager.getPayload() != null
                && payloadManager.getPayload().getHdWallets() != null
                && payloadManager.getPayload().getHdWallets().get(0).isMnemonicVerified();
    }

    ///////////////////////////////////////////////////////////////////////////
    // HELPER METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns the index for an {@link Account}, assuming that the supplied position was gotten from
     * a list of only those Accounts which are active.
     *
     * @param position The position of the {@link Account} that you want to select from a list of
     *                 active Accounts
     * @return The position of the {@link Account} within the full list of Accounts
     */
    public int getPositionOfAccountFromActiveList(int position) {
        List<Account> accounts = getAccounts();
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

    /**
     * Returns the index for an {@link Account} in a list of active-only Accounts, where the
     * supplied {@code accountIndex} is the position of the Account in the full list of both active
     * and archived Accounts.
     *
     * @param accountIndex The position of an {@link Account} in the full list of Accounts
     * @return The Account's position within a list of active-only Accounts. Will be -1 if you
     * attempt to find the position of an archived Account
     */
    public int getPositionOfAccountInActiveList(int accountIndex) {
        // Filter accounts by active
        List<Account> activeAccounts = new ArrayList<>();
        List<Account> accounts = getAccounts();
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            if (!account.isArchived()) {
                activeAccounts.add(account);
            }
        }

        // Find corrected position
        return activeAccounts.indexOf(getAccounts().get(accountIndex));
    }

}
