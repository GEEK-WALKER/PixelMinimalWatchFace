package com.benoitletondor.pixelminimalwatchface

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.wearable.watchface.CanvasWatchFaceService
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.benoitletondor.pixelminimalwatchface.settings.ComplicationConfigActivity

class AppUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if( intent?.action != "android.intent.action.MY_PACKAGE_REPLACED" ) {
            return
        }

        val storage = Injection.storage(context)
        val latestKnownVersion = storage.getAppVersion()
        if( BuildConfig.VERSION_CODE > latestKnownVersion ) {
            if( latestKnownVersion > 0 ) {
                onAppUpgrade(context, latestKnownVersion, BuildConfig.VERSION_CODE)
            }

            storage.setAppVersion(BuildConfig.VERSION_CODE)
        }
    }

    @Suppress("SameParameterValue", "UNUSED_PARAMETER")
    private fun onAppUpgrade(context: Context, oldVersion: Int, newVersion: Int) {
        if( oldVersion <= 33 ) {
            val storage = Injection.storage(context)

            if( storage.isUserPremium() && !storage.hasShownBatteryIndicatorNotification() ) {
                storage.setBatteryIndicatorNotificationShown()
                showBatteryIndicatorNotification(context)
            }
        }
    }

    private fun showBatteryIndicatorNotification(context: Context) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            if ( wallpaperManager.wallpaperInfo?.packageName != context.packageName ) {
                return
            }

            // Create notification channel if needed
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val mChannel = NotificationChannel(MISC_NOTIFICATION_CHANNEL_ID, context.getString(R.string.misc_notification_channel_name), importance)
                mChannel.description = context.getString(R.string.misc_notification_channel_description)

                val notificationManager = context.getSystemService(CanvasWatchFaceService.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(mChannel)
            }

            val activityIntent = Intent(context, ComplicationConfigActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_CANCEL_CURRENT)

            val notification = NotificationCompat.Builder(context, MISC_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.battery_indicator_notification_title))
                .setContentText(context.getString(R.string.battery_indicator_notification_message))
                .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.battery_indicator_notification_message)))
                .addAction(NotificationCompat.Action(R.drawable.ic_settings, context.getString(R.string.battery_indicator_notification_cta), pendingIntent))
                .setAutoCancel(true)
                .build()


            NotificationManagerCompat.from(context).notify(193728, notification)
        } catch (t: Throwable) {
            Log.e("PixelWatchFace", "Error performing update actions", t)
        }
    }

}