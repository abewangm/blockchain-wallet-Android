package piuk.blockchain.android.data.services;

import android.support.annotation.Nullable;

import info.blockchain.api.data.Balance;
import info.blockchain.wallet.exceptions.ApiException;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payload.data.Wallet;

import org.bitcoinj.core.ECKey;

import java.util.LinkedHashMap;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import okhttp3.ResponseBody;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver;
import piuk.blockchain.android.util.annotations.WebRequest;

public class PayloadService {

    private PayloadManager payloadManager;

    public PayloadService(PayloadManager payloadManager) {
        this.payloadManager = payloadManager;
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
    @WebRequest
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
     * @return An {@link Observable < Wallet >}
     */
    @WebRequest
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
    @WebRequest
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
    @WebRequest
    public Completable initializeAndDecrypt(String sharedKey, String guid, String password) {
        return Completable.fromCallable(() -> {
            payloadManager.initializeAndDecrypt(sharedKey, guid, password);
            return Void.TYPE;
        });
    }

    /**
     * Initializes and decrypts a user's payload given valid QR code scan data.
     *
     * @param data A QR's URI for pairing
     * @return A {@link Completable} object
     */
    @WebRequest
    public Completable handleQrCode(String data) {
        return Completable.fromCallable(() -> {
            payloadManager.initializeAndDecryptFromQR(data);
            return Void.TYPE;
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // TRANSACTION METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns a {@link Completable} which saves the current payload to the server.
     *
     * @return A {@link Completable} object
     */
    @WebRequest
    public Completable syncPayloadWithServer() {
        return Completable.fromCallable(() -> {
            if (!payloadManager.save()) throw new ApiException("Sync failed");
            return Void.TYPE;
        });
    }

    /**
     * Returns {@link Completable} which updates transactions in the PayloadManager.
     * Completable returns no value, and is used to call functions that return void but have side
     * effects.
     *
     * @return A {@link Completable} object
     * @see IgnorableDefaultObserver
     */
    @WebRequest
    public Completable updateAllTransactions() {
        return Completable.fromCallable(() -> {
            payloadManager.getAllTransactions(50, 0);
            return Void.TYPE;
        });
    }

    /**
     * Returns a {@link Completable} which updates all balances in the PayloadManager. Completable
     * returns no value, and is used to call functions that return void but have side effects.
     *
     * @return A {@link Completable} object
     * @see IgnorableDefaultObserver
     */
    @WebRequest
    public Completable updateAllBalances() {
        return Completable.fromCallable(() -> {
            payloadManager.updateAllBalances();
            return Void.TYPE;
        });
    }

    /**
     * Returns a {@link LinkedHashMap} of {@link Balance} objects keyed to their addresses.
     *
     * @param addresses A List of addresses as Strings
     * @return A {@link LinkedHashMap}
     */
    @WebRequest
    public Observable<LinkedHashMap<String, Balance>> getBalanceOfAddresses(List<String> addresses) {
        return Observable.fromCallable(() -> payloadManager.getBalanceOfAddresses(addresses));
    }

    /**
     * Update notes for a specific transaction hash and then sync the payload to the server
     *
     * @param transactionHash The hash of the transaction to be updated
     * @param notes           Transaction notes
     * @return A {@link Completable} object
     */
    @WebRequest
    public Completable updateTransactionNotes(String transactionHash, String notes) {
        payloadManager.getPayload().getTxNotes().put(transactionHash, notes);
        return syncPayloadWithServer();
    }

    ///////////////////////////////////////////////////////////////////////////
    // ACCOUNTS AND ADDRESS METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Derives new {@link Account} from the master seed
     *
     * @param accountLabel   A label for the account
     * @param secondPassword An optional double encryption password
     * @return An {@link Observable<Account>} wrapping the newly created Account
     */
    @WebRequest
    public Observable<Account> createNewAccount(String accountLabel, @Nullable String secondPassword) {
        return Observable.fromCallable(() -> payloadManager.addAccount(accountLabel, secondPassword));
    }

    /**
     * Sets a private key for an associated {@link LegacyAddress} which is already in the {@link
     * info.blockchain.wallet.payload.data.Wallet} as a watch only address
     *
     * @param key            An {@link ECKey}
     * @param secondPassword An optional double encryption password
     * @return An {@link Observable<Boolean>} representing a successful save
     */
    @WebRequest
    public Observable<LegacyAddress> setPrivateKey(ECKey key, @Nullable String secondPassword) {
        return Observable.fromCallable(() -> payloadManager.setKeyForLegacyAddress(key, secondPassword));
    }

    /**
     * Sets a private key for a {@link LegacyAddress}
     *
     * @param key            The {@link ECKey} for the address
     * @param secondPassword An optional double encryption password
     */
    @WebRequest
    public Observable<LegacyAddress> setKeyForLegacyAddress(ECKey key, @Nullable String secondPassword) {
        return Observable.fromCallable(() -> payloadManager.setKeyForLegacyAddress(key, secondPassword));
    }

    /**
     * Allows you to propagate changes to a {@link LegacyAddress} through the {@link
     * info.blockchain.wallet.payload.data.Wallet}
     *
     * @param legacyAddress The updated address
     * @return {@link Observable<Boolean>} representing a successful save
     */
    @WebRequest
    public Completable updateLegacyAddress(LegacyAddress legacyAddress) {
        return Completable.fromCallable(() -> {
            payloadManager.addLegacyAddress(legacyAddress);
            return Void.TYPE;
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // CONTACTS/METADATA METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Loads previously saved nodes from the Metadata service. If none are found, the {@link
     * Observable} returns false.
     *
     * @return An {@link Observable} object wrapping a boolean value, representing successfully
     * loaded nodes
     */
    @WebRequest
    public Observable<Boolean> loadNodes() {
        return Observable.fromCallable(() -> payloadManager.loadNodes());
    }

    /**
     * Generates the metadata and shared metadata nodes if necessary.
     *
     * @param secondPassword An optional second password.
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    @WebRequest
    public Completable generateNodes(@Nullable String secondPassword) {
        return Completable.fromCallable(() -> {
            payloadManager.generateNodes(secondPassword);
            return Void.TYPE;
        });
    }

    /**
     * Registers the user's MDID with the metadata service.
     *
     * @return An {@link Observable} wrapping a {@link ResponseBody}
     */
    @WebRequest
    public Observable<ResponseBody> registerMdid() {
        return payloadManager.registerMdid(payloadManager.getMetadataNodeFactory().getSharedMetadataNode());
    }

    /**
     * Unregisters the user's MDID from the metadata service.
     *
     * @return An {@link Observable} wrapping a {@link ResponseBody}
     */
    @WebRequest
    public Observable<ResponseBody> unregisterMdid() {
        return payloadManager.unregisterMdid(payloadManager.getMetadataNodeFactory().getSharedMetadataNode());
    }

}
