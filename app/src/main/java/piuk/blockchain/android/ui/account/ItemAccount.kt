package piuk.blockchain.android.ui.account

import org.bitcoinj.core.ECKey

class ItemAccount {

    enum class TYPE{
        ALL_ACCOUNTS_AND_LEGACY, ALL_LEGACY, SINGLE_ACCOUNT
    }

    var label: String? = null
    var displayBalance: String? = null
    var tag: String? = null
    var absoluteBalance: Long? = null

    //TODO Get rid of this Any
    var accountObject: Any? = null

    //Address/Xpub to fetch balance/tx list
    var address: String? = null
    var type: TYPE = TYPE.SINGLE_ACCOUNT

    constructor() {
        // Empty constructor for serialization
    }

    @Deprecated(message = "Still used in PendingTransaction.java")
    constructor(label: String,
                displayBalance: String,
                tag: String?,
                absoluteBalance: Long?,
                accountObject: Any?) {
        this.label = label
        this.displayBalance = displayBalance
        this.tag = tag
        this.absoluteBalance = absoluteBalance
        this.accountObject = accountObject
    }

    constructor(label: String,
                displayBalance: String,
                tag: String?,
                absoluteBalance: Long?,
                address: String?,
                type: TYPE) {
        this.label = label
        this.displayBalance = displayBalance
        this.tag = tag
        this.absoluteBalance = absoluteBalance
        this.address = address
        this.type = type
    }
}
