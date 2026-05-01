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
        context.getString(R.string.channel_name),
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = context.getString(R.string.channel_description)
    }
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
}

fun sendPillNotification(context: Context, userName: String, isBreakDay: Boolean, date: LocalDate) {
    val title = context.getString(
        if (isBreakDay) R.string.notif_placebo_title else R.string.notif_pill_title
    )

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP // Don't restart the app if it's open
        putExtra("OPEN_LOG_DATE", date.toString())
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val namePart = getLocalizedNamePart(context, userName)

    val message = context.getString(
        if (isBreakDay) R.string.notif_placebo_msg else R.string.notif_pill_msg,
        namePart
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_name)
        .setContentTitle(title)
        .setContentText(message)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(1, notification)
}

fun sendBuyingNotification(context: Context, userName: String) {
    val namePart = getLocalizedNamePart(context, userName)

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_name)
        .setContentTitle(context.getString(R.string.notif_buy_title))
        .setContentText(context.getString(R.string.notif_buy_msg, namePart))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(2, notification)
}

fun getLocalizedNamePart(context: Context, userName: String): String {
    val trimmedName = userName.trim()
    if (trimmedName.isEmpty()) return ""

    // get the current language code ("en", "es", "ja"...)
    val currentLanguage = context.resources.configuration.locales[0].language

    return if (currentLanguage == "ja") {
        "${trimmedName}さん"
    } else {
        ", $trimmedName"
    }
}