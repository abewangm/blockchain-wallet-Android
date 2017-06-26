package piuk.blockchain.android.ui.swipetoreceive

import info.blockchain.api.data.Balance
import io.reactivex.Observable
import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.annotations.Mockable
import java.math.BigInteger
import java.util.*

@Mockable
class SwipeToReceiveHelper(
        private val payloadDataManager: PayloadDataManager,
        private val prefsUtil: PrefsUtil
) {

    /**
     * Derives 5 addresses from the current point on the receive chain. Stores them alongside the
     * account name in SharedPrefs. Only stores addresses if enabled in SharedPrefs. This should be
     * called on a Computation thread as it can take up to 2 seconds on a mid-range device.
     */
    fun updateAndStoreAddresses() {
        if (getIfSwipeEnabled()) {
            val numOfAddresses = 5

            val defaultAccount = payloadDataManager.defaultAccount
            val receiveAccountName = defaultAccount.label
            storeAccountName(receiveAccountName)

            val stringBuilder = StringBuilder()

            for (i in 0 until numOfAddresses) {
                val receiveAddress = payloadDataManager.getReceiveAddressAtPosition(defaultAccount, i) ?:
                        // Likely not initialized yet
                        break

                stringBuilder.append(receiveAddress)
                        .append(",")
            }

            storeAddresses(stringBuilder.toString())
        }
    }

    /**
     * Returns the next unused address from the list of 5 stored for swipe to receive. Can return an
     * empty String if no unused addresses are found.
     */
    fun getNextAvailableAddress(): Observable<String> {
        return getBalanceOfAddresses(getReceiveAddresses())
                .map { map ->
                    for ((address, value) in map) {
                        val balance = value.finalBalance
                        if (balance.compareTo(BigInteger.ZERO) == 0) {
                            return@map address
                        }
                    }
                    ""
                }
    }

    /**
     * Returns a List of the next 5 available unused (at the time of storage) receive addresses. Can
     * return an empty list.
     */
    fun getReceiveAddresses(): List<String> {
        val addressString = prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, "")
        return when {
            addressString.isEmpty() -> emptyList()
            else -> addressString.split(",").dropLastWhile { it.isEmpty() }
        }
    }

    /**
     * Returns the account name associated with the receive addresses.
     */
    fun getAccountName(): String = prefsUtil.getValue(KEY_SWIPE_RECEIVE_ACCOUNT_NAME, "")

    private fun getIfSwipeEnabled(): Boolean = prefsUtil.getValue(PrefsUtil.KEY_SWIPE_TO_RECEIVE_ENABLED, true)

    private fun getBalanceOfAddresses(addresses: List<String>): Observable<HashMap<String, Balance>> {
        return payloadDataManager.getBalanceOfAddresses(addresses)
                .compose(RxUtil.applySchedulersToObservable())
    }

    private fun storeAddresses(addresses: String) {
        prefsUtil.setValue(KEY_SWIPE_RECEIVE_ADDRESSES, addresses)
    }

    private fun storeAccountName(accountName: String) {
        prefsUtil.setValue(KEY_SWIPE_RECEIVE_ACCOUNT_NAME, accountName)
    }

    companion object {
        const val KEY_SWIPE_RECEIVE_ADDRESSES = "swipe_receive_addresses"
        const val KEY_SWIPE_RECEIVE_ACCOUNT_NAME = "swipe_receive_account_name"
    }

}
