package piuk.blockchain.android.data.notifications;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.notifications.models.NotificationPayload;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.home.MainActivity;
import piuk.blockchain.android.ui.launcher.LauncherActivity;
import piuk.blockchain.android.util.ApplicationLifeCycle;
import piuk.blockchain.android.util.NotificationsUtil;
import piuk.blockchain.android.util.PrefsUtil;
import timber.log.Timber;

public class FcmCallbackService extends FirebaseMessagingService {

    public static final String EXTRA_CONTACT_ACCEPTED = "contact_accepted";
    public static final int ID_BACKGROUND_NOTIFICATION = 1337;
    public static final int ID_FOREGROUND_NOTIFICATION = 1338;

    @Inject protected NotificationManager notificationManager;
    @Inject protected PrefsUtil prefsUtil;
    @Inject protected RxBus rxBus;

    public FcmCallbackService() {
        Injector.getInstance().getAppComponent().inject(this);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if message contains a data payload.
        if (!remoteMessage.getData().isEmpty()) {
            Timber.d("Message data payload: %s", remoteMessage.getData());

            // Parse data, emit events
            NotificationPayload payload = new NotificationPayload(remoteMessage.getData());
            rxBus.emitEvent(NotificationPayload.class, payload);
            sendNotification(payload);
        }
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
     * appropriately - ie if accepted Contact, show {@link piuk.blockchain.android.ui.contacts.list.ContactsListActivity},
     * otherwise show {@link MainActivity}.
     */
    private void sendBackgroundNotification(NotificationPayload payload) {
        Intent notifyIntent = new Intent(getApplicationContext(), LauncherActivity.class);
        if (payload.getType() != null && payload.getType().equals(NotificationPayload.NotificationType.CONTACT_REQUEST)) {
            notifyIntent.putExtra(EXTRA_CONTACT_ACCEPTED, true);
        }

        PendingIntent intent =
                PendingIntent.getActivity(getApplicationContext(), 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        triggerHeadsUpNotification(payload, intent, ID_BACKGROUND_NOTIFICATION);
    }

    /**
     * Redirects the user to the {@link MainActivity} which will then launch the balance fragment by
     * default. If notification is from an accepted Contact, {@link MainActivity} will then launch
     * {@link piuk.blockchain.android.ui.contacts.list.ContactsListActivity} once startup checks
     * have finished.
     */
    private void sendForegroundNotification(NotificationPayload payload) {
        Intent notifyIntent = new Intent(getApplicationContext(), MainActivity.class);
        if (payload.getType() != null && payload.getType().equals(NotificationPayload.NotificationType.CONTACT_REQUEST)) {
            prefsUtil.setValue(PrefsUtil.KEY_CONTACTS_NOTIFICATION, true);
        }
        PendingIntent intent =
                PendingIntent.getActivity(getApplicationContext(), 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        triggerHeadsUpNotification(payload, intent, ID_FOREGROUND_NOTIFICATION);
    }

    /**
     * Triggers a notification with the "Heads Up" feature on >21, with the "beep" sound and a short
     * vibration enabled.
     *
     * @param payload        A {@link NotificationPayload} object from the Notification Service
     * @param pendingIntent  The {@link PendingIntent} that you wish to be called when the
     *                       notification is selected
     * @param notificationId The ID of the notification
     */
    private void triggerHeadsUpNotification(NotificationPayload payload,
                                            PendingIntent pendingIntent,
                                            int notificationId) {

        new NotificationsUtil(getApplicationContext(), notificationManager).triggerNotification(
                payload.getTitle() != null ? payload.getTitle() : "",
                payload.getTitle() != null ? payload.getTitle() : "",
                payload.getBody() != null ? payload.getBody() : "",
                R.drawable.ic_notification_white,
                pendingIntent,
                notificationId);
    }

}
