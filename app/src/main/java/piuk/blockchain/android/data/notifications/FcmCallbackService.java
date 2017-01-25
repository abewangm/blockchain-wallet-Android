package piuk.blockchain.android.data.notifications;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Map;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.contacts.list.ContactsListActivity;
import piuk.blockchain.android.util.NotificationsUtil;

public class FcmCallbackService extends FirebaseMessagingService {

    private static final String TAG = FcmCallbackService.class.getSimpleName();

    public static final Subject<NotificationPayload> notificationSubject = PublishSubject.create();

    public FcmCallbackService() {
        Log.d(TAG, "FcmCallbackService: constructor instantiated");
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            // Parse data, emit events
            NotificationPayload payload = new NotificationPayload(remoteMessage.getData());
            notificationSubject.onNext(payload);

            new NotificationsUtil(getApplicationContext()).setNotification(
                    payload.getTitle(),
                    payload.getBody(),
                    payload.getBody(),
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

        private String title;
        private String body;

        public NotificationPayload(Map<String, String> map) {
            if (map.containsKey("title")) {
                title = map.get("title");
            }

            if (map.containsValue("body")) {
                body = map.get("body");
            }
        }

        @Nullable
        public String getTitle() {
            return title;
        }

        @Nullable
        public String getBody() {
            return body;
        }
    }

}
