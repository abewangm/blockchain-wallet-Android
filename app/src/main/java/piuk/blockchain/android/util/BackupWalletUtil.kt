package piuk.blockchain.android.util

import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.util.annotations.Mockable
import timber.log.Timber
import java.security.SecureRandom
import java.util.*

@Mockable
class BackupWalletUtil(private val payloadDataManager: PayloadDataManager) {

    /**
     * Returns an ordered list of [Int], [String] pairs which can be used to confirm mnemonic.
     */
    fun getConfirmSequence(secondPassword: String?): List<Pair<Int, String>> {
        val mnemonic = getMnemonic(secondPassword)
        val randomGenerator = SecureRandom()
        val seen = mutableListOf<Int>()

        var i = 0
        while (i < 3) {
            val number = randomGenerator.nextInt(mnemonic!!.size)
            if (!seen.contains(number)) {
                seen.add(number)
                i++
            }
        }

        Collections.sort(seen)

        return (0..2).map { seen[it] to mnemonic!![seen[it]] }
    }

    /**
     * Returns a [MutableList] of Strings representing the user's backup mnemonic, or null
     * if the mnemonic isn't found.
     */
    fun getMnemonic(secondPassword: String?): List<String>? = try {
        payloadDataManager.wallet.decryptHDWallet(0, secondPassword)
        payloadDataManager.wallet.hdWallets[0].mnemonic.toList()
    } catch (e: Exception) {
        Timber.e("getMnemonic returned null", e)
        null
    }

}
