package com.example.focusguard.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.focusguard.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_WARNING = "channel_warning"
        const val CHANNEL_ALERTS = "channel_alerts"
        const val CHANNEL_STATUS = "channel_status"

        const val NOTIF_ID_WARNING = 1001
        const val NOTIF_ID_STATUS = 1002
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val warningChannel = NotificationChannel(
                CHANNEL_WARNING,
                "Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Warnings for excessive usage or doomscrolling"
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General alerts regarding account status"
            }

            val statusChannel = NotificationChannel(
                CHANNEL_STATUS,
                "Monitoring Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status of the FocusGuard service"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(warningChannel)
            manager.createNotificationChannel(alertChannel)
            manager.createNotificationChannel(statusChannel)
        }
    }

    fun showWarning(title: String, message: String) {
        // Need permission check for API 33+, but assume it's granted or handle gracefully
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_WARNING)
                .setSmallIcon(android.R.drawable.ic_dialog_alert) // Replace with app icon later
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            val manager = NotificationManagerCompat.from(context)
            manager.notify(NOTIF_ID_WARNING, builder.build())
        } catch (e: SecurityException) {
            // Log error
        }
    }

    fun getStatusNotification(content: String): android.app.Notification {
        return NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("FocusGuard Active")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
