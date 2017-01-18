package piuk.blockchain.android.data.contacts;

import info.blockchain.wallet.contacts.data.Contact;

import io.reactivex.functions.Predicate;

public final class ContactsPredicates {

    public static Predicate<Contact> filterById(String id) {
        return contact -> contact.getId() != null && contact.getId().equals(id);
    }


}
