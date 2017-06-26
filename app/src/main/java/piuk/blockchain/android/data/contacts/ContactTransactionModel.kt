package piuk.blockchain.android.data.contacts

import info.blockchain.wallet.contacts.data.FacilitatedTransaction

/**
 * Simple wrapper object for convenient list display
 */
data class ContactTransactionModel(
        val contactName: String,
        val facilitatedTransaction: FacilitatedTransaction
)
