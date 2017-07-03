package piuk.blockchain.android.ui.account

class AccountItem {

    var label: String? = null
    var address: String? = null
    var amount: String = ""
    var isArchived: Boolean = false
    var isWatchOnly: Boolean = false
    var isDefault: Boolean = false
    var correctedPosition: Int = -1
    var type: Int = 0

    internal constructor(correctedPosition: Int,
                         label: String?,
                         address: String?,
                         amount: String,
                         isArchived: Boolean,
                         isWatchOnly: Boolean,
                         isDefault: Boolean,
                         type: Int) {
        this.correctedPosition = correctedPosition
        this.label = label
        this.address = address
        this.amount = amount
        this.isArchived = isArchived
        this.isWatchOnly = isWatchOnly
        this.isDefault = isDefault
        this.type = type
    }

    internal constructor(type: Int) {
        this.type = type
    }

    companion object {
        const val TYPE_CREATE_NEW_WALLET_BUTTON = 0
        const val TYPE_IMPORT_ADDRESS_BUTTON = 1
        const val TYPE_ACCOUNT = 2
    }
}