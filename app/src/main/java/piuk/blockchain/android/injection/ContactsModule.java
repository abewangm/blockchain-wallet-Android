//package piuk.blockchain.android.injection;
//
//import info.blockchain.wallet.contacts.Contacts;
//
//import org.bitcoinj.crypto.DeterministicKey;
//
//import javax.inject.Singleton;
//
//import dagger.Module;
//import dagger.Provides;
//import piuk.blockchain.android.data.services.ContactsService;
//
//@Module
//public class ContactsModule {
//
//    @Provides
//    @Singleton
//    protected ContactsService provideSharedMetaDataService(DeterministicKey metaDataHDNode, DeterministicKey sharedMetaDataHDNode) {
//
//        ContactsService cs = null;
//        // TODO: 15/12/2016
//        try{
//            cs = new ContactsService(new Contacts(metaDataHDNode, sharedMetaDataHDNode));
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//        return cs;
//    }
//}