package piuk.blockchain.android.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import piuk.blockchain.android.R

class NotificationsUtil(
        private val context: Context,
        private val notificationManager: NotificationManager
) {

    fun triggerNotification(title: String,
                            marquee: String,
                            text: String,
                            drawablePostLollipop: Int,
                            drawablePreLollipop: Int,
                            activityToLaunch: Class<*>,
                            id: Int) {

        val drawableCompat = if (AndroidUtils.is21orHigher()) drawablePostLollipop else drawablePreLollipop
        val notifyIntent = Intent(context, activityToLaunch)
        val intent = PendingIntent.getActivity(
                context,
                0,
                notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(drawableCompat)
                .setColor(ContextCompat.getColor(context, R.color.primary_navy_medium))
                .setContentTitle(title)
                .setContentIntent(intent)
                .setWhen(System.currentTimeMillis())
                .setSound(Uri.parse("android.resource://${context.packageName}/${R.raw.beep}"))
                .setTicker(marquee)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(longArrayOf(100))
                .setOnlyAlertOnce(true)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setContentText(text)

        if (AndroidUtils.is26orHigher()) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.app_name),
                    importance
            ).apply {
                enableLights(true)
                lightColor = ContextCompat.getColor(context, R.color.primary_navy_medium)
                enableVibration(true)
                vibrationPattern = longArrayOf(100)
            }
            notificationManager.createNotificationChannel(notificationChannel)
        }

        notificationManager.notify(id, builder.build())
    }

    companion object {
        private val NOTIFICATION_CHANNEL_ID = "group_01"
    }
}
