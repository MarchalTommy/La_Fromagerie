package com.mtdevelopment.lafromagerie

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

const val ACTION_STOP_UPDATE = "stopUpdate"
const val EXTRA_NOTIFICATION_ID = "notification_id"

class NotificationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        when (intent?.action) {
            ACTION_STOP_UPDATE -> {
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

                // Arrêter le service de premier plan
                val serviceIntent = Intent(context, DeliveryTrackingService::class.java)
                context?.stopService(serviceIntent)

                val notificationManager =
                    context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)

                // Demander au service de s'arrêter lui même
                val stopServiceIntent = Intent(context, DeliveryTrackingService::class.java).apply {
                    action = DeliveryTrackingService.ACTION_STOP_SERVICE_FROM_NOTIFICATION
                }
                context.startService(stopServiceIntent)
            }

            else -> {
                // Keep ready for future actions
            }
        }
    }
}