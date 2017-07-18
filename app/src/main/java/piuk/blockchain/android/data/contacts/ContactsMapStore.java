package piuk.blockchain.android.data.contacts;

import java.util.HashMap;

public class ContactsMapStore {

    private HashMap<String, String> contactsTransactionMap = new HashMap<>();
    private HashMap<String, String> notesTransactionMap = new HashMap<>();

    public HashMap<String, String> getContactsTransactionMap() {
        return contactsTransactionMap;
    }

    public HashMap<String, String> getNotesTransactionMap() {
        return notesTransactionMap;
    }

    public void clearContactsTransactionMap() {
        contactsTransactionMap.clear();
    }

    public void clearNotesTransactionMap() {
        notesTransactionMap.clear();
    }
}
