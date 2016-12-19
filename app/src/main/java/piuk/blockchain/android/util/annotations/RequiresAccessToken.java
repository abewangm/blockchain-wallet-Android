package piuk.blockchain.android.util.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import piuk.blockchain.android.data.datamanagers.ContactsDataManager;

/**
 * Specifies that the method being called hits a Shared Metadata endpoing and requires an
 * authenticated access token. As such, this method should be called via the helper methods {@link
 * ContactsDataManager#callWithToken(ContactsDataManager.CompletableTokenRequest)} or {@link
 * ContactsDataManager#callWithToken(ContactsDataManager.ObservableTokenRequest)} depending on the
 * subscription type.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD})
public @interface RequiresAccessToken {
}