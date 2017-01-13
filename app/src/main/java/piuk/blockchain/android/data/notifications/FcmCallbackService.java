package piuk.blockchain.android.data.notifications;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import android.util.Log;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.contacts.ContactsListActivity;
import piuk.blockchain.android.util.NotificationsUtil;

public class FcmCallbackService extends FirebaseMessagingService {

    private static final String TAG = FcmCallbackService.class.getSimpleName();

    public static final Subject<NotificationPayload> notificationSubject = PublishSubject.create();

    public FcmCallbackService() {
        Log.d(TAG, "FcmCallbackService: constructor instantiated");
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "onMessageReceived: " + remoteMessage);

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            // Parse data, emit events
            notificationSubject.onNext(new NotificationPayload());

        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            RemoteMessage.Notification notification = remoteMessage.getNotification();

            Log.d(TAG, "Message Notification Body: " + notification.getBody());


            // Emit notification
            new NotificationsUtil(getApplicationContext()).setNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody(),
                    remoteMessage.getNotification().getTag(),
                    R.drawable.ic_notification_transparent,
                    R.drawable.ic_launcher,
                    ContactsListActivity.class,
                    1337);
        }
    }

    public static Subject<NotificationPayload> getNotificationSubject() {
        return notificationSubject;
    }

    @SuppressWarnings("WeakerAccess")
    public static class NotificationPayload {

        // lol i dunno because nothing is working right now
        String payload;

    }

}
