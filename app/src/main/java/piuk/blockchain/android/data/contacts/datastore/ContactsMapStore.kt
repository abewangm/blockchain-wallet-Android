package piuk.blockchain.android.data.contacts.datastore

import piuk.blockchain.android.data.contacts.models.ContactTransactionDisplayModel

class ContactsMapStore {

    /**
     * A [MutableMap] containing a [ContactTransactionDisplayModel] keyed to a Tx hash for convenient
     * display.
     */
    val displayMap = mutableMapOf<String, ContactTransactionDisplayModel>()

    fun clearDisplayMap() {
        displayMap.clear()
    }

}
