package piuk.blockchain.android.data.payload;

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
import io.reactivex.exceptions.Exceptions;
import okhttp3.ResponseBody;
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
    Completable initializeFromPayload(String payload, String password) {
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
    @WebRequest
    Observable<Wallet> restoreHdWallet(String mnemonic, String walletName, String email, String password) {
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
    Observable<Wallet> createHdWallet(String password, String walletName, String email) {
        return Observable.fromCallable(() -> payloadManager.create(walletName, email, password));
    }

    /**
     * Fetches the user's wallet payload, and then initializes and decrypts a payload using the
     * user's password.
     *
     * @param sharedKey The shared key as a String
     * @param guid      The user's GUID
     * @param password  The user's password
     * @return A {@link Completable} object
     */
    @WebRequest
    Completable initializeAndDecrypt(String sharedKey, String guid, String password) {
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
    Completable handleQrCode(String data) {
        return Completable.fromCallable(() -> {
            payloadManager.initializeAndDecryptFromQR(data);
            return Void.TYPE;
        });
    }

    /**
     * Upgrades a Wallet from V2 to V3 and saves it with the server. If saving is unsuccessful or
     * some other part fails, this will propagate an Exception.
     *
     * @param secondPassword     An optional second password if the user has one
     * @param defaultAccountName A required name for the default account
     * @return A {@link Completable} object
     */
    @WebRequest
    Completable upgradeV2toV3(@Nullable String secondPassword, String defaultAccountName) {
        return Completable.fromCallable(() -> {
            if (!payloadManager.upgradeV2PayloadToV3(secondPassword, defaultAccountName)) {
                throw Exceptions.propagate(new Throwable("Upgrade wallet failed"));
            }
            return Void.TYPE;
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // SYNC METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns a {@link Completable} which saves the current payload to the server.
     *
     * @return A {@link Completable} object
     */
    @WebRequest
    Completable syncPayloadWithServer() {
        return Completable.fromCallable(() -> {
            if (!payloadManager.save()) throw new ApiException("Sync failed");
            return Void.TYPE;
        });
    }

    /**
     * Returns a {@link Completable} which saves the current payload to the server whilst also
     * forcing the sync of the user's keys. This method generates 20 addresses per {@link
     * Account}, so it should be used only when strictly necessary (for instance, after enabling
     * notifications).
     *
     * @return A {@link Completable} object
     */
    @WebRequest
    Completable syncPayloadAndPublicKeys() {
        return Completable.fromCallable(() -> {
            if (!payloadManager.saveAndSyncPubKeys()) throw new ApiException("Sync failed");
            return Void.TYPE;
        });
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
    @WebRequest
    Completable updateAllTransactions() {
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
    Completable updateAllBalances() {
        return Completable.fromCallable(() -> {
            payloadManager.updateAllBalances();
            return Void.TYPE;
        });
    }

    /**
     * Update notes for a specific transaction hash and then sync the payload to the server
     *
     * @param transactionHash The hash of the transaction to be updated
     * @param notes           Transaction notes
     * @return A {@link Completable} object
     */
    @WebRequest
    Completable updateTransactionNotes(String transactionHash, String notes) {
        payloadManager.getPayload().getTxNotes().put(transactionHash, notes);
        return syncPayloadWithServer();
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
    @WebRequest
    Observable<LinkedHashMap<String, Balance>> getBalanceOfAddresses(List<String> addresses) {
        return Observable.fromCallable(() -> payloadManager.getBalanceOfAddresses(addresses));
    }

    /**
     * Derives new {@link Account} from the master seed
     *
     * @param accountLabel   A label for the account
     * @param secondPassword An optional double encryption password
     * @return An {@link Observable<Account>} wrapping the newly created Account
     */
    @WebRequest
    Observable<Account> createNewAccount(String accountLabel, @Nullable String secondPassword) {
        return Observable.fromCallable(() -> payloadManager.addAccount(accountLabel, secondPassword));
    }

    /**
     * Sets a private key for an associated {@link LegacyAddress} which is already in the {@link
     * Wallet} as a watch only address
     *
     * @param key            An {@link ECKey}
     * @param secondPassword An optional double encryption password
     * @return An {@link Observable<Boolean>} representing a successful save
     */
    @WebRequest
    Observable<LegacyAddress> setKeyForLegacyAddress(ECKey key, @Nullable String secondPassword) {
        return Observable.fromCallable(() -> payloadManager.setKeyForLegacyAddress(key, secondPassword));
    }

    /**
     * Allows you to add a {@link LegacyAddress} to the {@link Wallet}
     *
     * @param legacyAddress The new address
     * @return A {@link Completable} object representing a successful save
     */
    @WebRequest
    Completable addLegacyAddress(LegacyAddress legacyAddress) {
        return Completable.fromCallable(() -> {
            payloadManager.addLegacyAddress(legacyAddress);
            return Void.TYPE;
        });
    }

    /**
     * Allows you to propagate changes to a {@link LegacyAddress} through the {@link Wallet}
     *
     * @param legacyAddress The updated address
     * @return A {@link Completable} object representing a successful save
     */
    @WebRequest
    Completable updateLegacyAddress(LegacyAddress legacyAddress) {
        return Completable.fromCallable(() -> {
            payloadManager.updateLegacyAddress(legacyAddress);
            return Void.TYPE;
        });
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
    @WebRequest
    Observable<Boolean> loadNodes() {
        return Observable.fromCallable(() -> payloadManager.loadNodes());
    }

    /**
     * Generates the metadata and shared metadata nodes if necessary.
     *
     * @param secondPassword An optional second password.
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    @WebRequest
    Completable generateNodes(@Nullable String secondPassword) {
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
    Observable<ResponseBody> registerMdid() {
        return payloadManager.registerMdid(payloadManager.getMetadataNodeFactory().getSharedMetadataNode());
    }

    /**
     * Unregisters the user's MDID from the metadata service.
     *
     * @return An {@link Observable} wrapping a {@link ResponseBody}
     */
    @WebRequest
    Observable<ResponseBody> unregisterMdid() {
        return payloadManager.unregisterMdid(payloadManager.getMetadataNodeFactory().getSharedMetadataNode());
    }

}
