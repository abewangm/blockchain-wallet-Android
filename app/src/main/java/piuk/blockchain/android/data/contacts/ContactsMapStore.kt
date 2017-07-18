package piuk.blockchain.android.data.contacts

import java.util.*

class ContactsMapStore {

    val contactsTransactionMap = HashMap<String, String>()
    val notesTransactionMap = HashMap<String, String>()

    fun clearContactsTransactionMap() {
        contactsTransactionMap.clear()
    }

    fun clearNotesTransactionMap() {
        notesTransactionMap.clear()
    }
}
