package piuk.blockchain.android.data.notifications;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import javax.inject.Inject;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.contacts.list.ContactsListActivity;
import piuk.blockchain.android.ui.launcher.LauncherActivity;
import piuk.blockchain.android.util.ApplicationLifeCycle;
import piuk.blockchain.android.util.NotificationsUtil;

public class FcmCallbackService extends FirebaseMessagingService {

    private static final String TAG = FcmCallbackService.class.getSimpleName();

    public static final String EXTRA_CONTACTS_SERVICE = "contacts_service";
    public static final Subject<NotificationPayload> notificationSubject = PublishSubject.create();
    public static final int ID_BACKGROUND_NOTIFICATION = 1337;
    public static final int ID_FOREGROUND_NOTIFICATION = 1338;

    @Inject protected NotificationManager notificationManager;

    public FcmCallbackService() {
        Injector.getInstance().getAppComponent().inject(this);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            // Parse data, emit events
            NotificationPayload payload = new NotificationPayload(remoteMessage.getData());
            notificationSubject.onNext(payload);
            sendNotification(payload);
        }
    }

    public static Subject<NotificationPayload> getNotificationSubject() {
        return notificationSubject;
    }

    private void sendNotification(NotificationPayload payload) {
        if (ApplicationLifeCycle.getInstance().isForeground()
                && AccessState.getInstance().isLoggedIn()) {
            sendForegroundNotification(payload);
        } else {
            sendBackgroundNotification(payload);
        }
    }

    /**
     * Redirects the user to the {@link LauncherActivity} which will then handle the routing
     * appropriately.
     */
    private void sendBackgroundNotification(NotificationPayload payload) {
        Intent notifyIntent = new Intent(getApplicationContext(), LauncherActivity.class);
        notifyIntent.putExtra(EXTRA_CONTACTS_SERVICE, true);
        PendingIntent intent =
                PendingIntent.getActivity(getApplicationContext(), 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_notification_white)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.primary_navy_medium))
                .setContentTitle(payload.getTitle())
                .setTicker(payload.getTitle())
                .setContentText(payload.getBody())
                .setContentIntent(intent)
                .setWhen(System.currentTimeMillis())
                .setSound(Uri.parse("android.resource://"
                        + getApplicationContext().getPackageName()
                        + "/"
                        + R.raw.beep))
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[0])
                .setOnlyAlertOnce(true)
                .setDefaults(Notification.DEFAULT_LIGHTS);

        notificationManager.notify(ID_BACKGROUND_NOTIFICATION, builder.build());
    }

    /**
     * Redirects the user to the {@link ContactsListActivity}
     */
    private void sendForegroundNotification(NotificationPayload payload) {
        new NotificationsUtil(getApplicationContext(), notificationManager).setNotification(
                payload.getTitle(),
                payload.getTitle(),
                payload.getBody(),
                R.drawable.ic_notification_white,
                R.drawable.ic_notification_white,
                ContactsListActivity.class,
                ID_FOREGROUND_NOTIFICATION);
    }

}
