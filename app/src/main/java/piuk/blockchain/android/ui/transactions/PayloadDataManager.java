package piuk.blockchain.android.ui.transactions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import info.blockchain.api.data.Balance;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payload.data.Wallet;

import org.bitcoinj.core.ECKey;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver;
import piuk.blockchain.android.data.rxjava.RxUtil;

public class PayloadDataManager {

    private PayloadManager payloadManager;

    public PayloadDataManager(PayloadManager payloadManager) {
        this.payloadManager = payloadManager;
    }

    ///////////////////////////////////////////////////////////////////////////
    // AUTH METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Decrypts and initializes a wallet from a payload String. Handles both V3 and V1 wallets. Will
     * return a {@link DecryptionException} if the password isincorrect, otherwise can return a
     * {@link HDWalletException} which should be regarded as fatal.
     *
     * @param payload  The payload String to be decrypted
     * @param password The user's password
     * @return A {@link Completable} object
     */
    public Completable initializeFromPayload(String payload, String password) {
        return Completable.fromCallable(() -> {
            payloadManager.initializeAndDecryptFromPayload(payload, password);
            return Void.TYPE;
        });
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
        return Observable.fromCallable(() ->
                payloadManager.recoverFromMnemonic(mnemonic, walletName, email, password));
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
        return Observable.fromCallable(() -> payloadManager.create(walletName, email, password));
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
        return Completable.fromCallable(() -> {
            payloadManager.initializeAndDecrypt(sharedKey, guid, password);
            return Void.TYPE;
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // THE NEXT LOGICAL BLOCK -> TODO: ORGANISE REMAINING METHODS
    ///////////////////////////////////////////////////////////////////////////

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
     * Returns a {@link Completable} which saves the current payload to the server.
     *
     * @return A {@link Completable} object
     */
    // TODO: 10/03/2017 Remove schedulers, let viewmodels handle it
    public Completable syncPayloadWithServer() {
        return Completable.fromCallable(() -> {
            payloadManager.save();
            return Void.TYPE;
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Returns {@link Completable} which updates balances and transactions in the PayloadManager.
     * Completable returns no value, and is used to call functions that return void but have side
     * effects.
     *
     * @return A {@link Completable} object
     * @see IgnorableDefaultObserver
     */
    // TODO: 10/03/2017 Remove schedulers, let viewmodels handle it
    public Completable updateBalancesAndTransactions() {
        return Completable.fromCallable(() -> {
            payloadManager.getAllTransactions(50, 0);
            return Void.TYPE;
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Returns the next Receive address for a given account index.
     *
     * @param defaultIndex The index of the account for which you want an address to be generated
     * @return An {@link Observable} wrapping the receive address
     */
    public Observable<String> getNextReceiveAddress(int defaultIndex) {
        Account account = getWallet().getHdWallets().get(0).getAccounts().get(defaultIndex);
        return Observable.fromCallable(() -> payloadManager.getNextReceiveAddress(account));
    }

    /**
     * Returns an {@link ECKey} for a given {@link LegacyAddress}, optionally with a second password
     * should the private key be encrypted.
     *
     * @param legacyAddress  The {@link  LegacyAddress} to generate an Elliptic Curve Key for
     * @param secondPassword An optional second password, necessary if the private key is encrypted
     * @return An Elliptic Curve Key object {@link ECKey}
     * @throws UnsupportedEncodingException Thrown if the private key is formatted incorrectly
     * @throws DecryptionException          Thrown if the supplied password is wrong
     * @throws InvalidCipherTextException   Thrown if there's an issue decrypting the private key
     * @see LegacyAddress#isPrivateKeyEncrypted()
     */
    public ECKey getAddressECKey(LegacyAddress legacyAddress, @Nullable String secondPassword)
            throws UnsupportedEncodingException, DecryptionException, InvalidCipherTextException {
        return payloadManager.getAddressECKey(legacyAddress, secondPassword);
    }

    /**
     * Returns the balance for all imported addresses
     *
     * @return A {@link BigInteger} object
     */
    public BigInteger getImportedAddressesBalance() {
        return payloadManager.getImportedAddressesBalance();
    }

    ///////////////////////////////////////////////////////////////////////////
    // CONVENIENCE METHODS
    ///////////////////////////////////////////////////////////////////////////

    public Wallet getWallet() {
        return payloadManager.getPayload();
    }

    public int getDefaultAccountIndex() {
        return getWallet().getHdWallets().get(0).getDefaultAccountIdx();
    }

    public Account getDefaultAccount() {
        return getWallet().getHdWallets().get(0).getAccount(getDefaultAccountIndex());
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
        return payloadManager.getAddressBalance(address) != null
                ? payloadManager.getAddressBalance(address) : BigInteger.ZERO;
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
     * Returns a {@link LinkedHashMap} of {@link Balance} objects keyed to their addresses.
     *
     * @param addresses A List of addresses as Strings
     * @return A {@link LinkedHashMap}
     */
    public Observable<LinkedHashMap<String, Balance>> getBalanceOfAddresses(List<String> addresses) {
        return Observable.fromCallable(() -> payloadManager.getBalanceOfAddresses(addresses));
    }

    /**
     * Updates the balance of the address as well as that of the entire wallet. To be called after a
     * successful sweep to ensure that balances are displayed correctly before syncing the wallet.
     *
     * @param legacyAddress A {@link LegacyAddress} object from which you've just spent funds
     * @param spentAmount   The spent amount as a long
     * @throws Exception Thrown if the address isn't found
     */
    public void subtractAmountFromAddressBalance(LegacyAddress legacyAddress, long spentAmount) throws Exception {
        payloadManager.subtractAmountFromAddressBalance(
                legacyAddress.getAddress(),
                BigInteger.valueOf(spentAmount));
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
     * Returns true if the supplied address belongs to the user's wallet.
     *
     * @param address The address you want to query as a String
     * @return true if the address belongs to the user
     */
    public boolean isOwnHDAddress(String address) {
        return payloadManager.isOwnHDAddress(address);
    }

}
