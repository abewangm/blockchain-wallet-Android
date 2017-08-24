package piuk.blockchain.android.data.transactions

import info.blockchain.wallet.ethereum.data.EthAccount
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.multiaddress.TransactionSummary
import piuk.blockchain.android.ui.balance.CryptoCurrency

abstract class Displayable {

    abstract val cryptoCurrency: CryptoCurrency
    abstract val direction: TransactionSummary.Direction
    abstract val timeStamp: Long
    abstract val total: Long
    abstract val fee: Long
    abstract val hash: String
    open val confirmations = 3
    open val watchOnly: Boolean = false
    open val doubleSpend: Boolean = false
    open val isPending: Boolean = false

}

data class EthDisplayable(
        private val ethAccount: EthAccount,
        private val ethTransaction: EthTransaction
) : Displayable() {

    override val cryptoCurrency: CryptoCurrency
        get() = CryptoCurrency.ETH
    override val direction: TransactionSummary.Direction
        get() = when (ethTransaction.from) {
            ethAccount.account -> TransactionSummary.Direction.SENT
            else -> TransactionSummary.Direction.RECEIVED
        }
    override val timeStamp: Long
        get() = ethTransaction.timeStamp
    override val total: Long
        get() = ethTransaction.value.toLong()
    override val fee: Long
        get() = ethTransaction.gas.toLong()
    override val hash: String
        get() = ethTransaction.hash

}

data class BtcDisplayable(
        private val transactionSummary: TransactionSummary
) : Displayable() {

    override val cryptoCurrency: CryptoCurrency
        get() = CryptoCurrency.BTC
    override val direction: TransactionSummary.Direction
        get() = transactionSummary.direction
    override val timeStamp: Long
        get() = transactionSummary.time
    override val total: Long
        get() = transactionSummary.total.toLong()
    override val fee: Long
        get() = transactionSummary.fee.toLong()
    override val hash: String
        get() = transactionSummary.hash
    override val confirmations: Int
        get() = transactionSummary.confirmations
    override val watchOnly: Boolean
        get() = transactionSummary.isWatchOnly
    override val doubleSpend: Boolean
        get() = transactionSummary.isDoubleSpend
    override val isPending: Boolean
        get() = transactionSummary.isPending

}