package piuk.blockchain.android.ui.account

class ItemAccount {

    var label: String? = null
    var displayBalance: String? = null
    var tag: String? = null
    var absoluteBalance: Long? = null

    var accountObject: Any? = null

    constructor() {
        // Empty constructor for serialization
    }

    constructor(label: String?,
                displayBalance: String?,
                tag: String?,
                absoluteBalance: Long?,
                accountObject: Any?) {
        this.label = label
        this.displayBalance = displayBalance
        this.tag = tag
        this.absoluteBalance = absoluteBalance
        this.accountObject = accountObject
    }
}
