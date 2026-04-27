package dev.mariinkys.openPillReminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.time.LocalDate

const val CHANNEL_ID = "pill_reminder_channel"

fun createNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        "Pill Reminders",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Daily pill reminder notifications"
    }
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
}

fun sendPillNotification(context: Context, userName: String, isBreakDay: Boolean, date: LocalDate) {
    val title = if (isBreakDay) "Placebo Reminder" else "Pill Reminder"

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP // Don't restart the app if it's open
        putExtra("OPEN_LOG_DATE", date.toString()) // Pass the date as a string
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val namePart = userName.trim().takeIf { it.isNotEmpty() }
        ?.let { ", $it" }
        ?: ""

    val message = if (isBreakDay)
        "Time to take your placebo pill$namePart!"
    else
        "Time to take your pill$namePart!"

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(1, notification)
}
